"""Shared local/CI suite orchestration; underlying domain tools remain authoritative."""
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import time
import zipfile

from runtime_client import RuntimeFailure, save_json
from qa_artifacts import export_evidence

def plan(root, profile):
    from project_config import load_project
    profiles = load_project(root).get('tests', {}).get('profiles', {})
    if profile == 'all':
        names = list(profiles)
    else:
        names = [profile]
    tasks = []
    for name in names:
        declared = profiles.get(name)
        if not declared:
            tasks.append((name + '-not-configured', 'unavailable', name))
        else:
            tasks.extend((row['id'], row['kind'], row.get('value')) for row in declared)
    if not tasks:
        tasks.append(('profiles-not-configured', 'unavailable', profile))
    if len({row[0] for row in tasks}) != len(tasks):
        raise RuntimeFailure('Selected QA profiles contain duplicate task IDs.')
    return tasks


def cli_check(root, out, arguments, tests=False):
    out.mkdir(parents=True)
    command = [sys.executable, '-B', '-m', 'unittest', 'discover', '-s', 'tools/tests', '-v'] if tests else [sys.executable, '-B', str(root / 'tools/project.py'), *arguments, '--json']
    result = subprocess.run(command, cwd=root, capture_output=True, text=True, timeout=300)
    (out / 'output.log').write_text(result.stdout + result.stderr)
    if tests:
        skipped = re.search(r'skipped=(\d+)', result.stderr)
        count = re.search(r'Ran (\d+) tests?', result.stderr)
        payload = {'passed': result.returncode == 0 and count is not None and int(count.group(1)) > 0, 'skipped': int(skipped.group(1)) if skipped else 0}
        payload['qa_unavailable'] = payload['skipped'] > 0
        checks = [{'id': 'qa.tests', 'status': 'error' if result.returncode else 'warning' if payload['skipped'] else 'ok', 'message': 'Tooling tests completed; inspect output.log for tests and any explicit skips.'}]
    else:
        payload = json.loads(result.stdout); checks = payload['checks']
        if result.returncode and not any(c['status'] == 'error' for c in checks):
            checks.append({'id': 'qa.cli', 'status': 'error', 'message': 'CLI exited unsuccessfully.'})
    save_json(out / 'result.json', payload)
    return checks, payload


def run_task(root, config, private, identifier, kind, value, jar, baseline):
    out = private / identifier
    if kind == 'unavailable':
        return [{'status': 'warning', 'message': 'QA profile is not configured: ' + value}], {'qa_unavailable': True}
    native = kind in ('suite', 'scenario', 'matrix', 'explore', 'reduce', 'audio', 'spine')
    if native and not jar:
        raise RuntimeFailure('Native QA tasks require --jar with a concrete packaged game.')
    if native and not (os.environ.get('DISPLAY') or os.environ.get('WAYLAND_DISPLAY')):
        return [{'status': 'warning', 'message': 'No display environment for actual desktop GL.'}], {'qa_unavailable': True}
    if kind == 'tests': return cli_check(root, out, [], tests=True)
    if kind == 'cli': return cli_check(root, out, [value])
    if kind == 'formats':
        from audio_tools import validate_formats
        checks = validate_formats(root); out.mkdir(); payload = {'checks': checks}; save_json(out / 'result.json', payload)
        return checks, payload
    if kind == 'spine':
        from spine_runtime import packaged_spine
        return packaged_spine(root, config, out, jar, value)
    if kind == 'audio':
        from audio_runtime import test_audio
        payload = test_audio(root, config, out, jar=jar, cue=value)
        return [{'status': 'warning' if payload.get('qa_unavailable') else 'ok' if payload['passed'] else 'error', 'message': payload.get('error') or 'Declared native audio profile completed.'}], payload
    if kind == 'suite':
        from runtime_scenarios import run_suite
        return run_suite(root, config, value, out, jar)
    if kind == 'scenario':
        from runtime_scenario import run_scenario
        return run_scenario(root, config, root / value, out, jar, engine_replay=True)
    if kind == 'matrix':
        from runtime_visual import run_matrix
        return run_matrix(root, config, root / value, out, jar)
    if kind == 'pixels':
        if not baseline:
            return [{'status': 'warning', 'message': 'No reviewed compatible visual baseline selected.'}], {'qa_unavailable': True}
        from runtime_visual import image_library, compare_matrix
        image_library()
        checks, payload = compare_matrix(root, private / value / 'result.json', baseline, out)
        if any(row['status'] == 'incompatible_environment' for row in payload['captures']): payload['qa_unavailable'] = True
        return checks, payload
    if kind == 'trace':
        from runtime_trace import compare_traces
        return compare_traces(root, private / value['reference'], private / value['candidate'], out)
    if kind == 'explore':
        from runtime_explore import run_exploration
        return run_exploration(root, config, root / value, out, jar)
    if kind == 'reduce':
        from runtime_reduce import reduce_scenario
        return reduce_scenario(root, config, root / value, out, jar, max_trials=8, budget_seconds=180)
    raise RuntimeFailure('Unknown QA task kind: ' + kind)


def run_qa(root, config, profile, output, jar=None, baseline=None, allow_unavailable=False):
    root = Path(root).resolve(); out = Path(output).expanduser().resolve(); tasks = plan(root, profile)
    if out.exists() or any(out.is_relative_to(root / p) for p in ('assets', 'raw', 'tools', 'skills', '.git')):
        raise RuntimeFailure('QA evidence requires a new directory outside project sources/assets/tools.')
    jar = Path(jar).resolve() if jar else None
    jar_hash = hashlib.sha256(jar.read_bytes()).hexdigest() if jar else None
    out.mkdir(parents=True); private = out / 'private'; private.mkdir()
    revision = subprocess.run(['git','rev-parse','HEAD'],cwd=root,capture_output=True,text=True)
    dirty = subprocess.run(['git','status','--porcelain'],cwd=root,capture_output=True,text=True)
    engine_metadata = None
    if jar:
        with zipfile.ZipFile(jar) as archive:
            if 'blade-engine-build.properties' in archive.namelist():
                engine_metadata = archive.read('blade-engine-build.properties').decode()
    result = {'schema_version': 1, 'profile': profile, 'passed': False, 'complete': False, 'allow_unavailable': allow_unavailable,
              'jar_sha256': jar_hash, 'engine_build_metadata':engine_metadata,
              'game_revision':revision.stdout.strip() if revision.returncode==0 else None,'worktree_dirty':bool(dirty.stdout),
              'tasks': [], 'platform': platform.platform(),
              'device_checks': {'android': {'status': 'not_run', 'reason': 'Desktop suite; no device smoke executor selected.'},
                                'ios': {'status': 'unavailable' if platform.system() != 'Darwin' else 'not_run', 'reason': 'Requires Apple device/simulator environment and separate smoke execution.'}},
              'human_checks': {'status': 'separate', 'reason': 'Agent pixel/semantic/audio measurements do not certify all art, translations, listening or playthroughs.'}}
    started = time.monotonic()
    def persist():
        save_json(out / 'result.json', result)
        (out / 'report.md').write_text('# QA ' + profile + '\n\n' + '\n'.join('- ' + v['id'] + ': ' + v['status'] + (' — ' + v['error'] if v.get('error') else '') for v in result['tasks'])
            + '\n\nUnavailable checks remain explicit. See artifacts/artifact-index.json for retained/excluded evidence. Device/human checks are separate.\n')
    for identifier, kind, value in tasks:
        begin = time.monotonic(); row = {'id': identifier, 'kind': kind, 'status': 'running', 'evidence': 'private/' + identifier, 'exported_evidence':'artifacts/'+identifier}
        result['tasks'].append(row); persist()
        print('QA: ' + identifier, file=sys.stderr, flush=True)
        try:
            if jar and hashlib.sha256(jar.read_bytes()).hexdigest() != jar_hash: raise RuntimeFailure('Packaged game changed during QA; refusing mixed-build results.')
            needed=tuple(value.values()) if kind == 'trace' else (value,) if kind == 'pixels' else ()
            known={v['id']:v for v in result['tasks'][:-1]}
            if any(name not in known for name in needed): raise RuntimeFailure('QA comparison requires preceding declared task evidence.')
            dependencies=[known[name] for name in needed if known[name]['status'] in ('failed','unavailable')]
            if dependencies:
                unavailable=all(v['status']=='unavailable' for v in dependencies)
                checks=[{'status':'warning' if unavailable else 'error','message':'Blocked by '+', '.join(v['id'] for v in dependencies)}]
                payload={'qa_unavailable':unavailable}
            else:
                checks, payload = run_task(root, config, private, identifier, kind, value, jar, baseline)
            if jar and hashlib.sha256(jar.read_bytes()).hexdigest() != jar_hash:
                raise RuntimeFailure('Packaged game changed during QA; refusing mixed-build results.')
            errors = [c['message'] for c in checks if c['status'] == 'error']
            status = 'failed' if errors or payload.get('passed') is False and not payload.get('qa_unavailable') else 'unavailable' if payload.get('qa_unavailable') else 'warning' if any(c['status'] == 'warning' for c in checks) else 'passed'
            row.update(status=status, checks=checks, error='; '.join(errors) or payload.get('error'), elapsed_seconds=time.monotonic() - begin)
        except Exception as exc:
            row.update(status='failed', error=str(exc), elapsed_seconds=time.monotonic() - begin)
        persist()
    result['elapsed_seconds'] = time.monotonic() - started
    result['complete'] = all(v['status'] not in ('failed', 'unavailable') for v in result['tasks'])
    result['passed'] = all(v['status'] != 'failed' and (v['status'] != 'unavailable' or allow_unavailable) for v in result['tasks'])
    persist()
    try:
        artifacts = export_evidence(private, out / 'artifacts')
        result['artifacts'] = {'complete': artifacts['complete'], 'files': artifacts['files'], 'bytes': artifacts['bytes'], 'index': 'artifacts/artifact-index.json'}
        if not artifacts['complete']: result.update(passed=False, complete=False)
    except Exception as exc: result.update(passed=False, complete=False, artifact_error=str(exc))
    persist()
    checks = [{'id': 'qa.' + row['id'], 'status': 'error' if row['status'] == 'failed' or row['status'] == 'unavailable' and not allow_unavailable else 'warning' if row['status'] in ('warning', 'unavailable') else 'ok', 'message': row.get('error') or row['status']} for row in result['tasks']]
    if not result['passed'] and not any(c['status'] == 'error' for c in checks): checks.append({'id': 'qa.artifacts', 'status': 'error', 'message': result.get('artifact_error', 'Artifact budget incomplete.')})
    return checks, result


def execute(root, config, args):
    try:
        def path(value):
            if not value: return None
            p = Path(value).expanduser(); return p if p.is_absolute() else Path(root) / p
        return run_qa(root, config, args.profile, path(args.out), path(args.jar), path(args.baseline), args.allow_unavailable)
    except (RuntimeFailure, OSError, ValueError) as exc:
        return [{'id': 'qa', 'status': 'error', 'message': str(exc)}], {}

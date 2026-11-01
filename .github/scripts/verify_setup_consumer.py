#!/usr/bin/env python3
"""Verify fresh independent consumers of a complete local setup bundle (Python 3.10+)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import zipfile


def write(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def verify(bundle, output, native=False, offline=False, media=False):
    if media and not native:
        raise ValueError('--media requires --native.')
    bundle = Path(bundle).resolve()
    output = Path(output).absolute()
    if output.exists() or output.is_symlink():
        raise ValueError('Evidence destination must be new.')
    output.mkdir(parents=True)
    checks = []
    report = {'schema_version': 1, 'passed': False, 'complete': False,
              'bundle_sha256': hashlib.sha256(bundle.read_bytes()).hexdigest(),
              'native_selected': native, 'media_selected': media, 'status': 'running', 'checks': checks,
              'limits': ['Device and artistic/listening reviews are separate.',
                         'Local consumption is not remote CI or publication evidence.']}
    def command(name, arguments, cwd=output, env=None, timeout=600):
        log = output / (name + '.log')
        with log.open('wb') as stream:
            child = subprocess.Popen(list(map(str, arguments)), cwd=cwd, env=env,
                                     stdin=subprocess.DEVNULL, stdout=stream,
                                     stderr=subprocess.STDOUT, start_new_session=os.name == 'posix')
            try:
                code = child.wait(timeout=timeout)
            except BaseException:
                if os.name == 'posix':
                    os.killpg(child.pid, signal.SIGKILL)
                else:
                    child.kill()
                child.wait(timeout=15)
                raise
        # Keep failure logs bounded; full native exports have their own budgets.
        if log.stat().st_size > 2 * 1024 * 1024:
            with log.open('rb') as stream:
                stream.seek(-2 * 1024 * 1024, 2)
                tail = stream.read()
            log.write_bytes(b'[Earlier log output truncated]\n' + tail)
        checks.append({'id': name, 'passed': code == 0, 'log': log.name})
        print(name + ': ' + ('passed' if code == 0 else 'FAILED'), flush=True)
        write(output / 'result.json', report)
        if code:
            raise RuntimeError(name + ' failed; inspect ' + str(log))
    try:
        expected = report['bundle_sha256'] + '  ' + bundle.name + '\n'
        if Path(str(bundle) + '.sha256').read_text(encoding='utf-8') != expected:
            raise ValueError('Outer bundle checksum mismatch.')
        if native and (not sys.platform.startswith('linux') or not (os.environ.get('DISPLAY') or os.environ.get('WAYLAND_DISPLAY'))):
            raise ValueError('This native consumer runner requires Linux and an actual GL display (Xvfb is supported).')
        extracted = output / 'bundle'
        extracted.mkdir()
        with zipfile.ZipFile(bundle) as archive:
            for info in archive.infolist():
                path = Path(info.filename)
                if path.is_absolute() or '..' in path.parts or '\\' in info.filename or (info.external_attr >> 16) & 0o170000 == 0o120000:
                    raise ValueError('Unsafe bundle member.')
            archive.extractall(extracted)
        metadata = json.loads((extracted / 'setup-bundle.json').read_text())
        name = metadata['artifact']
        if Path(name).name != name or name in ('.', '..'):
            raise ValueError('Bundle artifact must be a basename.')
        jar = extracted / name
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
        if digest != metadata['sha256'] or (extracted / 'SHA256SUMS').read_text() != digest + '  ' + name + '\n':
            raise ValueError('Bundle checksum mismatch.')
        report['identity'] = metadata['identity']
        cases = [('desktop', {}),
                 ('single-noink', {'ink': False, 'interaction_mode': 'single_click', 'viewport': [1280, 720]}),
                 ('pie-external', {'interaction_mode': 'pie', 'art_storage': 'external', 'platforms': ['desktop', 'android', 'ios']}),
                 ('audio', {'audio': True}), ('spine', {'spine': True})]
        report['media_required'] = any(options.get('audio') or options.get('spine') for _, options in cases)
        if report['media_required'] and not media:
            report['limits'].append('Audio and Spine runtime capture checks were not selected.')
        headless = dict(os.environ)
        for key in ('DISPLAY', 'WAYLAND_DISPLAY', 'ANDROID_HOME', 'ANDROID_SDK_ROOT'):
            headless.pop(key, None)
        java = Path(os.environ['JAVA_HOME']) / 'bin/java' if os.environ.get('JAVA_HOME') else 'java'
        for identifier, options in cases:
            project = output / (identifier + ' café game')
            spec = output / (identifier + '-spec.json')
            write(spec, {'schema_version': 1, 'id': 'consumer-' + identifier,
                         'title': "L'aventure café " + identifier, 'package': 'org.example.consumer', **options})
            command(identifier + '-create', [java, '-jar', jar, 'create', '--spec', spec, '--out', project, '--json'], env=headless)
            cli = [sys.executable, '-B', project / 'tools/project.py']
            if options.get('art_storage') == 'external':
                command(identifier + '-art-root', [*cli, 'art-root', '--path', output / 'external art café', '--seed-starter', '--json'], cwd=project)
            command(identifier + '-foundation', [*cli, 'doctor', '--profile', 'foundation', '--json'], cwd=project)
            command(identifier + '-fast', [*cli, 'qa', '--profile', 'fast', '--out', output / (identifier + '-fast'), '--json'], cwd=project)
            if options.get('ink', True):
                command(identifier + '-ink', [*cli, 'ink-compile', '--json'], cwd=project)
                command(identifier + '-narrative', [*cli, 'ink-test', '--json'], cwd=project)
            wrapper = project / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
            command(identifier + '-package', [wrapper, ':desktop:dist', *(['--offline'] if offline else []), '--console=plain'], cwd=project, env=headless, timeout=1200)
            game = project / 'desktop/build/libs/desktop-0.1.0.jar'
            if native:
                command(identifier + '-native', [*cli, 'qa', '--profile', 'desktop', '--jar', game, '--out', output / (identifier + '-native'), '--json'], cwd=project)
                if media and (options.get('audio') or options.get('spine')):
                    command(identifier + '-media', [*cli, 'qa', '--profile', 'media', '--jar', game, '--out', output / (identifier + '-media'), '--json'], cwd=project)
        report['status'] = 'completed'
        report['passed'] = True
        report['complete'] = native and (media or not report.get('media_required', False))
        if not native:
            report['limits'].append('Native execution was not selected; builds/static checks do not certify gameplay.')
    except Exception as error:
        report['status'] = 'failed'
        report['error'] = str(error)
    finally:
        write(output / 'result.json', report)
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundle', required=True)
    parser.add_argument('--out', required=True)
    parser.add_argument('--native', action='store_true')
    parser.add_argument('--media', action='store_true', help='Run the optional audio and Spine capture QA; requires --native and its system prerequisites.')
    parser.add_argument('--offline', action='store_true', help='Build from already resolved dependencies; generation is always offline.')
    args = parser.parse_args()
    try:
        result = verify(args.bundle, args.out, args.native, args.offline, args.media)
    except Exception as error:
        result = {'passed': False, 'complete': False, 'error': str(error)}
    print(json.dumps(result, indent=2))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())

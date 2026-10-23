"""Bounded delta debugging through the shared packaged-game scenario runner."""
from copy import deepcopy
import hashlib
import json
from pathlib import Path
import time

from runtime_client import RuntimeFailure, save_json
from runtime_scenario import run_scenario, validate


def minimize(items, reproduces, max_trials=12, deadline=None):
    """Remove chunks without reordering steps; cache candidates, bound expensive runs.

    The caller has already proved the original sequence fails. A budget-limited
    result is a smaller reproducer, not a claim of global or one-step minimality.
    """
    current = list(items)
    granularity = 2
    cache = {}
    trials = 0
    exhausted = False
    while len(current) >= 2:
        size = (len(current) + granularity - 1) // granularity
        reduced = False
        for start in range(0, len(current), size):
            candidate = current[:start] + current[start + size:]
            if not candidate:
                continue
            key = json.dumps(candidate, sort_keys=True, separators=(',', ':'), allow_nan=False)
            if key not in cache:
                if trials >= max_trials or (deadline is not None and time.monotonic() >= deadline):
                    exhausted = True
                    break
                cache[key] = bool(reproduces(candidate))
                trials += 1
            if cache[key]:
                current = candidate
                granularity = max(2, granularity - 1)
                reduced = True
                break
        if exhausted:
            break
        if not reduced:
            if granularity >= len(current):
                break
            granularity = min(len(current), granularity * 2)
    return current, {'trials': trials, 'budget_exhausted': exhausted,
                     'single_step_minimal': not exhausted,
                     'global_minimum_proven': False}


def failure_signature(result, evidence):
    """Only correlated engine step failures count; launch/cleanup faults do not."""
    path = Path(evidence) / 'engine-job.json'
    snapshot = Path(evidence) / 'failure-state.json'
    if result.get('passed') is not False or result.get('evidence_errors') or not path.is_file() or not snapshot.is_file():
        return None
    job = json.loads(path.read_text())
    step = (job.get('progress') or {}).get('step_id')
    error = job.get('error') or {}
    if job.get('status') != 'failed' or step in (None, 'setup') or step != result.get('failed_step'):
        return None
    if not all(isinstance(error.get(k), str) and error[k] for k in ('code', 'message')):
        return None
    state = json.loads(snapshot.read_text())
    diagnostics = state.get('diagnostics')
    if not isinstance(diagnostics, dict):
        return None
    # A generic no-error guard can fail for several different engine causes.
    # Preserve the actual cause as well as the guard receipt, without volatile
    # frame/runner identities or stack positions.
    cause = {'last_error': diagnostics.get('last_error'), 'exception': diagnostics.get('exception'),
             'ink_errors': (state.get('ink') or {}).get('errors')}
    return {'step_id': step, 'code': error['code'], 'message': error['message'], 'diagnostics': cause}


def reduce_scenario(root, config, filename, output, jar=None, max_trials=12, budget_seconds=600):
    """Retain every isolated trial and verify the final candidate in a fresh run."""
    source = Path(filename).expanduser().resolve()
    spec = validate(json.loads(source.read_text()))
    out = Path(output).expanduser().resolve()
    root = Path(root).resolve()
    if type(max_trials) is not int or not 1 <= max_trials <= 128:
        raise RuntimeFailure('Reduction requires 1–128 candidate trials.')
    if type(budget_seconds) is not int or not 1 <= budget_seconds <= 7200:
        raise RuntimeFailure('Reduction budget must be 1–7200 seconds.')
    for directory in ('assets', 'raw', 'tools', 'skills', '.git'):
        protected = root / directory
        if out == protected or protected in out.parents:
            raise RuntimeFailure('Reduction evidence must be outside project source/assets/tool directories.')
    if out.exists():
        raise RuntimeFailure('Reduction output must be a new directory.')
    # Pin one concrete executable for every run; do not rebuild between trials.
    executable = Path(jar).expanduser().resolve() if jar else root / 'desktop/build/libs/desktop-0.01.jar'
    if not executable.is_file():
        raise RuntimeFailure('Reduction requires an existing packaged game JAR (--jar).')
    digest = hashlib.sha256(executable.read_bytes()).hexdigest()
    out.mkdir(parents=True)
    save_json(out / 'original-scenario.json', spec)
    save_json(out / 'manifest.json', {
        'schema_version': 1, 'jar': str(executable), 'jar_sha256': digest,
        'scenario_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
        'max_candidate_trials': max_trials, 'candidate_budget_seconds': budget_seconds,
        'budget_scope': 'Checked between runs; baseline/final confirmation are additional. Each engine replay has its own 60-second deadline.'})
    rows = []

    def trial(steps, name, expected=None):
        if hashlib.sha256(executable.read_bytes()).hexdigest() != digest:
            raise RuntimeFailure('Packaged game changed during reduction; refusing mixed-build evidence.')
        candidate = deepcopy(spec)
        candidate['steps'] = steps
        path = out / (name + '-scenario.json')
        save_json(path, candidate)
        _, result = run_scenario(root, config, path, out / name, executable, engine_replay=True)
        signature = failure_signature(result, out / name)
        row = {'id': name, 'step_ids': [s['id'] for s in steps], 'signature': signature,
               'matches_baseline': signature == expected if expected else None,
               'passed': result['passed'], 'error': result.get('error'),
               'evidence_errors': result.get('evidence_errors', []), 'evidence': str(out / name)}
        rows.append(row)
        save_json(out / 'trials.json', rows)
        return signature

    original = None
    stats = {'trials': 0, 'budget_exhausted': False, 'single_step_minimal': False, 'global_minimum_proven': False}
    reduced = spec['steps']
    confirmed = False
    failure = None
    try:
        original = trial(spec['steps'], 'baseline')
        if original:
            deadline = time.monotonic() + budget_seconds

            def reproduces(steps):
                return trial(steps, 'trial-%03d' % len(rows), original) == original

            reduced, stats = minimize(reduced, reproduces, max_trials, deadline)
            confirmed = trial(reduced, 'confirmation', original) == original
            if confirmed:
                candidate = deepcopy(spec)
                candidate['steps'] = reduced
                save_json(out / 'reduced-scenario.json', candidate)
            else:
                failure = 'Final candidate did not reproduce the original failure in a fresh session.'
    except (RuntimeFailure, OSError, ValueError) as exc:
        failure = str(exc)
    result = {'schema_version': 1, 'status': 'error' if failure else 'reproduced' if confirmed else 'not_reproduced',
              'error': failure, 'signature': original, 'confirmed': confirmed,
              'original_steps': len(spec['steps']), 'reduced_steps': len(reduced),
              **stats, 'trial_runs': len(rows),
              'limitations': ['Setup is an explicit debug fixture, not a reachable player checkpoint.',
                              'Exact step/code/message and diagnostic-cause matching is conservative; timing-dependent failures may not reduce.',
                              'Single-step minimality applies only to deletions with this fixture, build and observed oracle.']}
    save_json(out / 'result.json', result)
    (out / 'report.md').write_text('# Scenario reduction\n\n' + result['status'] + '\n\n'
        + str(len(spec['steps'])) + ' → ' + str(len(reduced)) + ' steps; '
        + str(len(rows)) + ' isolated runs.\n\n'
        + 'Original setup and step order are preserved. See trials.json and each run for evidence.\n')
    status = 'error' if failure else 'ok' if confirmed else 'warning'
    message = failure or ('Confirmed reduced reproducer.' if confirmed else 'No correlated engine step failure to reduce.')
    return [{'id': 'runtime.reduce', 'status': status, 'message': message, 'path': str(out / 'result.json')}], result

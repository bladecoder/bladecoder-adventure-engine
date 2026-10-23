"""Compare ordered semantic observations, without treating IDs/timing as game state."""
import hashlib
import json
from pathlib import Path

from runtime_client import RuntimeFailure, save_json


def normalize(events):
    result = []
    for event in events:
        kind = event['type']; data = event.get('data')
        if not isinstance(data, dict): continue
        if kind.startswith(('action.', 'verb.', 'ink.action.')):
            ref = data.get('source_ref') or {}
            payload = {key: data[key] for key in ('verb', 'flow', 'action', 'target', 'completion') if key in data}
            payload['source_ref'] = {key: ref[key] for key in ('source', 'resource', 'json_pointer', 'action_pointer', 'ink_path', 'flow', 'mapping') if key in ref}
        elif kind.startswith('ink.external.'):
            payload = {key: data[key] for key in ('name', 'arguments', 'result', 'exception', 'path', 'flow', 'scope') if key in data}
        elif kind in ('ink.line', 'ink.command', 'ink.variable', 'dialogue.text', 'scene.changed'):
            payload = data
        elif kind == 'dialogue.choices': payload = {'choices': data.get('choices')}
        else: continue
        result.append({'type': kind, 'scene': event.get('scene'), 'data': payload})
    return result


def compare_traces(root, reference, candidate, output):
    root = Path(root).resolve(); a = Path(reference).resolve(); b = Path(candidate).resolve(); out = Path(output).resolve()
    if out.exists() or any(out.is_relative_to(root / p) for p in ('assets', 'raw', 'tools', 'skills', 'ai-toolkit', 'automation', 'docs', '.git')):
        raise RuntimeFailure('Trace comparison output must be new and outside project source/assets/tools.')
    def load(folder):
        result = json.loads((folder / 'result.json').read_text())
        if result.get('passed') is not True: raise RuntimeFailure('Compare successful runs; diagnose original failing scenarios separately.')
        setup = json.loads((folder / 'manifest.json').read_text()).get('setup')
        build = json.loads((folder / 'process.json').read_text())['build']
        if not setup or not build.get('sha256'): raise RuntimeFailure('Trace comparison requires declared setup and a concrete packaged JAR identity.')
        recorded = json.loads((folder / 'event-recording.json').read_text())
        if recorded.get('errors') or recorded.get('thread_stopped') is not True:
            raise RuntimeFailure('Incomplete event recording cannot certify trace equality.')
        events = [json.loads(line) for line in (folder / 'events.jsonl').read_text().splitlines() if line]
        return normalize(events), setup, build['sha256']
    left, setup_a, build_a = load(a); right, setup_b, build_b = load(b)
    if setup_a != setup_b or build_a != build_b:
        raise RuntimeFailure('Compare identical declared setup and packaged JAR; different fixtures/builds are not repeatability evidence.')
    out.mkdir(parents=True)
    save_json(out / 'reference-trace.json', left); save_json(out / 'candidate-trace.json', right)
    def encoded(value): return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':'), allow_nan=False)
    first = next((i for i in range(max(len(left), len(right))) if i >= len(left) or i >= len(right) or encoded(left[i]) != encoded(right[i])), None)
    result = {'schema_version': 1, 'passed': first is None, 'reference_events': len(left), 'candidate_events': len(right),
              'first_difference': None if first is None else {'index': first, 'reference': left[first] if first < len(left) else None, 'candidate': right[first] if first < len(right) else None},
              'setup': setup_a, 'jar_sha256': build_a,
              'normalization': 'Ordered model/Ink action, external call/result, Ink line/command/variable, dialogue and scene observations. Runtime IDs, choice revisions, frames/wall/simulation/audio timing are omitted; semantic values/order remain.',
              'limits': ['Observed route only; equal traces do not prove universal determinism or all playthroughs.', 'Different traces require diagnosis; they are never silently accepted as equivalent.']}
    save_json(out / 'result.json', result)
    (out / 'report.md').write_text('# Semantic trace comparison\n\n' + ('PASS' if result['passed'] else 'DIFFERENCE at observation ' + str(first)) + '\n\n' + result['normalization'] + '\n')
    return [{'id': 'runtime.trace-compare', 'status': 'ok' if result['passed'] else 'error', 'message': 'Ordered semantic traces match.' if result['passed'] else 'Semantic traces differ; inspect first_difference and both complete traces.', 'path': str(out / 'result.json')}], result

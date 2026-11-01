"""Versioned declarative runtime scenarios and correlated step/command evidence."""
import hashlib
import json
from pathlib import Path
import re
import time
from runtime_client import RuntimeFailure, save_json, start_runtime, stop_runtime
from runtime_scenarios import Driver, EventRecorder
from runtime_ui import InputDriver
MISSING = object()
RESERVED_IDS = {'setup', 'final', 'failure', 'manifest', 'result', 'report', 'scenario', 'coverage', 'steps', 'assertions', 'input-history', 'event-recording', 'connection', 'process', 'session'}
TYPES = {'command', 'assert', 'wait', 'capture', 'click', 'key', 'frames', 'choose'}

def identifier(value):
    return isinstance(value, str) and re.fullmatch('[a-z0-9][a-z0-9-]{0,63}', value) is not None

def validate(spec):
    if not isinstance(spec, dict) or set(spec) - {'schema_version', 'id', 'description', 'setup', 'steps'}:
        raise RuntimeFailure('Scenario must be an object with known version-1 fields.')
    if type(spec.get('schema_version')) is not int or spec['schema_version'] != 1 or (not identifier(spec.get('id'))):
        raise RuntimeFailure('Scenario requires schema_version 1 and a stable lowercase id.')
    setup = spec.get('setup', {})
    if not isinstance(setup, dict) or set(setup) - {'chapter', 'scene', 'seed', 'locale', 'viewport'}:
        raise RuntimeFailure('Unknown scenario setup fields.')
    if not all((isinstance(setup.get(k), str) and setup[k] for k in ('chapter', 'scene', 'locale'))):
        raise RuntimeFailure('Setup must declare chapter, scene and locale.')
    if type(setup.get('seed')) is not int or not 0 <= setup['seed'] <= 2147483647:
        raise RuntimeFailure('Setup must declare a nonnegative 32-bit seed.')
    viewport = setup.get('viewport')
    if not isinstance(viewport, list) or len(viewport) != 2 or any((type(v) is not int for v in viewport)) or (not (320 <= viewport[0] <= 3840 and 240 <= viewport[1] <= 2160)):
        raise RuntimeFailure('Setup must declare a supported [width,height] viewport.')
    steps = spec.get('steps')
    if not isinstance(steps, list) or not 1 <= len(steps) <= 256:
        raise RuntimeFailure('A scenario requires 1–256 steps.')
    seen = set()
    for step in steps:
        if not isinstance(step, dict) or not identifier(step.get('id')) or step['id'] in seen or (step['id'] in RESERVED_IDS):
            raise RuntimeFailure('Step ids must be valid, unique and distinct from reserved evidence names.')
        seen.add(step['id'])
        kind = step.get('type')
        if kind not in TYPES:
            raise RuntimeFailure('Unsupported scenario step type: ' + str(kind))
        fields = {'id', 'type', 'label'} | {'command': {'operation', 'arguments', 'timeout_ms', 'expect_error'}, 'assert': {'condition'}, 'wait': {'condition', 'timeout_ms', 'advance_text'}, 'capture': {'kind'}, 'click': {'name', 'text', 'actor', 'point', 'button'}, 'key': {'keycode'}, 'frames': {'count'}, 'choose': {'text'}}[kind]
        if set(step) - fields:
            raise RuntimeFailure('Unknown fields in step ' + step['id'])
        if kind in ('assert', 'wait'):
            validate_condition(step.get('condition'))
        if kind == 'command' and (not isinstance(step.get('operation'), str) or not isinstance(step.get('arguments', {}), dict)):
            raise RuntimeFailure('Command requires operation and argument object.')
        if kind == 'command' and 'expect_error' in step and (not isinstance(step['expect_error'], str) or not step['expect_error']):
            raise RuntimeFailure('Expected command error must be a nonempty string.')
        if kind == 'click':
            selectors = [v for k, v in step.items() if k in ('name', 'text', 'actor', 'point')]
            if len(selectors) != 1:
                raise RuntimeFailure('A click requires exactly one selector.')
            if 'point' in step and (not isinstance(step['point'], list) or len(step['point']) != 2 or any((type(v) is not int for v in step['point']))):
                raise RuntimeFailure('Click point must be two integers.')
            if any((k in step and (not isinstance(step[k], str) or not step[k]) for k in ('name', 'text', 'actor'))):
                raise RuntimeFailure('Click selectors must be nonempty strings.')
            if type(step.get('button', 0)) is not int or not 0 <= step.get('button', 0) <= 2:
                raise RuntimeFailure('Invalid click button.')
        if kind == 'frames' and (type(step.get('count')) is not int or not 1 <= step['count'] <= 600):
            raise RuntimeFailure('Invalid frame count.')
        if kind == 'key' and (type(step.get('keycode')) is not int or not 0 <= step['keycode'] <= 255):
            raise RuntimeFailure('Invalid keycode.')
        if kind == 'choose' and (not isinstance(step.get('text'), str) or not step['text']):
            raise RuntimeFailure('Choice requires text.')
        if kind == 'capture' and step.get('kind', 'ui') not in ('ui', 'scene'):
            raise RuntimeFailure('Invalid capture kind.')
        if 'timeout_ms' in step and (type(step['timeout_ms']) is not int or not 1 <= step['timeout_ms'] <= 60000):
            raise RuntimeFailure('Invalid step deadline.')
        if 'advance_text' in step and type(step['advance_text']) is not bool:
            raise RuntimeFailure('advance_text must be boolean.')
    return spec

def validate_condition(condition, depth=0):
    if depth>4:raise RuntimeFailure('Condition nesting exceeds four levels.')
    if isinstance(condition,dict) and len(condition)==1 and next(iter(condition)) in ('all','any'):
        children=next(iter(condition.values()))
        if not isinstance(children,list) or not 1<=len(children)<=32:raise RuntimeFailure('Use 1–32 composite conditions.')
        for child in children:validate_condition(child,depth+1)
        return
    if not isinstance(condition, dict) or not isinstance(condition.get('path'), str) or (condition['path'] and (not condition['path'].startswith('/'))):
        raise RuntimeFailure('Condition requires a JSON pointer path.')
    operators = set(condition) - {'path'}
    if len(operators) != 1 or not operators <= {'equals', 'not_equals', 'contains', 'has_id', 'truthy'}:
        raise RuntimeFailure('Condition requires exactly one supported operator.')
    if re.search('~(?![01])', condition['path']):
        raise RuntimeFailure('Invalid JSON pointer escape.')
    if 'has_id' in condition and (not isinstance(condition['has_id'], str)):
        raise RuntimeFailure('has_id requires a string.')
    if 'truthy' in condition and type(condition['truthy']) is not bool:
        raise RuntimeFailure('truthy requires boolean.')

def pointer(state, path):
    value = state
    if path == '':
        return value
    for part in path[1:].split('/'):
        part = part.replace('~1', '/').replace('~0', '~')
        if isinstance(value, dict):
            value = value.get(part, MISSING)
        elif isinstance(value, list):
            if not re.fullmatch('0|[1-9][0-9]*', part):
                return MISSING
            try:
                value = value[int(part)]
            except IndexError:
                return MISSING
        else:
            return MISSING
    return value

def json_equal(value, expected):
    if type(value) is not type(expected): return False
    if isinstance(value, dict): return value.keys() == expected.keys() and all(json_equal(v, expected[k]) for k,v in value.items())
    if isinstance(value, list): return len(value) == len(expected) and all(json_equal(a,b) for a,b in zip(value,expected))
    return value == expected

def matches(state, condition):
    if 'all' in condition:return all(matches(state,c) for c in condition['all'])
    if 'any' in condition:return any(matches(state,c) for c in condition['any'])
    value = pointer(state, condition['path'])
    op = next((k for k in condition if k != 'path'))
    expected = condition[op]
    if value is MISSING:
        return False
    if op == 'equals':
        return json_equal(value, expected)
    if op == 'not_equals':
        return not json_equal(value, expected)
    if op == 'truthy':
        return bool(value) == expected
    if op == 'has_id':
        return isinstance(value, list) and any((isinstance(v, dict) and v.get('id') == expected for v in value))
    if op == 'contains':
        if isinstance(value, list): return any(json_equal(v, expected) for v in value)
        return isinstance(value, (str, dict)) and isinstance(expected, str) and expected in value
    return False

def coverage(out, final_state):
    events = []
    path = out / 'events.jsonl'
    if path.is_file():
        events = [json.loads(line) for line in path.read_text().splitlines() if line]
    save_json(out / 'transcript.json', [e for e in events if e['type'].startswith(('dialogue.', 'ink.'))])
    catalogue = {v['source']: v for v in final_state.get('source_catalogue', []) if isinstance(v, dict) and isinstance(v.get('source'), str)}
    observed = set(); started = set(); completed = set(); unmapped = []; ink_paths = set()
    external_calls = {}; external_results = []; selected_choices = []; choices = {}; transitions = []; commands = {}
    previous_scene = None
    for event in events:
        kind = event['type']; data = event.get('data')
        if not isinstance(data, dict): continue
        ref = data.get('source_ref') or {}
        if kind.startswith(('action.', 'verb.')):
            if ref.get('source'): observed.add(ref['source'])
            elif kind in ('action.started', 'verb.started'):
                unmapped.append({'type': kind, 'frame': event.get('frame'), 'runner_id': data.get('runner_id'), 'action': data.get('action')})
            if ref.get('action_pointer'):
                action = ref['resource'] + '#' + ref['action_pointer']
                if kind == 'action.started': started.add(action)
                if kind == 'action.completed': completed.add(action)
        ink_path = data.get('path') or ref.get('ink_path')
        if kind.startswith('ink.') and ink_path: ink_paths.add(str(ink_path))
        if kind == 'ink.external.call': external_calls[data['call_id']] = data
        if kind in ('ink.external.result', 'ink.external.failed'):
            external_results.append({'call': external_calls.get(data['call_id']), 'terminal': data, 'frame': event.get('frame'), 'command_id': event.get('command_id')})
        if kind == 'dialogue.choices': choices[data.get('choice_set')] = data.get('choices', [])
        if kind == 'command.started':
            commands[event.get('command_id')] = data.get('operation')
            if data.get('operation') == 'dialogue.choose':
                args = data.get('arguments') or {}; offered = choices.get(args.get('choice_set'), []); option = args.get('option')
                selected_choices.append({'text': offered[option] if type(option) is int and 0 <= option < len(offered) else None,
                    'choice_set': args.get('choice_set'), 'option': option, 'frame': event.get('frame')})
        if kind == 'scene.changed':
            operation = commands.get(event.get('command_id'))
            transitions.append({'from': previous_scene, 'to': data.get('scene'), 'operation': operation,
                'setup': operation == 'fixture.scene', 'frame': event.get('frame')})
            previous_scene = data.get('scene')
    actor_verbs = [v for v in catalogue.values() if v.get('actor') is not None]
    known_actions = {v['resource'] + '#' + v['json_pointer'] + '/actions/' + str(i)
                     for v in catalogue.values() for i in range(v.get('action_count', 0))}
    return {'schema_version': 1, 'event_types': sorted({e['type'] for e in events}),
        'source_coverage_available': bool(catalogue), 'known_model_sources': sorted(catalogue),
        'observed_sources': sorted(observed), 'unobserved_model_sources': sorted(set(catalogue) - observed),
        'known_actor_verbs': actor_verbs, 'unobserved_actor_verbs': [v for v in actor_verbs if v['source'] not in observed],
        'started_model_actions': sorted(started), 'completed_model_actions': sorted(completed),
        'unobserved_model_actions': sorted(known_actions - started), 'unmapped_action_events': unmapped,
        'observed_ink_paths': sorted(ink_paths), 'external_calls': external_results,
        'selected_choices': selected_choices, 'observed_choice_sets': list(choices.values()), 'scene_transitions': transitions,
        'scope': 'Observed runtime callbacks and current loaded-model definitions only. Unobserved is not unreachable; dynamic Ink actions and other chapters have no complete static denominator. Runtime Ink paths are not compiler source lines.'}

def run_scenario(root, config, filename, output, jar=None, engine_replay=False):
    filename = Path(filename).expanduser().resolve()
    spec = validate(json.loads(filename.read_text()))
    out = Path(output).expanduser().resolve()
    if out.exists():
        raise RuntimeFailure('Scenario output must be a new directory.')
    client = recorder = driver = session = None
    failure = None
    step_id = 'setup'
    records = []
    final_state = {}
    evidence_errors = []
    started = time.monotonic()
    try:
        client, out = start_runtime(root, config, out, jar=jar)
        save_json(out / 'scenario.json', spec)

        def observe(record):
            entry = {'step_id': step_id, 'elapsed_seconds': time.monotonic() - started, **record}
            with (out / 'commands.jsonl').open('a') as stream:
                stream.write(json.dumps(entry, ensure_ascii=False) + '\n')
        client.command_observer = observe
        recorder = EventRecorder(client, out)
        driver = Driver(client, out)
        ui = InputDriver(driver)
        session = client.debug('/session')
        needed = {'input.events', 'state.ui', 'fixture.seed', 'fixture.locale', 'fixture.viewport'}
        if engine_replay:
            needed.add('scenario.replay')
        if not needed <= set(session['capabilities']):
            raise RuntimeFailure('Required scenario capabilities unavailable: ' + str(sorted(needed - set(session['capabilities']))))
        save_json(out / 'manifest.json', {'schema_version': 1, 'scenario_id': spec['id'], 'scenario_sha256': hashlib.sha256(filename.read_bytes()).hexdigest(), 'session': session, 'setup': spec['setup'], 'supervisor_watchdog_seconds': 600})
        if engine_replay:
            job=client.command('scenario.run',{'scenario':spec},timeout_ms=60000,wait=False)
            deadline=time.monotonic()+65
            while job['status'] in ('accepted','running','cancelling'):
                progress=job.get('progress') or {}
                step_id=progress.get('step_id') or 'setup'
                save_json(out/'engine-job.json',job)
                if time.monotonic()>deadline:
                    try:client.debug('/commands/'+job['command_id']+'/cancel',{})
                    except Exception as exc:evidence_errors.append('replay cancellation: '+str(exc))
                    raise RuntimeFailure('Engine scenario supervisor watchdog at '+step_id)
                time.sleep(.1)
                job=client.debug('/commands/'+job['command_id'])
            save_json(out/'engine-job.json',job)
            step_id=(job.get('progress') or {}).get('step_id') or 'setup'
            partial=job.get('result') or {}
            records.extend(partial.get('steps',[]));driver.assertions.extend(partial.get('assertions',[]))
            save_json(out/'steps.json',records);save_json(out/'assertions.json',driver.assertions)
            for record in records:
                capture=record.get('capture')
                if capture:
                    data=client.debug('/artifacts/'+capture['artifact_id'],binary=True)
                    if hashlib.sha256(data).hexdigest()!=capture['sha256']:raise RuntimeFailure('Engine replay capture hash mismatch')
                    (out/(record['id']+'.png')).write_bytes(data)
                    receipt=client.debug('/commands/'+record['commands'][-1])['result']
                    save_json(out/(record['id']+'.json'),receipt)
            try:
                exported=client.command('inspector.export')['result']
                archive=client.debug('/artifacts/'+exported['artifact_id'],binary=True)
                if hashlib.sha256(archive).hexdigest()!=exported['sha256']:raise RuntimeFailure('Engine reproduction archive hash mismatch')
                (out/'engine-evidence.zip').write_bytes(archive)
                save_json(out/'engine-export.json',exported)
            except Exception as exc:evidence_errors.append('engine export: '+str(exc))
            final_state=client.state()
            if job['status']!='completed':raise RuntimeFailure('Engine scenario '+str(step_id)+': '+str(job.get('error')))
        else:
            setup = spec['setup']
            client.command('clock.freeze', {'value': True})
            client.command('fixture.viewport', dict(zip(('width', 'height'), setup['viewport'])))
            client.command('fixture.locale', {'locale': setup['locale']})
            client.command('fixture.seed', {'seed': setup['seed']})
            client.command('fixture.scene', {'chapter': setup['chapter'], 'scene': setup['scene']})
            client.command('wait', {'conditions': [{'path': '/assetState', 'equals': 'LOADED'}]}, timeout_ms=30000)
            client.command('input.event', {'type': 'mouseMoved', 'x': 0, 'y': 0})
            client.command('clock.step', {'frames': 4, 'delta': 0.05})
            client.capture(out, 'setup')
            for step in spec['steps']:
                step_id = step['id']
                driver.step_id = step_id
                kind = step['type']
                before = time.monotonic()
                if before - started > 600:
                    raise RuntimeFailure('Scenario supervisor watchdog expired.')
                row = {'id': step_id, 'type': kind, 'control_level': 'input' if kind in ('click', 'key') or (kind == 'command' and step['operation'] == 'input.event') else 'semantic' if kind == 'choose' or (kind == 'command' and (not step['operation'].startswith(('fixture.', 'clock.')))) else 'fixture' if kind == 'command' else 'observation'}
                records.append(row)
                save_json(out / 'steps.json', records)
                try:
                    if kind == 'command':
                        try:
                            client.command(step['operation'], step.get('arguments', {}), timeout_ms=step.get('timeout_ms', 10000))
                        except RuntimeFailure as exc:
                            if not step.get('expect_error') or step['expect_error'] not in str(exc):
                                raise
                            driver.check(True, step.get('label', step_id) + ' rejects expected error')
                        else:
                            if step.get('expect_error'):
                                raise RuntimeFailure('Expected command rejection did not occur: ' + step_id)
                    elif kind == 'assert':
                        driver.check(matches(client.state(), step['condition']), step.get('label', step_id))
                    elif kind == 'wait':
                        deadline = time.monotonic() + step.get('timeout_ms', 30000) / 1000
                        while not matches(client.state(), step['condition']):
                            if time.monotonic() > deadline:
                                raise RuntimeFailure('Scenario wait deadline: ' + step_id)
                            ui.frames(4)
                            state = client.state()
                            if step.get('advance_text') and state.get('text'):
                                client.command('dialogue.advance')
                    elif kind == 'capture':
                        client.capture(out, step_id, step.get('kind', 'ui'))
                    elif kind == 'click':
                        ui.click(step.get('point'), **{k: step[k] for k in ('name', 'text', 'actor', 'button') if k in step})
                    elif kind == 'key':
                        ui.key(step['keycode'])
                    elif kind == 'frames':
                        ui.frames(step['count'])
                    elif kind == 'choose':
                        state = client.state()
                        options = [i for i, v in enumerate(state['choices']) if step['text'] in v]
                        if len(options) != 1:
                            raise RuntimeFailure('Choice text must match one current choice.')
                        client.command('dialogue.choose', {'choice_set': state['choice_set'], 'option': options[0]})
                    state = client.state()
                    row.update(status='passed', frame=state['frame'], event_cursor=state['event_cursor'], elapsed_seconds=time.monotonic() - before)
                    save_json(out / (step_id + '-state.json'), state)
                    final_state = state
                except Exception as exc:
                    row.update(status='failed', error=str(exc))
                    raise
                finally:
                    save_json(out / 'steps.json', records)
    except Exception as exc:
        failure = str(exc)
    finally:
        if client:
            try:
                evidence_errors.extend(client.evidence(out, 'failure' if failure else 'final')['errors'])
                if client.observation_errors:
                    evidence_errors.extend(client.observation_errors)
            except Exception as exc:
                evidence_errors.append(str(exc))
        if recorder:
            try:
                recorder.close()
                evidence_errors.extend(recorder.failures)
                if recorder.thread.is_alive():
                    evidence_errors.append('Event recording thread did not stop.')
            except Exception as exc:
                evidence_errors.append('event recording: ' + str(exc))
        if (out / 'process.json').exists():
            try:
                stop_runtime(out)
            except Exception as exc:
                evidence_errors.append('cleanup: ' + str(exc))
        if engine_replay and session and failure:
            try:
                from runtime_recovery import recover_captures
                recovered = recover_captures(out, session, spec)
                evidence_errors.extend(recovered['errors'])
            except Exception as exc:
                evidence_errors.append('capture recovery: ' + str(exc))
        if evidence_errors and (not failure):
            failure = 'Evidence/cleanup incomplete; see evidence_errors.'
        if out.exists():
            try:
                save_json(out / 'coverage.json', coverage(out, final_state))
            except Exception as exc:
                evidence_errors.append('coverage: ' + str(exc))
                if not failure:
                    failure = 'Coverage evidence could not be assembled.'
        result = {'schema_version': 1, 'scenario_id': spec['id'], 'runner': 'engine' if engine_replay else 'client', 'passed': failure is None, 'error': failure, 'failed_step': step_id if failure else None, 'steps': records, 'assertions': driver.assertions if driver else [], 'evidence_errors': evidence_errors, 'elapsed_seconds': time.monotonic() - started, 'limitations': ['Setup is a debug fixture, not player reachability.', 'Controlled world clock/seeds do not fix OS/IO/audio timing.', 'Input events reach the actual game processor; native OS delivery is not tested.']}
        if out.exists():
            save_json(out / 'result.json', result)
            (out / 'report.md').write_text('# ' + spec['id'] + '\n\n' + ('PASS' if not failure else 'FAIL: ' + failure) + '\n\n' + '\n'.join(('- ' + r['id'] + ': ' + r.get('status', 'incomplete') for r in records)) + '\n')
    return ([{'id': 'runtime.scenario', 'status': 'error' if failure else 'ok', 'message': failure or 'Scenario passed: ' + spec['id'], 'path': str(out / 'result.json')}], result)

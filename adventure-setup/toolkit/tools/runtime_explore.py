"""Bounded seeded development exploration using only player-projected action choices."""
import hashlib
import json
from pathlib import Path
import random
import time

from runtime_client import RuntimeFailure, save_json, start_runtime, stop_runtime
from runtime_scenario import validate, coverage
from runtime_scenarios import Driver, EventRecorder


READY = {'all': [{'path': '/assetState', 'equals': 'LOADED'},
                 {'path': '/paused', 'equals': False},
                 {'any': [{'path': '/choices', 'truthy': True},
                          {'all': [{'path': '/cutMode', 'equals': False},
                                   {'path': '/text', 'equals': None}]}]}]}


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(',', ':'), allow_nan=False)


def available_actions(player):
    """No hidden actors, internal scene verbs, fixture writes, arbitrary saves or menus."""
    if player.get('paused') or player.get('assetState') != 'LOADED':
        return [], []
    choices = player.get('dialogOptions', [])
    if choices:
        ambiguous = sorted({text for text in choices if choices.count(text) > 1})
        return ([{'kind': 'choose', 'text': text} for text in sorted(set(choices)) if text not in ambiguous],
                [{'reason': 'ambiguous_choice_text', 'text': text} for text in ambiguous])
    if player.get('cutMode'):
        return [], []
    actors = player.get('actors', [])
    inventory = player.get('inventory', [])
    targets = sorted({v['id'] for v in actors + inventory})
    actions = []
    for actor in actors + inventory:
        for verb in actor.get('verbs', []):
            if verb == 'use':
                if actor not in inventory:
                    continue
                for target in targets:
                    if target != actor['id']:
                        actions.append({'kind': 'verb', 'actor': actor['id'], 'verb': verb, 'target': target})
            else:
                actions.append({'kind': 'verb', 'actor': actor['id'], 'verb': verb})
    unique = {canonical(v): v for v in actions}
    return [unique[key] for key in sorted(unique)], []


def progress_digest(state):
    """Logical changes only; ambient animation, frames, RNG, position and audio excluded."""
    scene = state.get('scene') or {}
    logical = {'chapter': state.get('chapter'), 'scene': scene.get('id'),
               'inventory': sorted(v['id'] for v in state.get('inventory', [])),
               'actors': sorted((v['id'], v.get('visible'), v.get('state')) for v in scene.get('actors', [])),
               'properties': state.get('properties'), 'ink_variables': (state.get('ink') or {}).get('variables')}
    return hashlib.sha256(canonical(logical).encode()).hexdigest()


def select_action(actions, digest, visited, rng):
    untried = [a for a in actions if (digest, canonical(a)) not in visited]
    pool = untried or actions
    return rng.choice(pool) if pool else None


def run_exploration(root, config, filename, output, jar=None):
    spec = json.loads(Path(filename).read_text())
    if not isinstance(spec, dict) or set(spec) != {'schema_version', 'id', 'setup', 'max_actions', 'stall_actions'}:
        raise RuntimeFailure('Exploration requires version, id, setup, max_actions and stall_actions.')
    route = {'schema_version': spec['schema_version'], 'id': spec['id'], 'setup': spec['setup'],
             'description': 'Seeded semantic development exploration; explicit fixture and player-projected candidates.',
             'steps': [{'id': 'initial-ready', 'type': 'wait', 'condition': READY, 'advance_text': True, 'timeout_ms': 20000}]}
    validate(route)
    if type(spec['max_actions']) is not int or not 1 <= spec['max_actions'] <= 40:
        raise RuntimeFailure('Explore 1–40 actions per route; split longer sessions at declared checkpoints.')
    if type(spec['stall_actions']) is not int or not 2 <= spec['stall_actions'] <= 40:
        raise RuntimeFailure('stall_actions must be 2–40.')
    root = Path(root).resolve(); out = Path(output).expanduser().resolve()
    if out.exists() or any(out.is_relative_to(root / f) for f in ('assets', 'raw', 'tools', 'skills', '.git')):
        raise RuntimeFailure('Exploration output must be new and outside project source/assets/tools.')
    client = recorder = None
    errors = []; failure = None; finding = None; decisions = []; visited = set(); all_candidates = set()
    rng = random.Random(spec['setup']['seed']); unchanged = 0; state = {}
    started = time.monotonic()

    def retain():
        save_json(out / 'route.json', validate(route))
        save_json(out / 'decisions.json', decisions)

    def settle():
        from runtime_scenario import matches
        deadline = time.monotonic() + 20
        # Flush deferred gameplay callbacks before evaluating readiness.
        client.command('clock.step', {'frames': 4, 'delta': .05})
        while True:
            current = client.state()
            diagnostic = current.get('diagnostics') or {}
            ink_errors = (current.get('ink') or {}).get('errors')
            if diagnostic.get('last_error') or diagnostic.get('exception') or ink_errors:
                condition = {'all': [{'path': '/diagnostics/last_error', 'equals': None},
                                     {'path': '/diagnostics/exception', 'equals': None},
                                     {'path': '/ink/errors', 'equals': None}]}
                route['steps'].append({'id': 'runtime-error-guard', 'type': 'assert', 'condition': condition})
                retain()
                raise RuntimeFailure('Observed runtime error: ' + canonical({'diagnostics': diagnostic, 'ink_errors': ink_errors}))
            if matches(current, READY):
                return current
            if time.monotonic() >= deadline:
                raise RuntimeFailure('Exploration readiness deadline; retained wait step is the reproduction candidate.')
            if current.get('text'):
                client.command('dialogue.advance')
            client.command('clock.step', {'frames': 4, 'delta': .05})

    try:
        client, out = start_runtime(root, config, out, jar=jar)
        save_json(out / 'exploration.json', spec)
        recorder = EventRecorder(client, out)
        def observe(record):
            with (out / 'commands.jsonl').open('a') as stream:
                stream.write(canonical({'decision': len(decisions), **record}) + '\n')
        client.command_observer = observe
        setup = spec['setup']
        client.command('clock.freeze', {'value': True})
        client.command('fixture.viewport', dict(zip(('width', 'height'), setup['viewport'])))
        client.command('fixture.locale', {'locale': setup['locale']})
        client.command('fixture.seed', {'seed': setup['seed']})
        client.command('fixture.scene', {'chapter': setup['chapter'], 'scene': setup['scene']})
        retain(); state = settle(); client.capture(out, 'entry')
        for index in range(spec['max_actions']):
            if time.monotonic() - started > 600:
                raise RuntimeFailure('Exploration supervisor deadline.')
            player = client.request('/state')
            actions, skipped = available_actions(player)
            digest = progress_digest(state)
            all_candidates.update(canonical(v) for v in actions)
            action = select_action(actions, digest, visited, rng)
            decision = {'index': index, 'logical_digest': digest, 'action': action,
                        'player_state': player, 'candidates': actions, 'skipped': skipped,
                        'frame': state['frame'], 'event_cursor': state['event_cursor']}
            decisions.append(decision); retain()
            if action is None:
                finding = 'no_supported_player_actions'; break
            fresh, _ = available_actions(client.request('/state'))
            if action not in fresh:
                decision['status'] = 'stale_candidate'
                finding = 'player_projection_changed'; break
            visited.add((digest, canonical(action)))
            prefix = 'action-%02d' % index
            if action['kind'] == 'choose':
                offered = state['choices']
                if offered.count(action['text']) != 1:
                    raise RuntimeFailure('Current dialogue differs from player projection.')
                step = {'id': prefix, 'type': 'choose', 'text': action['text']}
                route['steps'].append(step); retain()
                client.command('dialogue.choose', {'choice_set': state['choice_set'], 'option': offered.index(action['text'])})
            else:
                args = {'type': 'actorVerb', 'actorId': action['actor'], 'verb': action['verb']}
                if 'target' in action: args['target'] = action['target']
                route['steps'].append({'id': prefix, 'type': 'command', 'operation': 'gameplay', 'arguments': args})
                retain(); client.command('gameplay', args)
            route['steps'].extend([{'id': prefix + '-deferred', 'type': 'frames', 'count': 4},
                                  {'id': prefix + '-ready', 'type': 'wait', 'condition': READY,
                                   'advance_text': True, 'timeout_ms': 20000}])
            retain(); state = settle()
            changed = progress_digest(state) != digest
            unchanged = 0 if changed else unchanged + 1
            decision.update(status='completed', logical_progress=changed, unchanged_actions=unchanged)
            retain()
            client.capture(out, prefix)
            if unchanged >= spec['stall_actions']:
                finding = 'lack_of_logical_progress_candidate'; break
    except Exception as exc:
        failure = str(exc)
    finally:
        if client:
            try: errors.extend(client.evidence(out, 'failure' if failure else 'final')['errors']); errors.extend(client.observation_errors)
            except Exception as exc: errors.append(str(exc))
        if recorder:
            try:
                recorder.close(); errors.extend(recorder.failures)
                if recorder.thread.is_alive(): errors.append('Event recorder did not stop.')
            except Exception as exc: errors.append(str(exc))
        if (out / 'process.json').exists():
            try: stop_runtime(out)
            except Exception as exc: errors.append('cleanup: ' + str(exc))
        if errors and not failure: failure = 'Exploration evidence incomplete.'
        result = {'schema_version': 1, 'suite': 'exploration', 'passed': failure is None, 'error': failure,
                  'finding': finding, 'actions_completed': sum(v.get('status') == 'completed' for v in decisions),
                  'unique_state_actions': len(visited), 'evidence_errors': errors,
                  'untried_observed_actions': sorted(all_candidates - {canonical(v['action']) for v in decisions if v.get('status') == 'completed'}),
                  'limitations': ['Development fixture; not blind player evaluation or a full walkthrough.',
                                  'Candidates use the player projection; internal state is used for error/progress diagnosis only.',
                                  'No progress is a policy finding for human review, not proof of a dead end.',
                                  'Only exposed semantic actions are explored; free walking, arbitrary screen clicks and unseen routes remain uncovered.',
                                  'Replay candidates require independent verification; long routes may exceed the engine replay deadline.']}
        if out.exists():
            retain(); save_json(out / 'result.json', result)
            try: save_json(out / 'coverage.json', coverage(out, state))
            except Exception as exc:
                errors.append(str(exc)); result.update(passed=False, error=failure or 'Coverage evidence incomplete.')
                save_json(out / 'result.json', result)
            (out / 'report.md').write_text('# Seeded exploration\n\n' + (result['error'] or finding or 'Action budget reached')
                + '\n\nCompleted actions: ' + str(result['actions_completed']) + '\n\nSee decisions.json, route.json and coverage.json; unseen actions are not proven unreachable.\n')
    status = 'error' if not result['passed'] else 'warning' if finding else 'ok'
    return [{'id': 'runtime.explore', 'status': status, 'message': result['error'] or finding or 'Bounded exploration completed.',
             'path': str(out / 'result.json')}], result

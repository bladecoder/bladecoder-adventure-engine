"""Generic cutout preview/package checks through the shared runtime controller."""
from pathlib import Path
import shutil

from art_pipeline import create_output, sha, fingerprints
from asset_manifest import require, json_file
from art_sources import relative_path
from spine_tools import read_recipe
from project_config import load_project
from runtime_client import start_runtime, stop_runtime, save_json
from runtime_scenarios import Driver, EventRecorder


def fixture_model(name, animations, viewport, engine_version):
    require(bool(animations), 'The preview must contain at least one animation.')
    width, height = viewport
    return {'bladeEngineVersion': engine_version, 'sounds': {}, 'initScene': 'spine-preview', 'scenes': {
        'spine-preview': {'id': 'spine-preview', 'layers': [{'name': 'dynamic', 'dynamic': True}], 'verbs': {},
            'actors': {'preview-character': {'class': 'SpriteActor', 'id': 'preview-character', 'visible': True,
                'interaction': False, 'verbs': {}, 'layer': 'dynamic', 'pos': {'x': width / 2, 'y': height * .2},
                'bbox': [-64, 0, 64, 0, 64, 80, -64, 80], 'bboxFromRenderer': True,
                'renderer': {'class': 'com.bladecoder.engine.spine.SpineRenderer', 'initAnimation': animations[0], 'orgAlign': 4,
                    'fanims': {animation: {'class': 'SpineAnimationDesc', 'id': animation, 'source': name, 'atlas': name,
                        'duration': 1, 'animationType': 'REPEAT', 'count': -1, 'preload': True} for animation in animations}}}}}}}


def probe(root, config, output, selected, jar=None, fixture=None, expected=None):
    client = recorder = driver = None; errors = []
    result = {'schema_version': 1, 'passed': False, 'scope': 'actual Spine renderer; semantic animation probes',
              'limitations': ['Debug scene setup is not player reachability.', 'Captures and pose assertions do not certify artistic quality.']}
    out = Path(output).resolve()
    try:
        client, out = start_runtime(root, config, out, scene=selected['scene'], chapter=selected.get('chapter', '00'), jar=jar, asset_fixture=fixture)
        recorder = EventRecorder(client, out); driver = Driver(client, out)
        client.command('clock.freeze', {'value': True})
        def actor(): return driver.actor(client.state(), selected['actor'])
        def details(): return actor()['renderer']['details']
        initial = details()
        driver.check(initial.get('loaded'), 'actual Spine skeleton loaded')
        driver.check(initial['version'] == selected['editor_version'], 'exported Spine version loaded by matching runtime')
        driver.check(initial['source'] == selected['source'] and initial['atlas'] == selected['atlas'], 'configured skeleton and atlas selected')
        driver.check(set(selected['animations']) <= set(initial['animations']), 'all declared animations loaded')
        if expected:
            driver.check({bone['name'] for bone in initial['bones']} == {bone['name'] for bone in expected['bones']}, 'all template bones retained')
            driver.check({slot['name'] for slot in initial['slots']} == {slot['name'] for slot in expected['slots']}, 'all template slots retained')
        driver.check(not initial['premultiplied_alpha'], 'straight-alpha renderer convention preserved')
        # Frame the diagnostic fixture without changing the artist project or game model.
        if fixture:
            current = actor(); viewport = load_project(root).get('viewport', [960, 540])
            scale = min(1, viewport[0] * .65 / max(current['width'], 1), viewport[1] * .65 / max(current['height'], 1))
            client.command('fixture.actor', {'actor': selected['actor'], 'scaleX': scale, 'scaleY': scale})
        poses = []; motion = {name: [] for name in selected.get('motion_bones', [])}
        for index, animation in enumerate(selected['animations']):
            client.command('animation.play', {'actor': selected['actor'], 'animation': animation})
            before = details(); track = before['tracks'][0]
            driver.check(track['animation'] == animation, 'actual track selects ' + animation)
            frames = max(1, min(30, round(max(track['duration'], .4) / 8 / .05)))
            for sample in range(8):
                client.command('clock.step', {'frames': frames, 'delta': .05})
                current = details(); bones = {bone['name']: bone for bone in current['bones']}
                for bone in motion:
                    driver.check(bone in bones, 'declared motion bone exists: ' + bone)
                    motion[bone].append(tuple(bones[bone][key] for key in ('x', 'y', 'rotation')))
                poses.append({'animation': animation, 'sample': sample, 'details': current})
            driver.check(details()['tracks'][0]['time'] > track['time'], 'actual track time advances: ' + animation)
            client.capture(out, 'animation-' + str(index))
        for bone, values in motion.items():
            driver.check(any(any(abs(a - b) > .01 for a, b in zip(values[0], value)) for value in values[1:]), 'actual animated bone changes: ' + bone)
        save_json(out / 'poses.json', poses)
        client.command('gameplay', {'type': 'saveGame', 'target': 'spine-smoke.gamestate'})
        client.command('gameplay', {'type': 'loadGame', 'target': 'spine-smoke.gamestate'})
        client.command('wait', {'conditions': [{'path': '/assetState', 'equals': 'LOADED'}]}, timeout_ms=30000)
        driver.check(details()['loaded'] and details()['source'] == selected['source'], 'Spine renderer survives save/load')
        driver.check(client.state()['diagnostics']['last_error'] is None, 'no engine resource or animation errors')
        result['passed'] = True
    except Exception as error:
        result['error'] = str(error)
    finally:
        if client:
            try: errors.extend(client.evidence(out, 'final' if result['passed'] else 'failure')['errors'])
            except Exception as error: errors.append('evidence: ' + str(error))
        if recorder:
            try:
                recorder.close(); errors.extend(recorder.failures)
                if recorder.thread.is_alive(): errors.append("Event recorder thread did not stop.")
            except Exception as error: errors.append('event recorder: ' + str(error))
        if client:
            try: stop_runtime(out)
            except Exception as error: errors.append('owned process cleanup: ' + str(error))
    result['evidence_errors'] = errors
    if errors: result['passed'] = False
    result['assertions'] = driver.assertions if driver else []
    if out.exists(): save_json(out / 'spine-result.json', result)
    return result


def test(root, config, filename, output):
    require(output, 'Use --out NEW_DIRECTORY for runtime evidence.')
    root = Path(root).resolve(); report_path = Path(filename).expanduser().resolve(); report = json_file(report_path)
    require(report.get('kind') == 'spine-preview' and report.get('schema_version') == 1, 'Test a reviewed Spine preview report.')
    recipe, _, _, _, _ = read_recipe(root, config, report['recipe']); name = recipe['output']['name']
    require(fingerprints(report['inputs']) == report['inputs'], 'Preview inputs changed; regenerate before testing.')
    metadata = load_project(root)
    require(metadata.get('capabilities', {}).get('spine'), 'This project must select the Spine plugin capability.')
    out = create_output(output, root); content = out / 'asset-fixture'; content.mkdir(); (content / 'model').mkdir()
    model = fixture_model(name, report['animations'], metadata.get('viewport', [960, 540]), metadata['engine_version'])
    save_json(content / 'model/00.chapter.json', model)
    for row in report['files']:
        candidate = (report_path.parent / row['preview']).resolve()
        require(candidate.is_relative_to(report_path.parent) and sha(candidate) == row['sha256'], 'Stale or escaping preview candidate.')
        if row['root'] == 'repo' and row['output'].startswith('assets/'):
            relative_path(row['output']); target = content / Path(row['output']).relative_to('assets')
            require(target.resolve().is_relative_to(content.resolve()), 'Runtime fixture output escapes content directory.')
            target.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(candidate, target)
    selected = {'scene': 'spine-preview', 'actor': 'preview-character', 'source': name, 'atlas': name,
                'editor_version': recipe['editor_version'], 'animations': report['animations']}
    result = probe(root, config, out / 'session', selected, fixture=content, expected=report['template'])
    save_json(out / 'result.json', result)
    return [{'id': 'spine.test', 'status': 'ok' if result['passed'] else 'error', 'message': result.get('error') or 'Generic Spine preview verified through actual runtime.'}], result


def packaged_spine(root, config, output, jar, profile):
    selected = load_project(root).get('tests', {}).get('spine', {}).get(profile)
    require(selected is not None, 'No declared Spine test profile: ' + profile)
    result = probe(root, config, output, selected, jar=jar)
    return [{'id': 'spine.test', 'status': 'ok' if result['passed'] else 'error', 'message': result.get('error') or 'Declared packaged Spine profile verified.'}], result

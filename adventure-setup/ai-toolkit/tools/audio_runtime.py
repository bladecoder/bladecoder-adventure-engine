"""Project-declared native audio probes through the common controller/private sink."""
import json
from pathlib import Path
import subprocess
import shutil
import time
import wave

from audio_capture import isolated_sink
from audio_match import match, samples
from audio_tools import pcm_metrics, program, sha
from runtime_client import start_runtime, stop_runtime, RuntimeFailure, save_json
from runtime_scenarios import Driver, EventRecorder
from project_config import load_project


def read_profile(root, cue):
    selected = load_project(root).get('tests', {}).get('audio', {}).get(cue)
    if selected is None:
        raise ValueError('No audio test profile declared in adventure.json: ' + cue)
    return selected


def record(root, config, sink, output, seconds, action):
    process = None
    try:
        with output.with_suffix('.log').open('wb') as log:
            process = subprocess.Popen([program(root, config, 'ffmpeg'), '-nostdin', '-v', 'warning', '-y',
                '-f', 'pulse', '-i', sink + '.monitor', '-t', str(seconds), '-ar', '48000', '-ac', '2',
                '-c:a', 'pcm_s16le', '-flush_packets', '1', str(output)], stdout=log, stderr=log)
            deadline = time.monotonic() + 5
            while not output.exists() or output.stat().st_size <= 44:
                if process.poll() is not None or time.monotonic() > deadline:
                    raise RuntimeFailure('Private audio recorder did not become ready.')
                time.sleep(.02)
            action(process)
            process.wait(seconds + 10)
            if process.returncode:
                raise RuntimeFailure('Private audio capture failed; inspect its log.')
    finally:
        if process is not None and process.poll() is None:
            process.kill(); process.wait(5)


def test_audio(root, config, directory, keep=None, jar=None, cue=None):
    root = Path(root).resolve(); out = Path(directory).expanduser().resolve()
    selected = read_profile(root, cue)
    if keep:
        raise ValueError('Native captures are retained in --out; --keep-intermediates is for audio generation/import.')
    if out.exists(): raise ValueError('Audio evidence requires a new directory.')
    client = recorder = driver = None
    errors = []
    result = {'schema_version': 1, 'profile': cue, 'passed': False,
              'control_level': 'controlled native audio probes; optional project scenario',
              'capture_scope': 'owned game output only; private null sink',
              'perceptual_review': 'pending; measurements do not approve listening quality',
              'limitations': ['Linux Pulse/PipeWire capture requires the optional native audio stack.',
                              'Debug scene setup does not establish player reachability.']}
    sink_ready = False
    try:
        if not shutil.which(program(root, config, 'ffmpeg')):
            raise ValueError('FFmpeg is unavailable for private audio capture; configure paths.ffmpeg.')
        with isolated_sink() as sink:
            sink_ready = True
            try:
                client, out = start_runtime(root, config, out, scene=selected['scene'], jar=jar, audio_sink=sink, chapter=selected.get('chapter', '00'))
                recorder = EventRecorder(client, out); driver = Driver(client, out)
                client.command('audio.sfx', {'action': 'stop_all'})
                music = selected.get('music')
                if music:
                    deadline = time.monotonic() + 10
                    state = client.state('/audio/music')['value']
                    while not state.get('playing') and time.monotonic() < deadline:
                        time.sleep(.05); state = client.state('/audio/music')['value']
                    driver.check(state['resource'] == music and state['playing'], 'configured music plays in native backend')
                    paused = client.command('audio.music', {'action': 'pause'})['result']
                    driver.check(paused['paused'] and not paused['playing'], 'native music pauses')
                    original_volume = state['volume']
                    changed = client.command('audio.music', {'action': 'volume', 'volume': .2})['result']
                    driver.check(abs(changed['effective_volume'] - .2 * changed['bus_multiplier']) < .001, 'native music volume is observable')
                    client.command('audio.music', {'action': 'volume', 'volume': original_volume})
                    client.command('audio.music', {'action': 'resume'})
                    positions = []; wraps = [0]
                    def during_music(process):
                        deadline = time.monotonic() + selected.get('capture_seconds', 8) + 10
                        if selected.get('scenario'):
                            from runtime_scenario import validate
                            spec = validate(json.loads((root / selected['scenario']).read_text()))
                            job = client.command('scenario.run', {'scenario': spec}, timeout_ms=60000)
                            save_json(out / 'audio-scenario.json', job)
                        client.command('clock.freeze', {'value': False})
                        while process.poll() is None:
                            if time.monotonic() > deadline: raise RuntimeFailure('Private music capture watchdog expired.')
                            state = client.state('/audio/music')['value']
                            if positions and positions[-1]['position'] > state['position'] + .1: wraps[0] += 1
                            positions.append(state); save_json(out / 'music-positions.json', positions)
                            time.sleep(.08)
                    capture = out / 'native-music.wav'
                    record(root, config, sink, capture, selected.get('capture_seconds', 8), during_music)
                    metrics = pcm_metrics(capture)
                    driver.check(metrics['clipped_samples'] == 0 and metrics['rms_dbfs'] > -55 and metrics['peak_dbfs'] < -.5,
                                 'private music output is active and unclipped')
                    driver.check(wraps[0] >= selected.get('min_music_wraps', 0), 'configured native loop wraps observed')
                    result['music'] = {'resource': music, 'source_sha256': sha(root / 'assets/music' / music),
                        'capture_sha256': sha(capture), 'metrics': metrics, 'wraps': wraps[0]}
                    client.command('audio.music', {'action': 'pause'})
                else:
                    client.command('audio.music', {'action': 'stop'})
                result['sounds'] = []
                for sound in selected.get('sounds', []):
                    identifier, resource = sound['id'], sound['resource']
                    source = root / 'assets/sounds' / resource
                    with wave.open(str(source), 'rb') as wav:
                        driver.check(wav.getsampwidth() == 2 and wav.getcomptype() == 'NONE', 'source is PCM16: ' + identifier)
                        duration = wav.getnframes() / wav.getframerate(); channels = wav.getnchannels()
                    driver.check(.02 <= duration <= 5 and channels in (1, 2), 'bounded mono/stereo SFX: ' + identifier)
                    source_metrics = pcm_metrics(source)
                    client.command('audio.sfx', {'action': 'stop_all'}); time.sleep(.25)
                    capture = out / ('sfx-' + identifier + '.wav')
                    def start_sound(process):
                        time.sleep(.12)
                        state = client.command('audio.sfx', {'action': 'play', 'id': identifier})['result']
                        actual = next((item for item in state['sounds'] if item['id'] == identifier), None)
                        driver.check(actual is not None and actual['resource'] == resource and actual['start_accepted'],
                                     'native SFX start accepted: ' + identifier)
                        driver.check(not actual['loop'] and actual['pitch'] == 1, 'one-shot identity probe uses unshifted source: ' + identifier)
                    record(root, config, sink, capture, duration + 1.2, start_sound)
                    metrics = pcm_metrics(capture)
                    baseline = max(abs(value) for value in samples(capture, 0, .08))
                    tail = max(abs(value) for value in samples(capture, metrics['seconds'] - .08, .08))
                    driver.check(baseline < .001 and tail < .001, 'SFX starts/returns to silence: ' + identifier)
                    driver.check(metrics['clipped_samples'] == 0 and metrics['rms_dbfs'] > -55 and metrics['peak_dbfs'] < -.5, 'active unclipped SFX output: ' + identifier)
                    identity = match(capture, source, .8)
                    driver.check(identity['correlation'] > .9, 'native waveform matches source: ' + identifier)
                    if channels == 2 and source_metrics['stereo_nonidentical_frames'] > 100:
                        driver.check(metrics['stereo_nonidentical_frames'] > 100, 'native stereo image preserved: ' + identifier)
                    result['sounds'].append({'id': identifier, 'resource': resource, 'source_sha256': sha(source),
                        'capture_sha256': sha(capture), 'metrics': metrics, 'source_metrics': source_metrics, 'waveform_match': identity})
                    save_json(out / 'audio-result.json', result)
                client.capture(out, 'audio-final')
                driver.check(client.state()['diagnostics']['last_error'] is None, 'no engine diagnostic error')
                result['passed'] = True
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
    except Exception as error:
        result['error'] = str(error); result['passed'] = False
        if not sink_ready: result['qa_unavailable'] = True
    result['evidence_errors'] = errors
    if errors: result['passed'] = False
    result['assertions'] = driver.assertions if driver else []
    if out.exists(): save_json(out / 'audio-result.json', result)
    return result

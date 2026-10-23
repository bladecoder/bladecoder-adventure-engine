"""Shared local controller client and isolated desktop process/evidence management."""
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, urlopen
import uuid

from ink_tools import java_program
from project_config import toolkit_path
from project_config import toolkit_path


class RuntimeFailure(RuntimeError):
    pass


def save_json(path, data):
    Path(path).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


class RuntimeClient:
    def __init__(self, info):
        if not isinstance(info,dict) or not all(isinstance(info.get(k),str) and info[k] for k in ('url','session_id','mode')):
            raise RuntimeFailure('Invalid discovery record: url, session_id and mode are required.')
        if info['mode'] not in ('player','debug') or info.get('protocol',1)!=1:
            raise RuntimeFailure('Unsupported controller mode or protocol.')
        parsed = urlsplit(info['url'])
        if parsed.scheme != 'http' or parsed.hostname != '127.0.0.1' or not parsed.port:
            raise RuntimeFailure('Only a local engine controller is supported.')
        self.url = info['url'].rstrip('/')
        self.session = info['session_id']
        self.mode = info['mode']
        self.token = info.get('token')
        self.command_observer = None
        self.observation_errors = []
        self.health()

    @classmethod
    def connect(cls, path):
        path = Path(path).expanduser().resolve()
        if os.name == 'posix' and path.stat().st_mode & 0o077:
            raise RuntimeFailure('Discovery file must be private to its owner.')
        return cls(json.loads(path.read_text()))

    def request(self, path, body=None, binary=False, timeout=5):
        headers = {'Accept': 'application/json'}
        if self.token:
            headers['Authorization'] = 'Bearer ' + self.token
        payload = None
        if body is not None:
            payload = json.dumps(body, sort_keys=True, separators=(',', ':'), allow_nan=False).encode()
            headers['Content-Type'] = 'application/json'
        try:
            with urlopen(Request(self.url + path, data=payload, headers=headers), timeout=timeout) as response:
                data = response.read()
                return data if binary else json.loads(data)
        except HTTPError as exc:
            error = exc.read().decode('utf-8', 'replace')
            raise RuntimeFailure(f'Controller HTTP {exc.code}: {error[:2000]}') from exc
        except (URLError, TimeoutError, OSError) as exc:
            raise RuntimeFailure(f'Controller connection failed: {type(exc).__name__}') from exc

    def health(self):
        result = self.request('/health')
        if result.get('session_id') != self.session or result.get('mode') != self.mode:
            raise RuntimeFailure('Controller session/mode changed; reconnect explicitly.')
        return result

    def debug(self, path, body=None, **kwargs):
        if self.mode != 'debug':
            raise RuntimeFailure('This operation requires an explicitly started debug session.')
        self.health()
        return self.request('/debug/v1' + path, body, **kwargs)

    def state(self, path=None):
        if self.mode == 'player':
            if path:raise RuntimeFailure('Focused internal queries require debug mode.')
            self.health();return self.request('/state')
        return self.debug('/state' + ('?' + urlencode({'path': path}) if path else ''))

    def events(self, after=0, wait_ms=0):
        if self.mode == 'player':
            if after or wait_ms:raise RuntimeFailure('Player events use the existing last-action buffer.')
            self.health();return self.request('/events')
        return self.debug('/events?' + urlencode({'after': after, 'wait_ms': wait_ms}), timeout=max(5, wait_ms/1000+3))

    def command(self, operation, arguments=None, timeout_ms=10000, request_id=None, wait=True):
        payload = {'request_id': request_id or str(uuid.uuid4()), 'operation': operation,
                   'arguments': arguments or {}, 'timeout_ms': timeout_ms}
        self._observe_command({'phase':'submitted','payload':payload})
        try:
            result = self._command_result(payload, wait)
        except Exception as exc:
            self._observe_command({'phase':'failed','request_id':payload['request_id'],'error':str(exc)})
            raise
        self._observe_command({'phase':'returned','request_id':payload['request_id'],'job':result})
        return result

    def _observe_command(self, record):
        if self.command_observer:
            try:self.command_observer(record)
            except Exception as exc:self.observation_errors.append(str(exc))

    def _command_result(self, payload, wait):
        operation=payload['operation'];timeout_ms=payload['timeout_ms']
        result = self.debug('/commands', payload)
        if not wait:
            return result
        deadline = time.monotonic() + timeout_ms/1000 + 3
        while result['status'] in ('accepted', 'running', 'cancelling'):
            if time.monotonic() >= deadline:
                raise RuntimeFailure('Command watchdog expired: ' + result['command_id'])
            time.sleep(.03)
            result = self.debug('/commands/' + result['command_id'])
        if result['status'] != 'completed':
            raise RuntimeFailure(f"{operation}: {result['status']}: {result.get('error')}")
        return result

    def capture(self, directory, name='capture', kind='ui'):
        result = self.command('capture', {'kind': kind})['result']
        data = self.debug('/artifacts/' + result['artifact_id'], binary=True)
        if hashlib.sha256(data).hexdigest() != result['sha256']:
            raise RuntimeFailure('Capture hash mismatch.')
        path = Path(directory) / (name + '.png')
        path.write_bytes(data)
        save_json(path.with_suffix('.json'), result)
        return result

    def evidence(self, directory, label='checkpoint'):
        directory = Path(directory);directory.mkdir(parents=True, exist_ok=True)
        report = {'session_id': self.session, 'mode': self.mode, 'errors': []}
        for name, call in [('state', self.state), ('events', self.events)]:
            try:
                data = call();save_json(directory / (label+'-'+name+'.json'), data)
            except Exception as exc:
                report['errors'].append({'artifact': name, 'error': str(exc)})
        try:
            self.capture(directory, label+'-ui')
        except Exception as exc:
            report['errors'].append({'artifact': 'capture', 'error': str(exc)})
        save_json(directory/(label+'-evidence.json'), report)
        return report


def process_identity(pid):
    try:
        # Linux start ticks disambiguate PID reuse; no process is killed without this identity.
        text = Path(f'/proc/{pid}/stat').read_text()
        return text[text.rfind(')')+2:].split()[19]
    except (OSError, IndexError):
        return None


def start_runtime(root, config, directory, mode='debug', scene=None, jar=None, asset_fixture=None, audio_sink=None, overrides=None, hidden=True, chapter='00'):
    root = Path(root).resolve();out = Path(directory).expanduser().resolve()
    if audio_sink and not re.fullmatch(r'adventure_test_[a-f0-9]{32}',audio_sink):
        raise RuntimeFailure('Audio capture must use an owned isolated test sink.')
    if mode=='player' and scene:raise RuntimeFailure('--scene is a debug fixture; player mode uses normal new/load controls.')
    if out.is_relative_to(root/'assets') or out.is_relative_to(root/'raw'):
        raise RuntimeFailure('Evidence must stay outside packaged/source assets.')
    out.mkdir(parents=True, exist_ok=False)
    java = java_program(config, root)
    if not java:
        raise RuntimeFailure('A working Java runtime is required.')
    if jar and asset_fixture:raise RuntimeFailure('Asset fixtures use the development classpath; packaged overrides are a separate workflow.')
    if jar:
        jar = Path(jar).expanduser().resolve()
        arguments = ['-jar', str(jar)]
        identity = {'jar': str(jar), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}
    else:
        result = subprocess.run([str(root/'gradlew'),'--offline','--console=plain','-I',str(toolkit_path(root, "automation.gradle")),
                                 ':desktop:classes',':desktop:automationClasspath'], cwd=root, capture_output=True,text=True,timeout=60)
        match = re.search(r'^AUTOMATION_CP=(.+)$', result.stdout, re.M)
        if result.returncode or not match:
            (out/'build.log').write_text(result.stdout+result.stderr)
            raise RuntimeFailure('Desktop classpath preparation failed; see build.log.')
        classpath=match[1]
        if asset_fixture:
            fixture=Path(asset_fixture).resolve()
            if not fixture.is_dir() or fixture.is_relative_to(root/'assets') or fixture.is_relative_to(root/'raw'):
                raise RuntimeFailure('Asset fixture must be a separate existing test directory.')
            entries=sorted(p for p in fixture.rglob('*') if p.is_file())
            if any(p.is_symlink() or not p.resolve().is_relative_to(fixture) for p in entries):raise RuntimeFailure('Asset fixtures cannot escape their directory.')
            save_json(out/'asset-fixture.json',{'root':str(fixture),'files':{str(p.relative_to(fixture)):hashlib.sha256(p.read_bytes()).hexdigest() for p in entries}})
            classpath=str(fixture)+os.pathsep+classpath
        arguments = ['-cp', classpath, 'com.bladecoder.engine.DesktopLauncher']
        jars = [Path(p) for p in match[1].split(os.pathsep) if Path(p).name.startswith('blade-engine') and p.endswith('.jar')]
        identity = {'engine_jars': [{'path': str(p), 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in jars]}
    # Player mode keeps the historical nonzero port requirement.
    port = 0
    if mode == 'player':
        import socket
        with socket.socket() as reservation:
            reservation.bind(('127.0.0.1', 0));port = reservation.getsockname()[1]
    profile = out/'profile';profile.mkdir()
    command = [java, '-Duser.home='+str(profile), *arguments, '-w', '-http', str(port), '-http-mode', mode,
               '-http-info', str(out/'connection.json')]
    if hidden:command += ['-automation-window']
    if overrides:
        override_path=Path(overrides).expanduser().resolve()
        if any(override_path.is_relative_to(root/folder) for folder in ('assets','raw','tools','skills','ai-toolkit','automation','docs')):
            raise RuntimeFailure('Persistent overrides must stay outside packaged/source/tool directories.')
        command += ['-content-overrides',str(override_path)]
    log = (out/'process.log').open('wb')
    environment = None
    if audio_sink:
        environment = dict(os.environ, PULSE_SINK=audio_sink, ALSOFT_DRIVERS='pulse')
        # Avoid the reproduced OpenAL/libsystemd first-load failure on this host.
        # Scope preloading to the owned audio-test child, preserving inherited entries.
        import ctypes.util
        pulse_library = ctypes.util.find_library('pulse')
        if pulse_library:
            inherited = environment.get('LD_PRELOAD','')
            environment['LD_PRELOAD'] = pulse_library + (':'+inherited if inherited else '')
    process = subprocess.Popen(command, cwd=out, env=environment, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    log.close()
    record = {'pid': process.pid, 'start_identity': process_identity(process.pid), 'mode': mode,
              'owned': True, 'hidden_gl_window':hidden, 'build': identity, 'profile': str(profile), 'command': command}
    save_json(out/'process.json', record)
    deadline = time.monotonic()+25
    try:
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeFailure('Game exited during startup; see process.log.')
            if (out/'connection.json').is_file():
                try:
                    client = RuntimeClient.connect(out/'connection.json')
                    if mode == 'debug':
                        save_json(out/'session.json', client.debug('/session'))
                except (RuntimeFailure, ValueError):
                    time.sleep(.1)
                    continue
                if scene and mode == 'debug':
                    try:
                        client.command('fixture.scene', {'chapter': chapter, 'scene': scene})
                        client.command('wait', {'conditions': [{'path': '/assetState', 'equals': 'LOADED'}]})
                    except Exception as failure:
                        detail = {'phase': 'scene_fixture', 'chapter': chapter, 'scene': scene, 'error': str(failure)}
                        try: detail['evidence'] = client.evidence(out, 'startup-failure')
                        except Exception as observation: detail['evidence_error'] = str(observation)
                        save_json(out / 'startup-error.json', detail)
                        raise RuntimeFailure('Scene fixture setup failed: ' + str(failure)) from failure
                return client, out
            time.sleep(.1)
        raise RuntimeFailure('Game startup watchdog expired; see process.log.')
    except Exception:
        stop_runtime(out)
        raise


def stop_runtime(directory):
    out = Path(directory).expanduser().resolve()
    record = json.loads((out/'process.json').read_text())
    pid = record['pid']
    actual = process_identity(pid)
    if actual is None:
        return {'status': 'already_stopped'}
    if not record.get('owned') or not record.get('start_identity') or record['start_identity'] != actual:
        raise RuntimeFailure('Process ownership cannot be verified; refusing to stop it.')
    os.killpg(pid, signal.SIGCONT)
    os.killpg(pid, signal.SIGTERM)
    deadline=time.monotonic()+3
    while time.monotonic()<deadline:
        try:
            waited,_=os.waitpid(pid,os.WNOHANG)
            if waited:return {'status':'stopped','pid':pid}
        except ChildProcessError:
            pass
        if process_identity(pid)!=actual:return {'status':'stopped','pid':pid}
        time.sleep(.05)
    if process_identity(pid)==actual:
        os.killpg(pid,signal.SIGKILL)
        try:os.waitpid(pid,0)
        except ChildProcessError:pass
    return {'status':'stopped','pid':pid,'forced':True}

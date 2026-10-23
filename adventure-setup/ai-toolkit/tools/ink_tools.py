"""Compile source Ink atomically and verify output with the game's Java runtime."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile


def local_path(value, root):
    path=Path(value).expanduser()
    return path if path.is_absolute() else root/path


def program(value, root):
    found=shutil.which(value) if '/' not in value else None
    path=Path(found) if found else local_path(value,root)
    return str(path) if path.is_file() else None


def java_program(config,root):
    default=str(Path(os.environ['JAVA_HOME'])/'bin/java') if os.environ.get('JAVA_HOME') else 'java'
    return program(config.get('java',default),root)


def compiler_environment(root, config):
    root = Path(root).resolve()
    props = dict(re.findall(r'^(\w+)=(.*)$', (root / 'gradle.properties').read_text(), re.M))
    version = props.get('bladeInkVersion')
    cache = local_path(config.get('gradle_cache', os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle')) + '/caches/modules-2/files-2.1'), root)
    runtime = sorted((cache / 'com.bladecoder.ink/blade-ink' / str(version)).glob('*/blade-ink-' + str(version) + '.jar'))
    compilers = sorted((cache / 'com.bladecoder.ink/blade-ink-compiler' / str(version)).glob('*/blade-ink-compiler-' + str(version) + '.jar'))
    return {'version': version, 'runtime': runtime, 'compilers': compilers, 'java': java_program(config, root)}


def compile_ink(root,config,source=None,output=None,check_only=False):
    root=Path(root).resolve()
    if source is None:
        from project_config import load_project
        selected=load_project(root).get('ink')
        if not selected:
            return [{'id':'ink.not-configured','status':'warning','message':'Ink is not configured for this project.'}]
        source=selected['source']
        if output is None:output=selected['output']
    source=local_path(source,root)
    output=local_path(output,root) if output else root/'assets/model'/f'{source.stem}.ink.json'
    checks=[]
    def emit(code,status,message,**details):
        checks.append({'id':'ink.'+code,'status':status,'message':message,**details})
    if source.suffix!='.ink' or not output.name.endswith('.ink.json'):
        emit('paths','error','Use a .ink source and a .ink.json output; editable sources are never compiler outputs.')
        return checks
    environment=compiler_environment(root,config)
    version=environment['version'];jars=environment['runtime']
    if not jars:
        emit('runtime','error',f'Matching blade-ink {version} runtime binary is unavailable in the configured Gradle cache.')
        return checks
    java=environment['java']
    compiler=program(config.get('inklecate','inklecate'),root)
    compiler_jars=environment['compilers']
    cached_compiler='inklecate' not in config and bool(compiler_jars)
    if not java or (not compiler and not cached_compiler):
        emit('compiler','error','Configure paths.java and paths.inklecate (or ADVENTURE_JAVA/ADVENTURE_INKLECATE); compiler and matching runtime are required.')
        return checks
    if not source.is_file() or not output.parent.is_dir():
        emit('paths','error','Ink source and output parent directory must exist.',path=str(source),output=str(output))
        return checks
    if source.resolve()==output.resolve():
        emit('paths','error','Compiler output cannot replace its editable Ink source.')
        return checks
    command=([java,'-cp',os.pathsep.join(map(str,[jars[0],compiler_jars[0]])),str(root/'tools/java/InkCompiler.java')]
             if cached_compiler else [java,'-jar',compiler] if compiler.endswith('.jar') else [compiler])
    emit('compiler','ok',f'Using cached blade-ink compiler {version}.' if cached_compiler else 'Using the explicitly configured or PATH Ink compiler; output ordering may differ.',
         compiler_sha256=hashlib.sha256((compiler_jars[0] if cached_compiler else Path(compiler)).read_bytes()).hexdigest())
    try:
        with tempfile.TemporaryDirectory(prefix='adventure-ink-') as temp:
            generated=Path(temp)/output.name
            arguments=[str(source),str(generated)] if cached_compiler else ['-o',str(generated),str(source)]
            result=subprocess.run([*command,*arguments],cwd=source.parent,
                                  stdin=subprocess.DEVNULL,capture_output=True,text=True,timeout=60)
            if result.returncode or not generated.is_file():
                emit('compile','error','Ink compilation failed; existing output was preserved.',diagnostics=result.stdout+result.stderr)
                return checks
            data=generated.read_bytes()
            compiled=json.loads(data.decode('utf-8-sig'))
            if not isinstance(compiled,dict) or type(compiled.get('inkVersion')) is not int or not isinstance(compiled.get('root'),list):
                raise ValueError('Compiler did not emit a supported Ink envelope')
            probe=subprocess.run([java,'-cp',str(jars[0]),str(root/'tools/java/InkProbe.java'),str(generated)],
                                 cwd=root,stdin=subprocess.DEVNULL,capture_output=True,text=True,timeout=30)
            if probe.returncode:
                emit('compatibility','error',f'Compiled Ink version {compiled["inkVersion"]} cannot load in blade-ink {version}; existing output was preserved.',diagnostics=probe.stdout+probe.stderr)
                return checks
            emit('compatibility','ok',f'Compiled Ink {compiled["inkVersion"]} loads with the actual blade-ink {version} runtime binary.',runtime_sha256=hashlib.sha256(jars[0].read_bytes()).hexdigest())
            if result.stdout.strip() or result.stderr.strip():
                emit('compiler-diagnostics','warning','Compiler emitted diagnostics.',diagnostics=result.stdout+result.stderr)
            old=output.read_bytes() if output.is_file() else None
            if check_only:
                matches=old is not None and json.loads(old.decode('utf-8-sig'))==compiled
                emit('freshness','ok' if matches else 'error','Packaged story matches current sources.' if matches else 'Packaged story is missing or stale; run ink-compile.',path=str(output))
            elif old!=data:
                temporary=None
                try:
                    with tempfile.NamedTemporaryFile(dir=output.parent,prefix='.ink-',delete=False) as handle:
                        temporary=Path(handle.name);handle.write(data)
                    temporary.chmod(output.stat().st_mode & 0o777 if output.exists() else 0o644)
                    temporary.replace(output)
                finally:
                    if temporary and temporary.exists():temporary.unlink()
                emit('compile','ok','Compiled and atomically replaced the packaged story.',path=str(output),sha256=hashlib.sha256(data).hexdigest())
            else:
                emit('compile','ok','Packaged output is already byte-identical; no files changed.',path=str(output))
    except (OSError,ValueError,subprocess.SubprocessError) as exc:
        emit('compile','error',f'Ink tooling failed ({type(exc).__name__}: {exc}); existing output was preserved until successful verification.')
    return checks


def test_ink(root,config,files=None):
    """Run explicit narrative scenarios against the actual declared runtime."""
    root=Path(root).resolve();checks=[]
    props=dict(re.findall(r'^(\w+)=(.*)$',(root/'gradle.properties').read_text(),re.M))
    cache=local_path(config.get('gradle_cache',os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle'))+'/caches/modules-2/files-2.1'),root)
    def jar(group,name,version):
        return next((cache/group/name/version).glob(f'*/{name}-{version}.jar'),None)
    runtime=jar('com.bladecoder.ink','blade-ink',props['bladeInkVersion'])
    gdx=local_path(config['gdx_jar'],root) if 'gdx_jar' in config else jar('com.badlogicgames.gdx','gdx',props['gdxVersion'])
    java=java_program(config,root)
    if not runtime or not gdx or not java:
        return [{'id':'ink.test-dependencies','status':'error','message':'Narrative tests need the declared blade-ink/gdx binary JARs in the configured cache and a JDK 11+.'}]
    paths=[local_path(f,root) for f in files] if files else sorted((root/'tools/ink-scenarios').glob('*.json'))
    if not paths:return [{'id':'ink.tests-missing','status':'error','message':'No narrative scenarios found.'}]
    for file in paths:
        try:
            data=json.loads(file.read_text(encoding='utf-8'))
            story=local_path(data['story'],root)
            result=subprocess.run([java,'-cp',os.pathsep.join(map(str,[runtime,gdx])),str(root/'tools/java/InkScenarioRunner.java'),str(story),str(file)],
                                  cwd=root,stdin=subprocess.DEVNULL,capture_output=True,text=True,timeout=40)
            checks.append({'id':'ink.narrative-test','status':'ok' if result.returncode==0 else 'error',
                           'path':str(file),'message':'Narrative scenarios passed (engine commands recorded, not executed).' if result.returncode==0 else 'Narrative scenario failed.',
                           'diagnostics':result.stdout+result.stderr})
        except (OSError,ValueError,TypeError,KeyError,subprocess.SubprocessError) as exc:
            checks.append({'id':'ink.narrative-test','status':'error','path':str(file),'message':f'Cannot run narrative scenarios: {exc}'})
    return checks

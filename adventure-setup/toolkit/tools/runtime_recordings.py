"""Read-only compatibility audit and deliberate migration of legacy engine recordings."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
from ink_tools import java_program
from runtime_client import RuntimeFailure


def assess_entries(root,source,entries):
    root=Path(root).resolve();source=Path(source).resolve();actors=set()
    for chapter in (root/'assets/model').glob('*.chapter.json'):
        data=json.loads(chapter.read_text())
        for scene in data.get('scenes',{}).values():actors.update(scene.get('actors',{}))
    stem=source.name.removesuffix('.verbs.rec')
    checkpoint=source.with_name(stem+'.gamestate.rec')
    referenced={entry['actor'] for entry in entries if entry['kind']=='verb' and entry.get('actor')}
    missing=sorted(referenced-actors)
    indices=[entry['index'] for entry in entries if entry['kind']=='choice']
    invalid_saves=[entry['index'] for entry in entries if entry['kind']=='save' and (not isinstance(entry.get('target'),str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,100}',entry['target']) or '..' in entry['target'])]
    reasons=[]
    if not checkpoint.is_file():reasons.append('No packaged entry checkpoint; personal save directories are not used.')
    if missing:reasons.append('Recorded actors are absent from the current model: '+', '.join(missing))
    if indices:reasons.append('Dialogue choices are indices without expected text/revisions and require explicit migration.')
    if invalid_saves:reasons.append('Legacy save names are outside the current safe-basename contract.')
    if any(entry['legacy_wait_seconds']>0 for entry in entries):reasons.append('Legacy delays do not define readiness/completion assertions.')
    return {'source':str(source.relative_to(root)),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
            'step_count':len(entries),'recorded_actor_ids':sorted(referenced),'missing_actor_ids':missing,'index_only_choice_steps':indices,
            'packaged_entry_checkpoint':str(checkpoint.relative_to(root)) if checkpoint.is_file() else None,'unsafe_save_steps':invalid_saves,
            'legacy_wait_seconds':sum(v['legacy_wait_seconds'] for v in entries),'compatibility':'requires_migration' if reasons else 'candidate_for_validation',
            'automatic_replay_approved':False,'reasons':reasons,'normalized_steps':entries,
            'scope':'Static compatibility assessment only. No recorded verbs, delays or personal saves are executed.'}


def recording_runtime(root,config):
    root=Path(root).resolve();java=java_program(config,root)
    properties=dict(line.split('=',1) for line in (root/'gradle.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
    version=properties['gdxVersion']
    if config.get('gdx_jar'):
        jar=Path(config['gdx_jar']).expanduser();jar=jar if jar.is_absolute() else root/jar
    else:
        cache=Path(config.get('gradle_cache',str(Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/modules-2/files-2.1'))).expanduser()
        if not cache.is_absolute():cache=root/cache
        jar=next((cache/'com.badlogicgames.gdx/gdx'/version).glob(f'*/gdx-{version}.jar'),None)
    if not java or not jar or not jar.is_file():raise RuntimeFailure('Declared GDX binary and JDK 11+ required.')
    return java,jar,version


def decode_recording(root,config,source):
    root=Path(root).resolve();source=Path(source).expanduser().resolve()
    if not source.is_file() or source.stat().st_size>4*1024*1024:raise RuntimeFailure('Missing/oversized recording.')
    java,jar,version=recording_runtime(root,config)
    parsed=subprocess.run([java,'--class-path',str(jar),str(root/'tools/java/RecordingInspector.java'),str(source)],capture_output=True,text=True,timeout=30)
    if parsed.returncode:raise RuntimeFailure('Actual LibGDX recording parser rejected '+source.name+': '+parsed.stderr[-6000:])
    return json.loads(parsed.stdout)


def audit_recordings(root,config):
    root=Path(root).resolve()
    try:java,jar,version=recording_runtime(root,config)
    except RuntimeFailure as exc:return [{'id':'runtime.recordings','status':'warning','message':'Legacy grammar audit unavailable: '+str(exc)}],{'status':'unavailable','recordings':[]}
    records=[];checks=[]
    for source in sorted((root/'assets/tests').glob('*.verbs.rec')):
        if not source.resolve().is_relative_to(root/'assets/tests'):raise RuntimeFailure('Packaged recording escapes its fixture directory.')
        try:entries=decode_recording(root,config,source)
        except RuntimeFailure as exc:
            checks.append({'id':'runtime.recording.parse','status':'error','message':str(exc)});continue
        assessment=assess_entries(root,source,entries);records.append(assessment)
        checks.append({'id':'runtime.recording.compatibility','status':'warning' if assessment['reasons'] else 'ok','message':source.name+': '+assessment['compatibility']+'; '+str(assessment['step_count'])+' legacy steps audited.'})
    if not records and not checks:checks.append({'id':'runtime.recordings','status':'ok','message':'No packaged legacy recordings found.'})
    return checks,{'schema_version':1,'status':'audited','recordings':records,'parser':'Actual declared LibGDX JsonReader, source-launch mode','gdx_version':version}


def migrate_recording(root,config,spec,output):
    from runtime_scenario import validate,validate_condition
    from runtime_client import save_json
    root=Path(root).resolve();out=Path(output).expanduser().resolve()
    if out.exists() or any(out.is_relative_to(root/f) for f in ('assets','raw','tools','skills')):raise RuntimeFailure('Migration output must be a new directory outside source/assets/tools.')
    if not isinstance(spec,dict) or set(spec)!={'schema_version','id','source','setup','selected'} or type(spec['schema_version']) is not int or spec['schema_version']!=1:
        raise RuntimeFailure('Migration needs schema_version 1, id, source, setup and explicit selected mappings.')
    if not isinstance(spec['source'],str):raise RuntimeFailure('Migration source must be a filename.')
    source=Path(spec['source']).expanduser();source=(source if source.is_absolute() else root/source).resolve()
    entries=decode_recording(root,config,source);selected=spec['selected']
    if not isinstance(selected,list) or not 1<=len(selected)<=50:raise RuntimeFailure('Select 1–50 mapped steps per scenario; split longer routes at declared checkpoints.')
    rows=[];previous=-1;provenance=[]
    for mapping in selected:
        if not isinstance(mapping,dict) or set(mapping)-{'index','id','before','after','choice_text','advance_text'} or not {'index','id','before','after'}<=set(mapping):raise RuntimeFailure('Each mapping needs index, id and explicit before/after conditions.')
        index=mapping['index'];name=mapping['id']
        if type(index) is not int or not previous<index<len(entries):raise RuntimeFailure('Selected legacy indices must be increasing and in range.')
        if not isinstance(name,str) or not re.fullmatch(r'[a-z0-9][a-z0-9-]{0,40}',name):raise RuntimeFailure('Invalid migration step id.')
        previous=index;entry=entries[index];validate_condition(mapping['before']);validate_condition(mapping['after'])
        advance=mapping.get('advance_text',False)
        if type(advance) is not bool:raise RuntimeFailure('advance_text must be explicit boolean.')
        rows.append({'id':name+'-ready','type':'wait','condition':mapping['before'],'advance_text':advance})
        if entry['kind']=='choice':
            if not isinstance(mapping.get('choice_text'),str) or not mapping['choice_text']:raise RuntimeFailure('Index-only dialogue needs explicit expected current choice text.')
            rows.append({'id':name,'type':'choose','text':mapping['choice_text']})
        else:
            if entry['kind']=='verb':
                if entry['actor']:
                    op='gameplay';args={'type':'actorVerb','actorId':entry['actor'],'verb':entry['verb']}
                    if entry.get('target'):args['target']=entry['target']
                elif entry['verb']=='goto':op='gameplay';args={'type':'sceneVerb','verb':'goto'}
                else:raise RuntimeFailure('Internal/unknown scene verbs need a deliberate supported mapping.')
            elif entry['kind']=='move':op='gameplay';args={'type':'goto','x':entry['x'],'y':entry['y']}
            elif entry['kind']=='save':
                target=entry.get('target')
                if not isinstance(target,str) or not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.-]{0,100}',target) or '..' in target:raise RuntimeFailure('Unsafe legacy save filename.')
                op='gameplay';args={'type':'saveGame','target':target}
            else:raise RuntimeFailure('Unsupported legacy step kind.')
            rows.append({'id':name,'type':'command','operation':op,'arguments':args})
        # Allow queued callbacks to run, then prove completion via the mapped predicate.
        rows.append({'id':name+'-deferred','type':'frames','count':4})
        rows.append({'id':name+'-completed','type':'wait','condition':mapping['after'],'advance_text':advance})
        rows.append({'id':name+'-assert','type':'assert','condition':mapping['after']})
        provenance.append({'legacy_index':index,'legacy_wait_seconds':entry['legacy_wait_seconds'],'kind':entry['kind'],'choice_text':mapping.get('choice_text')})
    scenario={'schema_version':1,'id':spec['id'],'description':'Explicit migration of selected recorded semantic actions; legacy delays/choice indices are not replayed.','setup':spec['setup'],'steps':rows}
    validate(scenario)
    out.mkdir(parents=True,exist_ok=False);save_json(out/'scenario.json',scenario)
    metadata={'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'source_steps':len(entries),'migrated':provenance,
              'scope':'Only selected mapped steps, with declared setup and readiness/assertions. This does not establish equivalence to the full legacy recording.'}
    save_json(out/'migration.json',metadata)
    return {'scenario':str(out/'scenario.json'),'migration':metadata,'validation':'Prepared only; execute with the shared client/engine runner.'}

"""Inspect the supported literal Ink-to-engine references, without inventing state."""
import re
from pathlib import Path
from chapter_validate import Validator
from model_json import load_model
from project_config import load_project
from art_sources import relative_path

EXTERNALS={'inInventory':1,'inInventory2':2,'getActorState':1,'getSceneState':1,
           'getPlayer':0,'getProperty':1,'getCurrentScene':0,'isDebug':0,'isVisible':1}


def ink_sources(root,entry):
    root=Path(root).resolve()
    relative_path(entry)
    base=(root/entry).parent.resolve()
    if not base.is_relative_to(root):
        return [],[(root/entry,0,'Ink source directory escapes the project.')]
    found=[];errors=[];active=set();visited=set()
    def visit(path):
        path=path.resolve()
        if not path.is_relative_to(base):
            errors.append((path,0,'INCLUDE escapes the entry source directory; external includes require an explicit supported workflow.'));return
        if path in active:
            errors.append((path,0,'Cyclic INCLUDE graph.'));return
        if path in visited:return
        active.add(path)
        try:text=path.read_text(encoding='utf-8-sig')
        except (OSError,UnicodeError) as exc:
            errors.append((path,0,f'Cannot read Ink source ({type(exc).__name__}).'));active.remove(path);return
        found.append((path,text));visited.add(path)
        for line,content in enumerate(text.splitlines(),1):
            m=re.match(r'^\s*INCLUDE\s+(.+?)(?:\s*//.*)?$',content)
            if m:
                # This project's compiler resolves includes relative to the entry source directory.
                target=(root/entry).parent/m[1].strip()
                if target.resolve() in active:errors.append((path,line,'Cyclic INCLUDE graph.'))
                else:visit(target)
        active.remove(path)
    visit(root/entry)
    return found,errors


def validate_ink_links(root,entry=None):
    root=Path(root).resolve();checks=[]
    if entry is None:
        selected=load_project(root).get('ink')
        if not selected:
            return [{'id':'ink.not-configured','status':'warning','message':'Ink is not configured for this project.'}]
        entry=selected['source']
    validator=Validator(root)
    def emit(code,status,file,line,message,**details):
        checks.append({'id':'ink.'+code,'status':status,'path':str(file.relative_to(root)) if file.is_relative_to(root) else str(file),
                       'line':line,'message':message,**details})
    sources,errors=ink_sources(root,entry)
    for file,line,message in errors:emit('include','error',file,line,message)
    models=[]
    try:
        for file in sorted((root/'assets/model').glob('*.chapter.json')):
            model,_=load_model(file);models.append(model)
        validator.world,_=load_model(root/'assets/model/world')
    except (OSError,ValueError) as exc:
        emit('links-model','error',root/'assets/model',0,f'Cannot inspect model definitions: {exc}');return checks
    validator.scenes={key:scene for model in models for key,scene in model.get('scenes',{}).items()}
    validator.model={'sounds':{key:sound for model in models for key,sound in model.get('sounds',{}).items()}}
    validator.scene_id=None
    command_count=0;query_count=0
    for file,text in sources:
        block=False
        for line,raw in enumerate(text.splitlines(),1):
            # Whole-line and trailing comments; source control strings are handled conservatively.
            content=raw.strip()
            if content.startswith('/*'):block=True
            if block:
                if '*/' in content:block=False
                continue
            if content.startswith('//'):continue
            content=re.split(r'\s+//',content,maxsplit=1)[0]
            decl=re.match(r'EXTERNAL\s+(\w+)\s*\(([^)]*)\)',content)
            if decl:
                arity=len([x for x in decl[2].split(',') if x.strip()])
                if decl[1] not in EXTERNALS:emit('external','error',file,line,f'External {decl[1]!r} has no binding in the reviewed engine contract.')
                elif arity!=EXTERNALS[decl[1]]:emit('external','error',file,line,f'External {decl[1]} expects {EXTERNALS[decl[1]]} parameters, found {arity}.')
            command=re.match(r'^>\s*([\w.]+)\s*(?::\s*(.*))?$',content)
            if command:
                command_count+=1;name,params=command.groups();params=params or ''
                if '{' in params or '{' in name:
                    emit('command-dynamic','warning',file,line,'Interpolated command cannot be resolved statically.',resolution='dynamic');continue
                resolved='Leave' if name=='LeaveNow' else name if name in validator.contract['actions'] else 'com.bladecoder.engine.actions.'+name+'Action' if '.' not in name else name
                if resolved not in validator.contract['actions']:
                    emit('command-class','warning' if '.' in name else 'error',file,line,f'Uncatalogued Ink command {name!r}; review actual classpath extensions.');continue
                action={'class':resolved}
                for part in params.split(',') if params else []:
                    pair=re.split('[:=]',part,maxsplit=1);key=pair[0].strip();value=pair[1].strip() if len(pair)>1 else None
                    if key in action:emit('command-parameter','warning',file,line,f'Repeated command parameter {key!r}; engine keeps the last value.')
                    action[key]=value
                before=len(validator.checks)
                validator.actions([action],'/command')
                for diagnostic in validator.checks[before:]:
                    if diagnostic['id']=='chapter.control':continue # Command block flow is not a chapter action array.
                    emit(diagnostic['id'].replace('chapter.','command-'),diagnostic['status'],file,line,diagnostic['message'],
                         **({'resolution':'dynamic'} if diagnostic.get('resolution') else {}))
            for match in re.finditer(r'\b(inInventory2?|getActorState|getSceneState|isVisible)\s*\(\s*"([^"{}]+)"',content):
                query_count+=1;function,value=match.groups();value=value.removeprefix('>')
                before=len(validator.checks)
                if function=='getSceneState':validator.scene_ref(value,'/query')
                else:validator.actor(value,'/query',qualified=function in ('getActorState','isVisible'))
                for diagnostic in validator.checks[before:]:
                    emit('query-ref',diagnostic['status'],file,line,diagnostic['message'],**({'resolution':'dynamic'} if diagnostic.get('resolution') else {}))
    checks.append({'id':'ink.links-coverage','status':'ok','message':f'Inspected {len(sources)} included sources, {command_count} literal commands and {query_count} literal engine queries. Dynamic interpolation, complex grammar, control flow and source/compiled freshness require separate checks.',
                   'sources':len(sources),'commands':command_count,'queries':query_count})
    return checks

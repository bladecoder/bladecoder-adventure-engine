"""Template-based Spine inspection, editable round trips and reviewed runtime exports."""
import copy
import json
import math
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

from art_sources import roots_for, within, relative_path
from asset_manifest import fields, require, json_file, shared_path
from art_pipeline import sha, fingerprints, create_output, export_preview, run, program, local_config
from atlas_tools import packer_classpath
from ink_tools import java_program


def plain(value):
    require(isinstance(value,str) and re.fullmatch(r'[A-Za-z][A-Za-z0-9_-]*',value), 'Use a plain Spine/runtime basename.')
    return value


def read_recipe(root, config, filename):
    path=(root/filename).resolve();data=json_file(path)
    fields(data,('schema_version','id','editor_version','runtime_family','source','settings','skeleton','output','required_animations'),('pieces_manifest','edits','workflow'))
    workflow=data.get('workflow','derive');require(workflow in ('derive','export'),'workflow must be derive or export.')
    if workflow=='derive':require(all(k in data for k in ('pieces_manifest','edits')) and 'editable' in data['output'],'Derivation requires pieces, edits and a distinct editable output.')
    require(data['schema_version']==1,'Unsupported Spine recipe schema.')
    plain(data['id']);plain(data['skeleton'])
    require(re.fullmatch(r'\d+\.\d+\.\d+',data['editor_version']) is not None,'Pin an exact Spine editor version.')
    require(data['editor_version'].rsplit('.',1)[0]==data['runtime_family'],'Editor and runtime major/minor must match.')
    fields(data['source'],('project','images'));fields(data['settings'],('json','binary','atlas'))
    fields(data['output'],('name','skeleton','atlas'),('editable',));plain(data['output']['name'])
    for p in [*data['source'].values(),*[data[k] for k in ('pieces_manifest','edits') if k in data],*data['output'].values()]:relative_path(p)
    for kind in data['settings']:shared_path(root,data['settings'][kind])
    json_settings=json_file(root/data['settings']['json']);binary_settings=json_file(root/data['settings']['binary']);atlas_settings=json_file(root/data['settings']['atlas'])
    require(json_settings.get('class')=='export-json' and json_settings.get('nonessential') is True and json_settings.get('cleanUp') is False and json_settings.get('format')=='JSON','Editable JSON must retain nonessential data without cleanup and use strict JSON.')
    require(binary_settings.get('class')=='export-binary' and binary_settings.get('extension')=='.skel' and binary_settings.get('packAtlas') is None,'Use a binary skeleton preset and explicit atlas packing.')
    require(atlas_settings.get('premultiplyAlpha') is False and atlas_settings.get('useIndexes') is False and atlas_settings.get('outputFormat')=='png','Current Spine renderer requires straight-alpha PNG and exact attachment names.')
    if workflow=='export':require(data['skeleton']==data['output']['name'],'Export output name must match the actual maintained skeleton name.')
    require(data['output']['skeleton']=='assets/spine/'+data['output']['name']+'.skel','Skeleton destination/name mismatch.')
    require(data['output']['atlas']=='assets/atlases/1/'+data['output']['name']+'.atlas','Atlas destination/name mismatch.')
    if workflow=='derive':require(data['output']['editable'].startswith('characters/') and data['output']['editable'].endswith('/'+data['output']['name']+'.spine'),'Editable project must be in a dedicated characters/ directory.')
    art,linked=roots_for(config,root);require(art is not None and art.is_dir(),'Configure an available art_root.')
    source=(art/data['source']['project']).resolve();images=(art/data['source']['images']).resolve()
    target=(art/data['output']['editable']).resolve() if workflow=='derive' else None
    require(within(source,[art]) and within(images,[art]) and source.is_file() and images.is_dir(),'Missing or escaping editable Spine source/images.')
    if target:require(within(target,[art]) and not within(target,[source.parent]) and not within(source,[target.parent]),'Derived project must not overwrite the template tree.')
    require(isinstance(data['required_animations'],list) and all(isinstance(s,str) and s for s in data['required_animations']),'Declare required template animations.')
    return data,path,art,source,images


def launcher(root,config):
    if 'spine' in config:
        value=config['spine'];found=shutil.which(value) if '/' not in value else str((root/Path(value).expanduser()).resolve())
    else:found=shutil.which('Spine') or shutil.which('spine') or str(Path.home()/'apps/Spine/Spine.sh')
    require(found and Path(found).is_file() and os.access(found,os.X_OK),'Configure paths.spine or ADVENTURE_SPINE to the installed launcher.')
    return found


def cli(root,config,version,args,log):
    command=[launcher(root,config),'--hide-license','-u',version,*map(str,args)]
    result=subprocess.run(command,stdin=subprocess.DEVNULL,capture_output=True,text=True,timeout=120)
    output=result.stdout+result.stderr
    # Keep names/activation details out of shared reports, even with older launchers.
    output=re.sub(r'(?im)^Licensed to:.*$','Licensed to: <hidden>',output)
    Path(log).write_text(output)
    if result.returncode:raise ValueError('Spine command failed; see '+str(log)+': '+output[-1500:])
    require('Spine '+version in output,'Launcher did not confirm the pinned editor version.')
    return output


def attachments(data):
    result=[]
    for skin in data.get('skins',[]):
        for slot,items in skin.get('attachments',{}).items():
            for name,item in items.items():result.append((skin['name'],slot,name,item))
    return result


def image_names(data):
    return {a.get('path',name) for _,_,name,a in attachments(data) if a.get('type','region') in ('region','mesh')}


def validate_data(data,images=None,required=()):
    require(isinstance(data,dict) and all(k in data for k in ('skeleton','bones','slots','skins','animations')),'Expected Spine JSON with nonessential data.')
    def finite_numbers(value):
        if isinstance(value,float):require(math.isfinite(value),'Spine numbers must be finite.')
        elif isinstance(value,dict):
            for child in value.values():finite_numbers(child)
        elif isinstance(value,list):
            for child in value:finite_numbers(child)
    finite_numbers(data)
    require(not any(data.get(k) for k in ('ik','transform','path','physics','constraints')),'Constraint rigs need an explicit adapter extension.')
    bones=[b['name'] for b in data['bones']];slots=[s['name'] for s in data['slots']]
    require(len(set(bones))==len(bones) and bones,'Duplicate or missing bones.');require(len(set(slots))==len(slots),'Duplicate slots.')
    seen=set()
    for bone in data['bones']:
        require('parent' not in bone or bone['parent'] in seen,'Bone parent must exist before child: '+bone['name']);seen.add(bone['name'])
    require(all(s['bone'] in seen for s in data['slots']),'Slot references missing bone.')
    skins=data['skins'];require(isinstance(skins,list) and len({s['name'] for s in skins})==len(skins),'Expected unique Spine 4.x skins.')
    available={s:set() for s in slots}
    for skin,slot,name,item in attachments(data):
        require(slot in available,'Attachment references missing slot: '+slot);available[slot].add(name)
        require(item.get('type','region')=='region','This cutout adapter supports region attachments; mesh/constraint workflows need an explicit extension.')
        path=item.get('path',name);relative_path(path)
        require(not path.endswith('.png'),'Attachment paths omit .png.')
        require(all(type(item.get(k,0)) in (float,int) and math.isfinite(item.get(k,0)) for k in ('x','y','rotation','scaleX','scaleY','width','height')),'Nonfinite attachment geometry.')
        require(item.get('width',0)>0 and item.get('height',0)>0,'Attachment dimensions must be positive.')
        if images is not None:
            file=(Path(images)/(path+'.png')).resolve()
            require(within(file,[Path(images).resolve()]) and file.is_file(),'Missing attachment image: '+path)
    for slot in data['slots']:require(slot.get('attachment') is None or slot['attachment'] in available[slot['name']],'Setup attachment missing from skins.')
    require(not set(required)-data['animations'].keys(),'Required animations are missing.')
    def timeline(value):
        if isinstance(value,dict):
            for child in value.values():timeline(child)
        elif isinstance(value,list):
            if value and all(isinstance(v,dict) for v in value):
                times=[v.get('time',0) for v in value]
                require(all(type(t) in (float,int) and math.isfinite(t) and t>=0 for t in times) and times==sorted(times),'Timeline times must be finite, nonnegative and ordered.')
            for child in value:timeline(child)
    for name,animation in data['animations'].items():
        require(not set(animation.get('bones',{}))-seen,'Animation references missing bone: '+name)
        require(not set(animation.get('slots',{}))-set(slots),'Animation references missing slot: '+name)
        for slot,parts in animation.get('slots',{}).items():
            for key in parts.get('attachment',[]):require(key.get('name') is None or key['name'] in available[slot],'Timeline attachment is missing: '+slot)
        for frame in animation.get('drawOrder',[]):
            require(all(o['slot'] in slots for o in frame.get('offsets',[])),'Draw order references missing slot.')
        timeline(animation)
    return {'version':data['skeleton']['spine'],'bones':data['bones'],'slots':data['slots'],
            'skins':[s['name'] for s in skins],'images':sorted(image_names(data)),
            'animations':list(data['animations']),'attachments':len(attachments(data))}


def adapt_data(data,edits):
    fields(edits,('schema_version','bone_offsets','attachment_paths','animations','replace_animations'))
    require(edits['schema_version']==1,'Unsupported adaptation version.')
    result=copy.deepcopy(data);bones={b['name']:b for b in result['bones']}
    require(not set(edits['bone_offsets'])-bones.keys(),'Cannot offset an unknown bone.')
    for name,values in edits['bone_offsets'].items():
        fields(values,(),('x','y','rotation'))
        for k,v in values.items():
            require(type(v) in (int,float) and math.isfinite(v),'Bone offsets must be finite numbers.');bones[name][k]=bones[name].get(k,0)+v
    applied=set()
    for skin,slot,name,item in attachments(result):
        key=skin+'/'+slot+'/'+name
        if key in edits['attachment_paths']:
            relative_path(edits['attachment_paths'][key]);item['path']=edits['attachment_paths'][key];applied.add(key)
    require(applied==set(edits['attachment_paths']),'Unknown attachment path replacement.')
    require(set(edits['replace_animations']) <= edits['animations'].keys() & result['animations'].keys(),'Replacement animations must name existing timelines and provide their edits.')
    require(not (edits['animations'].keys() & result['animations'].keys())-set(edits['replace_animations']),'Existing animation replacement must be explicit.')
    result['animations'].update(copy.deepcopy(edits['animations']));validate_data(result)
    return result


def prepare(root,config,filename,out):
    recipe,path,art,source,images=read_recipe(root,config,filename)
    captured=fingerprints([path,source,*images.glob('*.png'),*[root/recipe[k] for k in ('edits',) if k in recipe],*[root/p for p in recipe['settings'].values()]])
    project_info=cli(root,config,recipe['editor_version'],['-i',source],out/'project-info.log')
    baseline=out/'baseline';baseline.mkdir()
    cli(root,config,recipe['editor_version'],['-i',source,'-o',baseline,'-e',root/recipe['settings']['json']],out/'source-export.log')
    file=baseline/(recipe['skeleton']+'.json')
    require(sorted(baseline.glob('*.json'))==[file],'This recipe must identify the only skeleton in the project; split multi-skeleton workflows explicitly.')
    data=json_file(file)
    summary=validate_data(data,images,recipe['required_animations'])
    version=re.search(r'Spine version: ([0-9.]+)',project_info);summary['project_version']=version.group(1) if version else None
    require(summary['version'].rsplit('.',1)[0]==recipe['runtime_family'],'Exported data/runtime family mismatch.')
    return recipe,art,images,data,summary,captured


def inspect(root,config,filename,output):
    out=create_output(output,root)
    recipe,art,images,data,summary,captured=prepare(root,config,filename,out)
    require(fingerprints(captured)==captured,'Source changed during inspection.')
    result={'schema_version':1,'kind':'spine-inspection','recipe':recipe['id'],'template':summary,'inputs':captured}
    (out/'report.json').write_text(json.dumps(result,indent=2)+'\n');return result


def canonical_animation_defaults(animations):
    """Spine may omit explicit default frame values when importing valid new timelines."""
    result = copy.deepcopy(animations)
    def times(value):
        if isinstance(value, dict):
            if value.get('time') == 0: value.pop('time')
            for child in value.values(): times(child)
        elif isinstance(value, list):
            for child in value: times(child)
    times(result)
    defaults = {'rotate': {'value': 0}, 'translate': {'x': 0, 'y': 0},
                'translatex': {'value': 0}, 'translatey': {'value': 0},
                'scale': {'x': 1, 'y': 1}, 'scalex': {'value': 1}, 'scaley': {'value': 1},
                'shear': {'x': 0, 'y': 0}, 'shearx': {'value': 0}, 'sheary': {'value': 0}}
    for animation in result.values():
        for timelines in animation.get('bones', {}).values():
            for kind, frames in timelines.items():
                for frame in frames if kind in defaults else []:
                    for key, default in defaults[kind].items():
                        if frame.get(key) == default: frame.pop(key)
    return result


def preview(root,config,filename,output):
    if json_file((root/filename).resolve()).get('workflow')=='export':return export_existing(root,config,filename,output)
    out=create_output(output,root)
    recipe,art,images,data,summary,captured=prepare(root,config,filename,out)
    pieces=out/'pieces';checks=export_preview(root,config,[recipe['pieces_manifest']],pieces)
    require(not any(c['status']=='error' for c in checks),str(checks))
    piece_report=json_file(pieces/'report.json');captured.update(piece_report['inputs'])
    editable=out/'editable';(editable/'images').mkdir(parents=True)
    files=[]
    for row in piece_report['files']:
        image=pieces/row['preview']
        require(row['image']['alpha_max']>0,'An exported character piece is entirely transparent: '+image.name)
        require(not (editable/'images'/image.name).exists(),'Duplicate character image basename: '+image.name)
        shutil.copyfile(image,editable/'images'/image.name)
    available={p.stem for p in (editable/'images').glob('*.png')}
    edited=adapt_data(data,json_file(root/recipe['edits']));edited['skeleton']['images']='./images/'
    validate_data(edited,editable/'images',recipe['required_animations'])
    require(image_names(edited)==available,'Recipe must export exactly the used attachment images; references/guides stay out.')
    name=recipe['output']['name'];project=editable/(name+'.spine');json_path=editable/(name+'.json')
    json_path.write_text(json.dumps(edited,indent=2)+'\n')
    cli(root,config,recipe['editor_version'],['-i',json_path,'-o',project,'-r',name],out/'import.log')
    roundtrip=out/'roundtrip';roundtrip.mkdir()
    cli(root,config,recipe['editor_version'],['-i',project,'-o',roundtrip,'-e',root/recipe['settings']['json']],out/'roundtrip.log')
    imported=json_file(roundtrip/(name+'.json'));validate_data(imported,editable/'images',recipe['required_animations'])
    for key in ('bones','slots','skins'):
        require(edited[key]==imported[key],'Round trip changed '+key+'; review before producing runtime exports.')
    require(canonical_animation_defaults(edited['animations'])==canonical_animation_defaults(imported['animations']),'Round trip changed animation semantics; review before producing runtime exports.')
    runtime=out/'runtime';(runtime/'spine').mkdir(parents=True);(runtime/'atlases').mkdir()
    cli(root,config,recipe['editor_version'],['-i',project,'-o',runtime/'spine','-e',root/recipe['settings']['binary']],out/'binary-export.log')
    # Pack only explicit used images, with no inherited historical pack.json.
    cli(root,config,recipe['editor_version'],['-i',editable/'images','-o',runtime/'atlases','-n',name,'-p',root/recipe['settings']['atlas']],out/'atlas-pack.log')
    jars,lock=packer_classpath(root,config);verifier=root/'tools/java/AtlasVerifier.java'
    captured.update(fingerprints([*jars,lock,verifier]))
    metadata=run([java_program(config,root),'-cp',os.pathsep.join(map(str,jars)),verifier,runtime/'atlases'/(name+'.atlas')])
    atlas=json.loads(metadata.split('ATLAS_DATA=',1)[1]);require({r['name'] for r in atlas['regions']}==available,'Atlas regions differ from attachment images.')
    require(all(r['index']==-1 for r in atlas['regions']),'Spine attachment names must not collapse to frame indices.')
    for region in atlas['regions']:
        image=next(r for r in piece_report['files'] if Path(r['output']).stem==region['name'])['image']
        require((region['originalWidth'],region['originalHeight'])==(image['width'],image['height']),'Atlas original canvas changed.')
    def add(candidate,kind,destination):
        target=(root if kind=='repo' else art)/destination
        files.append({'preview':candidate.relative_to(out).as_posix(),'root':kind,'output':destination,'sha256':sha(candidate),'baseline':sha(target) if target.exists() else None})
    add(runtime/'spine'/(name+'.skel'),'repo',recipe['output']['skeleton'])
    for candidate in [runtime/'atlases'/(name+'.atlas'),*[runtime/'atlases'/p for p in atlas['pages']]]:
        add(candidate,'repo',str(Path(recipe['output']['atlas']).parent/candidate.name))
    external=Path(recipe['output']['editable']).parent
    for candidate in [project,json_path,*sorted((editable/'images').glob('*.png'))]:
        add(candidate,'art',str(external/candidate.relative_to(editable)))
    for row in piece_report['files']:add(pieces/row['preview'],'repo',row['output'])
    magick=program(local_config(config,root),'magick')
    run([magick,'montage',*sorted((editable/'images').glob('*.png')),'-background','#aaaaaa','-geometry','160x180+5+5','-tile','6x',out/'pieces-contact.png'])
    require(fingerprints(captured)==captured,'Inputs changed during preview.')
    result={'schema_version':1,'kind':'spine-preview','recipe':str((root/filename).resolve()),'art_root':str(art),'inputs':captured,'files':files,'template':summary,'animations':list(edited['animations']),'atlas':atlas,'roundtrip':'bones, slots and skins exactly preserved; timelines preserved after known Spine default normalization','runtime_family':recipe['runtime_family'],'editor_version':recipe['editor_version']}
    (out/'report.json').write_text(json.dumps(result,indent=2)+'\n');return result


def export_existing(root,config,filename,output):
    out=create_output(output,root)
    recipe,art,images,data,summary,captured=prepare(root,config,filename,out)
    require(recipe.get('workflow')=='export','Use an export workflow recipe for a maintained project.')
    name=recipe['output']['name'];source=art/recipe['source']['project']
    runtime=out/'runtime';(runtime/'spine').mkdir(parents=True);(runtime/'atlases').mkdir()
    cli(root,config,recipe['editor_version'],['-i',source,'-o',runtime/'spine','-e',root/recipe['settings']['binary']],out/'binary-export.log')
    # Copy exactly the referenced images so no guide/legacy settings leak into packing.
    with tempfile.TemporaryDirectory(prefix='adventure-spine-images-') as temp:
        staging=Path(temp)
        for image in image_names(data):
            target=staging/(image+'.png');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(images/(image+'.png'),target)
        cli(root,config,recipe['editor_version'],['-i',staging,'-o',runtime/'atlases','-n',name,'-p',root/recipe['settings']['atlas']],out/'atlas-pack.log')
    jars,lock=packer_classpath(root,config);verifier=root/'tools/java/AtlasVerifier.java';captured.update(fingerprints([*jars,lock,verifier]))
    metadata=run([java_program(config,root),'-cp',os.pathsep.join(map(str,jars)),verifier,runtime/'atlases'/(name+'.atlas')])
    atlas=json.loads(metadata.split('ATLAS_DATA=',1)[1]);require({r['name'] for r in atlas['regions']}==image_names(data),'Packed regions differ from maintained project images.')
    require(all(r['index']==-1 for r in atlas['regions']),'Attachment names must not become frame indices.')
    files=[]
    for candidate,destination in [(runtime/'spine'/(name+'.skel'),recipe['output']['skeleton']),
            (runtime/'atlases'/(name+'.atlas'),recipe['output']['atlas']),
            *[(runtime/'atlases'/p,str(Path(recipe['output']['atlas']).parent/p)) for p in atlas['pages']]]:
        target=root/destination;files.append({'preview':candidate.relative_to(out).as_posix(),'root':'repo','output':destination,'sha256':sha(candidate),'baseline':sha(target) if target.exists() else None})
    require(fingerprints(captured)==captured,'Maintained sources changed during export.')
    result={'schema_version':1,'kind':'spine-preview','workflow':'export','recipe':str((root/filename).resolve()),'art_root':str(art),'inputs':captured,'files':files,'template':summary,'animations':list(data['animations']),'atlas':atlas,'runtime_family':recipe['runtime_family'],'editor_version':recipe['editor_version']}
    (out/'report.json').write_text(json.dumps(result,indent=2)+'\n');return result


def apply(root,config,filename):
    path=Path(filename).expanduser().resolve();report=json_file(path)
    require(report.get('schema_version')==1 and report.get('kind')=='spine-preview','Expected a Spine preview report.')
    recipe,_,art,source,images=read_recipe(root,config,report['recipe'])
    require(str(art)==report['art_root'],'Art root differs from reviewed preview; regenerate it.')
    require(fingerprints(report['inputs'])==report['inputs'],'Preview input changed; regenerate it.')
    pairs=[];targets=set();external=(art/recipe['output']['editable']).parent.resolve() if recipe.get('workflow','derive')=='derive' else None
    for row in report['files']:
        relative_path(row['output']);candidate=(path.parent/row['preview']).resolve()
        require(within(candidate,[path.parent]) and candidate.is_file(),'Invalid candidate path.')
        require(row['root'] in ('repo','art'),'Unknown output root.')
        target=((root if row['root']=='repo' else art)/row['output']).resolve()
        require(row['root']!='art' or external is not None,'An export-only recipe cannot replace editable sources.')
        allowed=[root/'assets/spine',root/'assets/atlases/1',root/'raw/images'/('spine-'+recipe['id'])] if row['root']=='repo' else [external]
        require(within(target,[p.resolve() for p in allowed]) and not within(target,[source.parent]) and str(target) not in report['inputs'],'Destination escapes dedicated output roots or overwrites input.')
        require(target not in targets and target.suffix in ('.skel','.atlas','.png','.json','.spine'),'Duplicate or unsupported output.')
        require(sha(candidate)==row['sha256'] and (sha(target) if target.exists() else None)==row['baseline'],'Candidate/destination changed since preview.')
        targets.add(target);pairs.append((candidate,target))
    # Validate the entire batch before writes, and restore originals on IO failure.
    with tempfile.TemporaryDirectory(prefix='adventure-spine-apply-') as temp:
        backup=Path(temp);applied=[]
        try:
            for i,(candidate,target) in enumerate(pairs):
                existed=target.exists()
                if existed:shutil.copyfile(target,backup/str(i))
                target.parent.mkdir(parents=True,exist_ok=True);applied.append((i,target,existed))
                with tempfile.NamedTemporaryFile(dir=target.parent,delete=False) as f:staging=Path(f.name)
                try:shutil.copyfile(candidate,staging);staging.replace(target)
                finally:staging.unlink(missing_ok=True)
        except OSError:
            for i,target,existed in reversed(applied):
                if existed:shutil.copyfile(backup/str(i),target)
                else:target.unlink(missing_ok=True)
            raise
    return {'applied':len(pairs),'template_preserved':sha(source)==report['inputs'][str(source)]}


def execute(root,config,args):
    try:
        if args.action=='apply':result=apply(root,config,args.recipe)
        elif args.action=='test':
            from spine_runtime import test
            return test(root,config,args.recipe,args.out)
        else:
            require(args.out,'Use --out NEW_DIRECTORY.')
            result=(inspect if args.action=='inspect' else export_existing if args.action=='export' else preview)(root,config,args.recipe,args.out)
        return [{'id':'spine.'+args.action,'status':'ok','message':'Spine '+args.action+' completed.','path':str(args.out or args.recipe)}],result
    except (ValueError,OSError,KeyError,TypeError,subprocess.SubprocessError) as exc:
        return [{'id':'spine.'+args.action,'status':'error','message':str(exc)}],{}

"""Read-only Blade Engine MODEL validation. No engine, art tree or GUI required."""
import json
import math
from pathlib import Path
import re
from model_json import load_model, ModelSyntaxError, pointer

CONTRACT = Path(__file__).resolve().parent / 'model-contract'
ACTORS = {'AnchorActor','ObstacleActor','WalkZoneActor','InteractiveActor','SpriteActor','CharacterActor'}
RENDERERS = {'AtlasRenderer','ImageRenderer','TextRenderer','ParticleRenderer','com.bladecoder.engine.spine.SpineRenderer'}
STATE_FIELDS = {'currentScene','inventories','currentInventory','timeOfGame','cutmode','customProperties','chapter','musicEngine','uiActors','dirtyProps','tweens','playingSound','playerInside','currentAnimation','flipX','currentFrameIndex','faTween','cb','ip','currentTarget','story','verbRunners'}
# Null dereferences/invalid lookup arguments verified in these run() methods.
# Editor required=true alone is never sufficient evidence of a runtime error.
RUNTIME_REQUIRED = {
    'AnimationAction':{'animation'}, 'LeaveAction':{'scene'},
    'PickUpAction':{'actor'}, 'MoveToSceneAction':{'actor'},
    'SetActorAttrAction':{'actor'}, 'SetStateAction':{'actor'},
    'PositionAction':{'actor'}, 'RandomPositionAction':{'actor','minPosition','maxPosition'},
    'IfAttrAction':{'actor','attr'}, 'IfSceneAttrAction':{'attr'},
}
NULLABLE_HINTS = {('SayAction','actor'),('TextAction','style'),('PositionAnimAction','mode')}


def finite(value):
    try:
        return not isinstance(value, bool) and math.isfinite(float(value))
    except (TypeError, ValueError, OverflowError):
        return False


class Validator:
    def __init__(self, root):
        self.root = Path(root).resolve()
        self.schema = json.loads((CONTRACT/'chapter.schema.json').read_text())['$defs']
        self.contract = json.loads((CONTRACT/'actions.json').read_text())
        self.checks, self.lines, self.file = [], {}, ''
        try:
            from model_contract import extend_contract
            extend_contract(self.root, self.contract)
        except (OSError, ValueError) as error:
            self.emit('contract', 'error', '', 'Cannot load project action contract: ' + str(error))
        self.scenes, self.world, self.model = {}, {}, {}
        self.scene_id = None
        self.atlases = {}
        self.ink = None
        self.ink_paths = set()
        self.story_changes = False
        self.inventory_ids = {'DEFAULT'}
        self.stats = {'chapters':0,'scenes':0,'actors':0,'actions':0}

    def emit(self, code, status, path, message, **details):
        self.checks.append({'id':'chapter.'+code,'status':status,'message':message,
                            'path':self.file,'json_path':path or '', 'line':self.lines.get(path),**details})

    def structure(self, value, name, path=''):
        self.shape(value, self.schema[name], path)

    def shape(self, value, spec, path):
        if '$ref' in spec:
            name = spec['$ref'].rsplit('/',1)[1]
            if name in ('actor','renderer') and isinstance(value,dict):
                cls = value.get('class')
                known = ACTORS if name=='actor' else RENDERERS
                if isinstance(cls,str) and cls in known:
                    name = cls.rsplit('.',1)[-1]
                else:
                    status = 'warning' if isinstance(cls,str) and '.' in cls else 'error'
                    self.emit('class',status,pointer(path,'class'),f'Unsupported {name} class {cls!r}; extension internals are not validated.')
            return self.structure(value,name,path)
        if 'anyOf' in spec:
            options = spec['anyOf']
            if value is None and any(s.get('type')=='null' for s in options):
                return
            if spec.get('type')=='array':
                if isinstance(value,list) and 0<len(value)<6:
                    self.emit('geometry','error',path,'A nonempty polygon needs at least three vertices.')
            else:
                return self.shape(value,options[0],path)
        if 'const' in spec and value!=spec['const']:
            self.emit('class','error',path,f'Expected class {spec["const"]!r}.')
        expected = spec.get('type')
        types = expected if isinstance(expected,list) else [expected]
        valid = {'object':isinstance(value,dict),'array':isinstance(value,list),
                 'string':isinstance(value,str),'null':value is None,'boolean':type(value) is bool,
                 'number':isinstance(value,(float,int)) and finite(value),
                 'integer':type(value) is int and finite(value)}
        if expected and not any(valid.get(t,True) for t in types):
            self.emit('type','error',path,f'Expected {expected}, found {type(value).__name__}.')
            return
        if 'enum' in spec and value not in spec['enum']:
            self.emit('enum','error',path,f'Value {value!r} is outside {spec["enum"]}.')
        if isinstance(value,dict):
            for key in spec.get('required',[]):
                if key not in value:
                    self.emit('required','error',pointer(path,key),f'Missing required model field {key}.')
            props = spec.get('properties',{})
            extra = spec.get('additionalProperties',True)
            for key,v in value.items():
                p = pointer(path,key)
                if key in props:
                    self.shape(v,props[key],p)
                elif isinstance(extra,dict):
                    self.shape(v,extra,p)
                elif extra is False:
                    label = 'Saved-game field is not MODEL data' if key in STATE_FIELDS else 'Unknown field for the supported model type'
                    self.emit('field','error',p,f'{label}: {key}.')
        elif isinstance(value,list):
            if len(value)<spec.get('minItems',0):
                self.emit('geometry','error',path,f'At least {spec["minItems"]} values required.')
            for i,v in enumerate(value):
                self.shape(v,spec.get('items',{}),pointer(path,i))

    def read(self,path):
        self.file = str(path.relative_to(self.root)) if path.is_relative_to(self.root) else str(path)
        self.lines = {}
        try:
            value,self.lines=load_model(path)
            if not isinstance(value,dict):
                raise ModelSyntaxError('Model root must be an object')
            return value
        except (OSError,UnicodeError,ValueError,RecursionError) as exc:
            self.emit('syntax','error','',str(exc))
            return None

    def dynamic(self,value,path):
        if isinstance(value,str) and value.startswith('$'):
            self.emit('dynamic','warning',path,f'Runtime reference {value!r} cannot be resolved statically.',resolution='dynamic')
            return True
        return False

    def actor(self,value,path,scene=None,qualified=False):
        if not value or not isinstance(value,str) or self.dynamic(value,path):
            return None
        actor_id=value
        scene_id=scene if scene is not None else self.scene_id
        if qualified:
            sep='#' if '#' in value else '.' if '.' in value else None
            if sep:
                scene_id,actor_id=value.split(sep,1)
                if scene_id and scene_id not in self.scenes:
                    self.emit('scene-ref','error',path,f'Unknown scene {scene_id!r} in actor reference {value!r}.')
                    return None
        if actor_id != value and self.dynamic(actor_id,path):
            return None
        target=self.scenes.get(scene_id,{})
        actors=target.get('actors',{})
        if isinstance(actors,dict) and actor_id in actors:
            return actors[actor_id]
        candidates=[s['actors'][actor_id] for s in self.scenes.values() if isinstance(s,dict) and isinstance(s.get('actors'),dict) and actor_id in s['actors']]
        if candidates or scene_id is None:
            self.emit('runtime-actor','warning',path,f'Actor {actor_id!r} is not initially in this scene; inventory, UI, movement or global-verb context may resolve it.',resolution='dynamic')
            return candidates[0] if len(candidates)==1 else None
        self.emit('actor-ref','error',path,f'Unknown actor {actor_id!r} in chapter and initial scene {scene_id!r}.')
        return None

    def scene_ref(self,value,path):
        if isinstance(value,str) and value and not self.dynamic(value,path) and value not in self.scenes:
            self.emit('scene-ref','error',path,f'Unknown scene {value!r}.')

    def resource(self,folder,name,path,extension=''):
        if not isinstance(name,str) or not name or self.dynamic(name,path):
            return []
        candidate=Path(name+extension)
        if candidate.is_absolute() or '..' in candidate.parts or '\\' in name:
            self.emit('resource-path','error',path,f'Unsafe runtime resource path {name!r}.')
            return []
        base=self.root/'assets'/folder
        direct=base/candidate
        # Resolution resources are sibling scale directories selected at runtime.
        paths=([direct] if direct.is_file() else []) + [d/candidate for d in sorted(base.glob('*')) if d.is_dir() and (d/candidate).is_file()]
        if not paths:
            self.emit('resource','error',path,f'Missing packaged {folder} resource {name+extension!r}.')
        for found in paths:
            if not found.resolve().is_relative_to((self.root/'assets').resolve()):
                self.emit('resource-path','error',path,f'Resource escapes packaged assets: {found}.')
        return paths

    def atlas(self,name,path,region=None):
        files=self.resource('atlases',name,path,'.atlas')
        for file in files:
            if file not in self.atlases:
                regions=set(); pages=[]; expect_page=True
                try:lines=file.read_text(encoding='utf-8-sig').splitlines()
                except (OSError,UnicodeError) as exc:
                    self.emit('atlas-read','error',path,f'Cannot read atlas {file}: {type(exc).__name__}.')
                    continue
                for raw in lines:
                    line=raw.strip()
                    if not line:
                        expect_page=True
                    elif ':' not in line:
                        if expect_page:
                            pages.append(line);expect_page=False
                        else:
                            regions.add(line)
                self.atlases[file]=(regions,pages)
            regions,pages=self.atlases[file]
            if not pages:self.emit('atlas-format','error',path,f'Atlas {file.name} has no readable texture pages.')
            for page in pages:
                image=file.parent/page
                if not image.is_file() or not image.resolve().is_relative_to((self.root/'assets').resolve()):
                    self.emit('atlas-page','error',path,f'Missing or unsafe atlas page {page!r} in {file.name}.')
            if region and region not in regions:
                self.emit('atlas-region','error',path,f'Atlas {file.relative_to(self.root)} has no region {region!r}.')

    def animation(self,actor,name,path):
        if not isinstance(actor,dict) or not isinstance(name,str) or not name:
            return
        renderer=actor.get('renderer')
        if not isinstance(renderer,dict):
            self.emit('renderer-combination','error',path,'Animation target has no renderer.')
            return
        fanims=renderer.get('fanims',{})
        if name in fanims:
            return
        flip=lambda x:x[:-4]+'right' if x.endswith('left') else x[:-5]+'left' if x.endswith('right') else x
        if flip(name) in fanims:
            return
        prefix=name.rsplit('.',1)[0]+'.'
        if any(k.startswith(prefix) for k in fanims):
            self.emit('animation-direction','warning',path,f'Animation {name!r} needs directional fallback; runtime direction remains unverified.')
        else:
            self.emit('animation-ref','error',path,f'Unknown declared animation {name!r} for actor {actor.get("id")!r}.')

    def renderer(self,actor,path):
        r=actor.get('renderer')
        if not isinstance(r,dict):return
        cls=r.get('class')
        if cls not in RENDERERS:return
        if cls=='TextRenderer':
            self.resource('ui',r.get('fontName'),pointer(path,'fontName'),'.ttf')
        elif cls=='ParticleRenderer':
            self.resource('particles',r.get('particleName'),pointer(path,'particleName'))
            if r.get('atlasName'):self.atlas(r['atlasName'],pointer(path,'atlasName'))
            self.emit('particle-coverage','warning',path,'Particle definition exists; emitter textures and settings are not interpreted.')
        else:
            fanims=r.get('fanims',{})
            if not isinstance(fanims,dict):return
            for key,a in fanims.items():
                p=pointer(pointer(path,'fanims'),key)
                if not isinstance(a,dict):continue
                self.identity(key,a,p)
                expected={'AtlasRenderer':'AtlasAnimationDesc','ImageRenderer':'AnimationDesc','com.bladecoder.engine.spine.SpineRenderer':'SpineAnimationDesc'}[cls]
                descriptor=a.get('class',expected)
                if descriptor not in (expected,'com.bladecoder.engine.anim.'+expected):
                    self.emit('renderer-combination','error',pointer(p,'class'),f'{cls} requires {expected} descriptors.')
                source=a.get('source')
                if cls=='AtlasRenderer':
                    self.atlas(source,pointer(p,'source'),key)
                elif cls=='ImageRenderer':
                    self.resource('images',source[1:] if isinstance(source,str) and source.startswith('@') else source,pointer(p,'source'))
                    if isinstance(source,str) and source.startswith('@'):
                        self.emit('localized-image','warning',pointer(p,'source'),'Checked literal fallback; locale-specific selection is not simulated.')
                else:
                    self.resource('spine',source,pointer(p,'source'),'.skel')
                    self.atlas(a.get('atlas') or source,pointer(p,'source'))
                if a.get('sound'):self.sound_ref(a['sound'],pointer(p,'sound'),actor)
            if r.get('initAnimation'):self.animation(actor,r['initAnimation'],pointer(path,'initAnimation'))
            if cls.endswith('SpineRenderer'):
                self.emit('spine-coverage','warning',path,'Skeleton/atlas files checked; binary skeleton animation names, skins, attachments and runtime compatibility remain unverified.')

    def identity(self,key,value,path):
        if isinstance(value,dict) and value.get('id')!=key:
            self.emit('id','error',pointer(path,'id'),f'Map key {key!r} differs from stored id {value.get("id")!r}.')

    def sound_ref(self,value,path,actor=None):
        sounds=self.model.get('sounds',{})
        if actor and isinstance(actor.get('sounds'),dict):sounds={**sounds,**actor['sounds']}
        if isinstance(value,str) and value and not self.dynamic(value,path) and value not in sounds:
            self.emit('sound-ref','error',path,f'Unknown sound ID {value!r}.')

    def geometry(self,actor,path):
        points=actor.get('bbox')
        if points==[] and actor.get('class') in ('WalkZoneActor','ObstacleActor'):
            self.emit('geometry','error',pointer(path,'bbox'),'Navigation polygon cannot be empty.')
        if isinstance(points,list) and points and all(finite(v) for v in points):
            if len(points)<6:
                self.emit('geometry','error',pointer(path,'bbox'),'A nonempty polygon needs at least three vertices.')
            if len(points)%2:
                self.emit('geometry','error',pointer(path,'bbox'),'Polygon must have x/y pairs.')
            elif len(points)>=6:
                pairs=list(zip(points[::2],points[1::2]))
                area=sum(x*y2-x2*y for (x,y),(x2,y2) in zip(pairs,pairs[1:]+pairs[:1]))
                if area==0 and actor.get('class') in ('WalkZoneActor','ObstacleActor'):
                    self.emit('geometry','error',pointer(path,'bbox'),'Navigation polygon has zero area.')
        if actor.get('class')=='CharacterActor' and finite(actor.get('walkingSpeed',1)) and float(actor.get('walkingSpeed',1))<=0:
            self.emit('geometry','warning',pointer(path,'walkingSpeed'),'Nonpositive walking speed may prevent movement.')

    def action_value(self,v,meta,path):
        if v is None:return
        typ=meta['java_type']
        enum=meta.get('enum')
        if enum:
            if not isinstance(v,str) or v.upper() not in enum:self.emit('action-enum','error',path,f'Expected one of {enum}, found {v!r}.')
        elif typ in ('boolean','Boolean'):
            if type(v) is not bool and (not isinstance(v,str) or v.lower() not in ('true','false')):self.emit('action-type','error',path,'Expected boolean or boolean string.')
        elif typ in ('float','Float','int','Integer'):
            integer=typ in ('int','Integer')
            valid=finite(v)
            if valid:
                valid=bool(re.fullmatch(r'[+-]?\d+',str(v))) and -(2**31)<=int(v)<2**31 if integer else abs(float(v))<=3.4028234663852886e38
            if not valid:self.emit('action-type','error',path,f'Expected finite Java {typ} parameter within its numeric range.')
        elif typ=='Vector2':
            if not isinstance(v,str) or len(re.split('[,_]',v))!=2 or not all(finite(x) for x in re.split('[,_]',v)):
                self.emit('action-type','error',path,'Expected vector parameter as "x,y" or "x_y".')
        elif typ=='Color':
            if not isinstance(v,str) or not (v in ('black','white') or re.fullmatch(r'#?(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})',v)):
                self.emit('action-type','error',path,'Expected black/white or 6/8-digit hexadecimal color.')
        elif typ in ('String','SceneActorRef','ActorAnimationRef'):
            if not isinstance(v,str):self.emit('action-type','error',path,f'Expected string {typ} parameter.')
        else:
            self.emit('action-type-coverage','warning',path,f'Unsupported action parameter Java type {typ}.')

    def actions(self,actions,path,owner=None):
        if not isinstance(actions,list):return
        controls={}
        for index,a in enumerate(actions):
            p=pointer(path,index)
            if not isinstance(a,dict):continue
            self.stats['actions']+=1
            cls=a.get('class')
            info=self.contract['actions'].get(cls)
            if not info:
                status = 'warning' if isinstance(cls,str) and '.' in cls else 'error'
                self.emit('action-class',status,pointer(p,'class'),f'Uncatalogued action {cls!r}; custom extension requires review.')
                if isinstance(a.get('caID'),str):controls.setdefault(a['caID'],[]).append(('UnknownControl',p))
                continue
            short=info['class'].rsplit('.',1)[-1]
            fields=info['fields']
            if short=='DisableActionAction':
                self.emit('disabled','warning',p,'Disabled action retained; serializedAction is intentionally not executed or reference-validated.')
            for key,v in a.items():
                if key=='class':continue
                q=pointer(p,key)
                if key not in fields:
                    self.emit('action-field','error',q,f'Unknown action parameter {key!r} for {cls}.')
                    continue
                meta=fields[key];self.action_value(v,meta,q)
                if not isinstance(v,str) or not v or self.dynamic(v,q):continue
                semantic=meta['semantic_type'];typ=meta['java_type']
                if short=='CameraAction' and key=='followActor' and v=='none':continue
                if typ=='SceneActorRef' or semantic in ('ACTOR','CHARACTER_ACTOR','INTERACTIVE_ACTOR','SPRITE_ACTOR','WALKZONE_ACTOR','SCENE_ACTOR','SCENE_CHARACTER_ACTOR','SCENE_INTERACTIVE_ACTOR','SCENE_SPRITE_ACTOR','SCENE_WALKZONE_ACTOR'):
                    resolved=self.actor(v,q,qualified=typ=='SceneActorRef')
                    kind=resolved.get('class') if isinstance(resolved,dict) else None
                    allowed=None
                    if 'CHARACTER_ACTOR' in semantic:allowed={'CharacterActor'}
                    elif 'SPRITE_ACTOR' in semantic:allowed={'SpriteActor','CharacterActor'}
                    elif 'INTERACTIVE_ACTOR' in semantic:allowed={'InteractiveActor','SpriteActor','CharacterActor'}
                    elif 'WALKZONE_ACTOR' in semantic:allowed={'WalkZoneActor'}
                    if allowed and kind in ACTORS and kind not in allowed:
                        self.emit('actor-combination','error',q,f'{cls}.{key} requires {sorted(allowed)}, found {kind}.')
                elif semantic=='SCENE':self.scene_ref(v,q)
                elif typ=='ActorAnimationRef':
                    sep='#' if '#' in v else '.' if '.' in v else None
                    aid,anim=v.split(sep,1) if sep else (None,v)
                    target=self.actor(aid,q) if aid else owner
                    if short=='SpineSecondaryAnimationAction':
                        self.emit('secondary-animation','warning',q,'Secondary animation names require a Spine binary reader; empty animation clears the track.')
                    elif anim:self.animation(target,anim,q)
                elif semantic=='SOUND':self.sound_ref(v,q,owner)
                elif semantic in ('MUSIC_ASSET','SOUND_ASSET','PARTICLE_ASSET','FONT_ASSET'):
                    self.resource({'MUSIC_ASSET':'music','SOUND_ASSET':'sounds','PARTICLE_ASSET':'particles','FONT_ASSET':'ui'}[semantic],v,q)
                elif semantic=='ATLAS_ASSET':self.atlas(v,q)
                elif semantic=='CHAPTER':self.resource('model',v,q,'.chapter.json')
                elif semantic=='LAYER':
                    actor_ref=a.get('actor')
                    layer_scene=self.scene_id
                    if isinstance(actor_ref,str) and '#' in actor_ref:layer_scene=actor_ref.split('#',1)[0]
                    names={x.get('name') for x in self.scenes.get(layer_scene,{}).get('layers',[]) if isinstance(x,dict)}
                    if layer_scene is None or (isinstance(actor_ref,str) and actor_ref.startswith('$')):
                        self.emit('layer-dynamic','warning',q,'Layer lookup depends on runtime actor context.',resolution='dynamic')
                    elif v not in names:self.emit('layer-ref','error',q,f'Unknown target scene layer {v!r}.')
            for key,meta in fields.items():
                if meta['required_hint'] and meta['java_type'] not in ('boolean','float','int') and meta['initializer'] in (None,'null') and not a.get(key):
                    if (short,key) in NULLABLE_HINTS:continue
                    definite=key=='caID' or key in RUNTIME_REQUIRED.get(short,set())
                    self.emit('action-required','error' if definite else 'warning',pointer(p,key),
                              f'Action {cls} omits editor-required {key}; '+('runtime requires it.' if definite else 'runtime necessity has not been established.'))
            if 'caID' in fields:
                cid=a.get('caID')
                if isinstance(cid,str):controls.setdefault(cid,[]).append((short,p))
            if short in ('DropItemAction','PickUpAction','SetPlayerAction') and a.get('inventory'):
                inventory=a['inventory']
                code='inventory-dynamic' if inventory in self.inventory_ids else 'inventory-ref'
                detail='declared by SetPlayer or initially available' if code=='inventory-dynamic' else 'not declared by any initial SetPlayer action'
                self.emit(code,'warning',pointer(p,'inventory'),f'Inventory {inventory!r} is {detail}; runtime creation/order/membership remain unverified.',resolution='dynamic')
            if short=='DropItemAction' and a.get('actor'):
                self.actor(a['actor'],pointer(p,'actor'))
            if short=='MusicAction' and a.get('filename'):
                self.resource('music',a['filename'],pointer(p,'filename'))
            if short=='InkNewStoryAction' and a.get('storyName'):
                self.resource('model',a['storyName'],pointer(p,'storyName'),'.ink.json')
            if short=='InkRunAction' and isinstance(a.get('path'),str):
                dest=a['path']
                if not self.dynamic(dest,pointer(p,'path')):
                    if self.ink is None or self.story_changes:self.emit('ink-dynamic','warning',pointer(p,'path'),'No single fixed Ink story is available; destination unresolved.',resolution='dynamic')
                    elif dest not in self.ink_paths:self.emit('ink-ref','error',pointer(p,'path'),f'Unknown named compiled Ink destination {dest!r}.')
            if short in ('RunVerbAction','CancelVerbAction') and a.get('verb'):
                target=self.actor(a['actor'],pointer(p,'actor')) if a.get('actor') else None
                pools=[self.world.get('verbs',{}),self.scenes.get(self.scene_id,{}).get('verbs',{})]
                if target:pools.insert(0,target.get('verbs',{}))
                ids={v.get('id') for pool in pools if isinstance(pool,dict) for v in pool.values() if isinstance(v,dict)}
                if a['verb'] not in ids:
                    self.emit('verb-ref','warning',pointer(p,'verb'),f'Verb {a["verb"]!r} is not in the initial lookup scopes; state, actor or global context may change lookup.',resolution='dynamic')
            if short=='LeaveAction' and a.get('initVerb'):
                destination=a.get('scene')
                if isinstance(destination,str) and destination.startswith('$'):
                    self.emit('verb-ref','warning',pointer(p,'initVerb'),f'Entry verb {a["initVerb"]!r} targets runtime scene {destination!r}; destination scope is unresolved.',resolution='dynamic')
                else:
                    target=self.scenes.get(destination,{})
                    pools=[target.get('verbs',{}),self.world.get('verbs',{})]
                    ids={v.get('id') for pool in pools for v in pool.values() if isinstance(v,dict)}
                    if a['initVerb'] not in ids:self.emit('verb-ref','error',pointer(p,'initVerb'),f'Unknown destination scene/global entry verb {a["initVerb"]!r}.')
        for cid,entries in controls.items():
            if any(e[0]=='UnknownControl' for e in entries):
                self.emit('control-coverage','warning',entries[0][1],f'Control ID {cid!r} involves an uncatalogued extension; block shape unverified.')
                continue
            remaining=0
            for kind,p in entries:
                if kind=='EndAction':
                    if not remaining:self.emit('control','error',p,f'Orphan End for control ID {cid!r}.')
                    else:remaining-=1
                else:
                    if remaining:self.emit('control','error',p,f'Control ID {cid!r} is reused before its preceding block is closed.')
                    remaining=2 if kind.startswith('If') else 1
            if remaining:self.emit('control','error',entries[-1][1],f'Control ID {cid!r} lacks {remaining} closing End action(s).')

    def verbs(self,owner,path,required=False):
        if required and 'verbs' not in owner:
            self.emit('verbs-required','error',pointer(path,'verbs'),'MODEL readers require an explicit verbs object, even when empty; omission leaves a null VerbManager map.')
        verbs=owner.get('verbs',{})
        if not isinstance(verbs,dict):return
        for key,verb in verbs.items():
            p=pointer(pointer(path,'verbs'),key)
            if not isinstance(verb,dict):continue
            # Engine key is id[.target][.state], including empty target before state.
            expected=verb.get('id','')
            if verb.get('target') is not None:expected+='.'+verb['target']
            if verb.get('state') is not None:expected+='.'+verb['state']
            if key!=expected:self.emit('verb-key','error',p,f'Verb key {key!r} differs from Verb.getHashKey() {expected!r}.')
            if verb.get('target'):self.actor(verb['target'],pointer(p,'target'))
            self.actions(verb.get('actions'),pointer(p,'actions'),owner if 'class' in owner else None)

    def dialogs(self,actor,path):
        dialogs=actor.get('dialogs',{})
        if not isinstance(dialogs,dict):return
        for key,dialog in dialogs.items():
            p=pointer(pointer(path,'dialogs'),key)
            if not isinstance(dialog,dict):continue
            self.identity(key,dialog,p)
            for index,option in enumerate(dialog.get('options',[])):
                if not isinstance(option,dict):continue
                q=pointer(pointer(p,'options'),index)
                next_dialog=option.get('next')
                if next_dialog and next_dialog!='this' and next_dialog not in dialogs:
                    self.emit('dialog-ref','error',pointer(q,'next'),f'Unknown actor dialog {next_dialog!r}.')
                verb_id=option.get('verbId')
                ids={v.get('id') for v in actor.get('verbs',{}).values() if isinstance(v,dict)}
                ids|={v.get('id') for v in self.world.get('verbs',{}).values() if isinstance(v,dict)}
                if verb_id and verb_id not in ids:self.emit('verb-ref','warning',pointer(q,'verbId'),f'Dialog verb {verb_id!r} is absent from initial actor/global scopes.',resolution='dynamic')

    def translations(self,value,path,keys):
        if isinstance(value,dict):
            for k,v in value.items():
                p=pointer(path,k)
                # Only fields interpreted as translated text; @ image sources are separate.
                if k in ('desc','text','responseText') and isinstance(v,str) and v.startswith('@') and v[1:] not in keys:
                    self.emit('localization','error',p,f'Missing localization key {v[1:]!r} in the owning model bundle.')
                if k!='serializedAction':self.translations(v,p,keys)
        elif isinstance(value,list):
            for i,v in enumerate(value):self.translations(v,pointer(path,i),keys)

    def bundle(self,path):
        # Java properties keys support escaped separators and continuation lines.
        keys=set();pending=''
        if not path.is_file():return keys
        try:lines=path.read_text(encoding='utf-8-sig').splitlines()
        except (OSError,UnicodeError) as exc:
            self.emit('bundle-read','error','',f'Cannot read owning localization bundle {path.name}: {type(exc).__name__}.')
            return keys
        for line in lines:
            line=pending+line.lstrip() if pending else line.lstrip()
            if len(line)-len(line.rstrip('\\')) & 1:
                pending=line[:-1];continue
            pending=''
            if not line or line.startswith(('#','!')):continue
            match=re.match(r'((?:\\.|[^\s=:])*)',line)
            key=match[1]
            key=re.sub(r'\\u([0-9a-fA-F]{4})',lambda m:chr(int(m[1],16)),key)
            key=re.sub(r'\\(.)',r'\1',key)
            keys.add(key)
        return keys

    def load_ink(self,story,path):
        self.ink=None;self.ink_paths=set()
        if not isinstance(story,str) or not story:return
        files=self.resource('model',story,path,'.ink.json')
        if not files:return
        try:
            self.ink=json.loads(files[0].read_text(encoding='utf-8-sig'))
            if not isinstance(self.ink,dict) or not isinstance(self.ink.get('root'),list):
                raise ValueError('Expected an Ink object with a root container array')
            def walk(container,prefix=''):
                if not isinstance(container,list):return
                if container and isinstance(container[-1],dict):
                    for name,child in container[-1].items():
                        if name.startswith('#') or not isinstance(child,list):continue
                        full=prefix+'.'+name if prefix else name
                        self.ink_paths.add(full);walk(child,full)
            walk(self.ink.get('root'))
        except (OSError,UnicodeError,ValueError,AttributeError) as exc:
            self.ink=None
            self.emit('ink-read','error',path,f'Cannot inspect compiled Ink story: {exc}')

    def chapter(self,model):
        self.scenes={}
        before=sum(c['status']=='error' for c in self.checks)
        self.structure(model,'chapter')
        if sum(c['status']=='error' for c in self.checks)>before:
            self.emit('incomplete','warning','','Semantic checks skipped for this chapter after structural errors; repair structure and rerun.')
            return False
        self.model=model; self.scenes={k:v for k,v in model.get('scenes',{}).items() if isinstance(v,dict)}
        def changes(value):
            if isinstance(value,dict):
                if value.get('class') in ('InkNewStory','com.bladecoder.engine.actions.InkNewStoryAction'):return True
                return any(changes(v) for k,v in value.items() if k!='serializedAction')
            return isinstance(value,list) and any(changes(v) for v in value)
        self.story_changes=changes(model) or changes(self.world)
        self.inventory_ids={'DEFAULT'}
        def inventories(value):
            if isinstance(value,dict):
                if value.get('class') in ('SetPlayer','com.bladecoder.engine.actions.SetPlayerAction') and isinstance(value.get('inventory'),str):
                    self.inventory_ids.add(value['inventory'])
                for k,v in value.items():
                    if k!='serializedAction':inventories(v)
            elif isinstance(value,list):
                for v in value:inventories(v)
        inventories(model);inventories(self.world)
        version=model.get('bladeEngineVersion')
        if version!=self.contract['engine_version']:
            self.emit('version','warning','/bladeEngineVersion',f'Model declares {version!r}; contract targets {self.contract["engine_version"]}.')
        self.scene_ref(model.get('initScene'),'/initScene')
        ink=model.get('inkManager')
        self.load_ink(ink.get('storyName') if isinstance(ink,dict) else None,'/inkManager/storyName')
        for key,sound in model.get('sounds',{}).items():
            if isinstance(sound,dict):
                p=pointer('/sounds',key);self.identity(key,sound,p);self.resource('sounds',sound.get('filename'),pointer(p,'filename'))
        for sid,scene in self.scenes.items():
            self.scene_id=sid;self.stats['scenes']+=1
            p=pointer('/scenes',sid);self.identity(sid,scene,p)
            layers=scene.get('layers',[])
            names=[v.get('name') for v in layers if isinstance(v,dict)] if isinstance(layers,list) else []
            if len(names)!=len(set(names)):self.emit('layer','error',pointer(p,'layers'),'Duplicate layer names.')
            actors=scene.get('actors',{})
            if not isinstance(actors,dict):continue
            for field,expected in [('player','CharacterActor'),('walkZone','WalkZoneActor')]:
                value=scene.get(field)
                if value:
                    target=actors.get(value) if isinstance(value,str) else None
                    if not target:self.emit('actor-ref','error',pointer(p,field),f'Initial {field} actor {value!r} is absent from scene.')
                    elif target.get('class')!=expected:self.emit('actor-combination','error',pointer(p,field),f'{field} must reference {expected}.')
            if scene.get('backgroundAtlas'):self.atlas(scene['backgroundAtlas'],pointer(p,'backgroundAtlas'),scene.get('backgroundRegionId'))
            elif scene.get('backgroundRegionId'):self.emit('background','error',pointer(p,'backgroundRegionId'),'Background region has no background atlas.')
            music=scene.get('musicDesc')
            if isinstance(music,dict):self.resource('music',music.get('filename'),pointer(pointer(p,'musicDesc'),'filename'))
            size=scene.get('sceneSize')
            if isinstance(size,dict) and any(finite(size.get(k,0)) and float(size.get(k,0))<=0 for k in ('x','y')):self.emit('geometry','error',pointer(p,'sceneSize'),'Explicit scene dimensions must be positive (omitted vector components default to zero).')
            for aid,actor in actors.items():
                if not isinstance(actor,dict):continue
                self.stats['actors']+=1
                q=pointer(pointer(p,'actors'),aid);self.identity(aid,actor,q);self.geometry(actor,q)
                if actor.get('layer') and actor['layer'] not in names:self.emit('layer-ref','error',pointer(q,'layer'),f'Unknown scene layer {actor["layer"]!r}.')
                self.renderer(actor,pointer(q,'renderer'));self.verbs(actor,q,required=actor.get('class') in {'InteractiveActor','SpriteActor','CharacterActor'});self.dialogs(actor,q)
            self.verbs(scene,p,required=True)
        chapter_id=Path(self.file).name.removesuffix('.chapter.json')
        self.translations(model,'',self.bundle(self.root/'assets/model'/f'{chapter_id}.properties'))
        self.stats['chapters']+=1
        return True


def validate_chapters(root,files=None):
    validator=Validator(root)
    model_dir=validator.root/'assets/model'
    world_path=model_dir/'world'
    world=validator.read(world_path)
    world_lines,world_file=validator.lines,validator.file
    world_valid=False
    if world is not None:
        validator.world=world
        before=sum(c['status']=='error' for c in validator.checks)
        validator.structure(world,'world')
        world_valid=sum(c['status']=='error' for c in validator.checks)==before
        if not world_valid:validator.world={}
        for key in ('width','height'):
            if finite(world.get(key)) and float(world[key])<=0:validator.emit('geometry','error','/'+key,'World viewport dimensions must be positive.')
        chapter=world.get('initChapter')
        if isinstance(chapter,str) and not (model_dir/(chapter+'.chapter.json')).is_file():validator.emit('chapter-ref','error','/initChapter',f'Initial chapter {chapter!r} does not exist.')
    paths=[Path(p) if Path(p).is_absolute() else validator.root/p for p in files] if files else sorted(model_dir.glob('*.chapter.json'))
    if not paths:validator.emit('missing','error','','No chapter models found.')
    for path in paths:
        model=validator.read(path)
        completed=False
        if model is not None:
            try:completed=validator.chapter(model)
            except (TypeError,AttributeError,KeyError) as exc:
                # Shape errors have already been reported; don't invent successful semantics.
                validator.emit('incomplete','error','',f'Semantic checks could not continue after malformed structure ({type(exc).__name__}).')
        if world_valid and completed:
            validator.file,validator.lines=world_file,world_lines;validator.scene_id=None
            validator.verbs(world,'',required=True)
            validator.translations(world,'',validator.bundle(model_dir/'world.properties'))
    validator.checks.append({'id':'chapter.coverage','status':'ok','message':'Completed supported MODEL checks; warnings describe unresolved runtime references and reader limits. This is not gameplay verification.',**validator.stats})
    return validator.checks

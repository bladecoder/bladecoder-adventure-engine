"""Mutation tests for MODEL structure, lookup, resources and runtime uncertainty."""
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.dont_write_bytecode=True
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from chapter_validate import validate_chapters, Validator
from model_json import ModelJSON, ModelSyntaxError


class ChapterTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(prefix='adventure-chapters-')
        self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)
        (self.root/"adventure.json").write_text('{"schema_version":1,"art_storage":"external","capabilities":{}}')
        self.model={'bladeEngineVersion':'4.5.0-SNAPSHOT','sounds':{'pop':{'id':'pop','filename':'pop.wav'}},
            'initScene':'room','inkManager':{'storyName':'00'},'scenes':{'room':{
                'id':'room','layers':[{'name':'background'}],
                'backgroundAtlas':'room','backgroundRegionId':'background','player':'Hero','walkZone':'walkzone',
                'verbs':{'init':{'id':'init','actions':[]}},
                'actors':{
                    'Hero':{'class':'CharacterActor','id':'Hero','verbs':{},'bbox':[0,0,1,0,1,1], 'layer':'background',
                        'renderer':{'class':'AtlasRenderer','initAnimation':'stand.right','fanims':{
                            'stand.right':{'class':'AtlasAnimationDesc','id':'stand.right','source':'room'}}}},
                    'anchor':{'class':'AnchorActor','id':'anchor','bbox':[]},
                    'walkzone':{'class':'WalkZoneActor','id':'walkzone','bbox':[0,0,10,0,10,10,0,10]}}}}}
        self.world={'width':1920,'height':1080,'initChapter':'00','verbs':{}}
        self.write('assets/atlases/1/room.atlas','room.png\nsize: 8,8\nfilter: Linear,Linear\nbackground\n  xy: 0,0\nstand.right\n  index: 0\n')
        self.write('assets/atlases/1/room.png','image presence only')
        self.write('assets/sounds/pop.wav','audio presence only')
        self.write('assets/model/00.properties','description=Descripción\n')
        self.write('assets/model/world.properties','global=Global\n')
        self.write('assets/model/00.ink.json',json.dumps({'inkVersion':21,'root':[['done'],{'dialog':['done',{'reply':['done',None]}]}]}))

    def write(self,name,text):
        path=self.root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text);return path

    def run_model(self,actions=None):
        if actions is not None:self.model['scenes']['room']['verbs']['init']['actions']=actions
        self.write('assets/model/00.chapter.json',json.dumps(self.model))
        self.write('assets/model/world',json.dumps(self.world))
        return validate_chapters(self.root)

    def errors(self,checks):return [x for x in checks if x['status']=='error']

    def assert_code(self,checks,code,status='error'):
        found=[x for x in checks if x['id']=='chapter.'+code and x['status']==status]
        self.assertTrue(found,checks)
        for item in found:self.assertIn('json_path',item);self.assertIn('path',item)
        return found

    def test_project_actions_are_explicit_and_type_checked(self):
        action={'class':'org.example.adventure.actions.SignalAction','enabled':True}
        self.assert_code(self.run_model([action]),'action-class','warning')
        declaration={'schema_version':1,'capabilities':{},'model_contract':'ai-toolkit/tools/project-actions.json'}
        self.write('adventure.json',json.dumps(declaration))
        extension={'schema_version':1,'engine_version':'4.5.0-SNAPSHOT','actions':{
            action['class']:{'class':action['class'],'fields':{
                'enabled':{'java_type':'boolean','semantic_type':'NOT_SET','required_hint':False,'initializer':'false'}}}}}
        self.write('ai-toolkit/tools/project-actions.json',json.dumps(extension))
        checks=self.run_model([action]);self.assertFalse(self.errors(checks),checks)
        self.assertFalse(any(row['id']=='chapter.action-class' for row in checks))
        self.assert_code(self.run_model([{**action,'enabled':27}]),'action-type')

    def test_project_contract_cannot_replace_engine_actions(self):
        self.write('adventure.json',json.dumps({'schema_version':1,'model_contract':'ai-toolkit/tools/project-actions.json'}))
        self.write('ai-toolkit/tools/project-actions.json',json.dumps({'schema_version':1,'actions':{'Wait':{'class':'org.example.Wait','fields':{}}}}))
        checks=self.run_model([{'class':'Wait','time':'invalid'}])
        self.assert_code(checks,'contract');self.assert_code(checks,'action-type')

    def test_valid_model_and_read_only(self):
        checks=self.run_model();self.assertFalse(self.errors(checks),checks)
        before={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in self.root.rglob('*') if p.is_file()}
        validate_chapters(self.root)
        self.assertEqual(before,{p:hashlib.sha256(p.read_bytes()).hexdigest() for p in self.root.rglob('*') if p.is_file()})

    def test_empty_verb_maps_are_explicit_runtime_requirements(self):
        del self.model['scenes']['room']['actors']['Hero']['verbs']
        self.assert_code(self.run_model(),'verbs-required')
        self.model['scenes']['room']['actors']['Hero']['verbs']={}
        del self.model['scenes']['room']['verbs']
        self.assert_code(self.run_model(),'verbs-required')
        self.model['scenes']['room']['verbs']={}
        del self.world['verbs']
        self.assert_code(self.run_model(),'verbs-required')

    def test_id_and_initial_references(self):
        scene=self.model['scenes']['room'];scene['id']='wrong';scene['player']='absent';scene['walkZone']='Hero'
        checks=self.run_model();self.assert_code(checks,'id');self.assert_code(checks,'actor-ref');self.assert_code(checks,'actor-combination')

    def test_bad_types_unknown_fields_state_and_classes(self):
        for key,value in [('visible','yes'),('inventories',{}),('typo',True),('class','CharactreActor')]:
            with self.subTest(key=key):
                original=copy.deepcopy(self.model)
                self.model['scenes']['room']['actors']['Hero'][key]=value
                self.assertTrue(self.errors(self.run_model()))
                self.model=original
        self.model['scenes']['room']['actors']['anchor']['class']='org.example.CustomActor'
        self.assert_code(self.run_model(),'class','warning')

    def test_wrong_scene_actor_and_layer(self):
        self.assert_code(self.run_model([{'class':'ActorState','actor':'missing#Hero'}]),'scene-ref')
        self.assert_code(self.run_model([{'class':'ActorState','actor':'room#missing'}]),'actor-ref')
        self.assert_code(self.run_model([{'class':'SetActorAttr','actor':'Hero','layer':'missing'}]),'layer-ref')

    def test_dynamic_and_moved_actors_are_not_errors(self):
        self.model['scenes']['storage']={'id':'storage','verbs':{},'layers':[],'actors':{'item':{'class':'InteractiveActor','id':'item','verbs':{},'bbox':[]}}}
        checks=self.run_model([{'class':'ActorState','actor':'$PLAYER'},{'class':'ActorState','actor':'item'}])
        self.assertFalse(self.errors(checks),checks)
        self.assert_code(checks,'dynamic','warning');self.assert_code(checks,'runtime-actor','warning')

    def test_editor_required_hints_do_not_reject_primitive_defaults(self):
        self.assertFalse(self.errors(self.run_model([{'class':'Animation','animation':'Hero#stand.left'}])))
        self.assert_code(self.run_model([{'class':'Animation'}]),'action-required')

    def test_action_types_enums_inherited_fields_and_unknown_params(self):
        for action,code in [({'class':'Wait','time':'abc'},'action-type'),
                             ({'class':'Animation','animation':'Hero#stand.right','repeat':'bad'},'action-enum'),
                             ({'class':'Position','actor':'Hero','position':'a,b'},'action-type'),
                             ({'class':'SetActorAttr','actor':'Hero','tint':'purple'},'action-type'),
                             ({'class':'Wait','time':1,'typo':0},'action-field'),
                             ({'class':'NotReal'},'action-class'),
                             ({'class':'IfProperty','name':'x','value':'1'},'action-required')]:
            with self.subTest(action=action):self.assert_code(self.run_model([action]),code)
        self.assertFalse(self.errors(self.run_model([{'class':'Wait','time':'1.5'},{'class':'Camera','followActor':'none'}])))
        self.assertFalse(self.errors(self.run_model([{'class':'Cutmode','value':'TrUe'}])))
        self.assert_code(self.run_model([{'class':'Animation','animation':'Hero#stand.right','count':2**40}]),'action-type')
        self.assert_code(self.run_model([{'class':'Wait','time':'1e100'}]),'action-type')

    def test_flat_control_blocks_and_disabled_action(self):
        cond={'class':'IfProperty','name':'x','value':'1','caID':'A'}
        end={'class':'End','caID':'A'}
        self.assertFalse(self.errors(self.run_model([cond,end,end])))
        self.assertFalse(self.errors(self.run_model([cond,end,end,cond,end,end])))
        self.assert_code(self.run_model([cond,end]),'control')
        self.assert_code(self.run_model([end]),'control')
        self.assertFalse(self.errors(self.run_model([{'class':'RunOnce','caID':'B'},{'class':'End','caID':'B'}])))
        checks=self.run_model([{'class':'DisableAction','serializedAction':'{"class":"Leave","scene":"missing"}'}])
        self.assertFalse(self.errors(checks));self.assert_code(checks,'disabled','warning')
        checks=self.run_model([{'class':'example.CustomIf','caID':'A'},end,end])
        self.assertFalse(self.errors(checks));self.assert_code(checks,'control-coverage','warning')

    def test_localized_text_keys_and_image_source_are_separate(self):
        hero=self.model['scenes']['room']['actors']['Hero'];hero['desc']='@description'
        self.assertFalse(self.errors(self.run_model()))
        hero['desc']='@missing';self.assert_code(self.run_model(),'localization')

    def test_ink_paths_from_compiled_named_containers(self):
        self.assertFalse(self.errors(self.run_model([{'class':'InkRun','path':'dialog.reply'}])))
        self.assert_code(self.run_model([{'class':'InkRun','path':'missing'}]),'ink-ref')
        self.model.pop('inkManager')
        checks=self.run_model([{'class':'InkRun','path':'dialog.reply'}])
        self.assertFalse(self.errors(checks));self.assert_code(checks,'ink-dynamic','warning')

    def test_runtime_story_switch_and_qualified_player(self):
        checks=self.run_model([{'class':'InkNewStory','storyName':'00'}, {'class':'InkRun','path':'runtime_path'},
                               {'class':'ActorState','actor':'room#$PLAYER'}])
        self.assertFalse(self.errors(checks));self.assert_code(checks,'ink-dynamic','warning')
        self.assert_code(checks,'dynamic','warning')

    def test_actor_type_and_renderer_descriptor_combinations(self):
        self.assert_code(self.run_model([{'class':'Goto','actor':'anchor'}]),'actor-combination')
        self.model['scenes']['room']['actors']['Hero']['renderer']['fanims']['stand.right']['class']='SpineAnimationDesc'
        self.assert_code(self.run_model([]),'renderer-combination')

    def test_dialog_references_and_enum_with_constructor_arguments(self):
        hero=self.model['scenes']['room']['actors']['Hero']
        hero['dialogs']={'dialog':{'id':'dialog','options':[{'next':'missing'}]}}
        self.assert_code(self.run_model(),'dialog-ref')
        hero['dialogs']['dialog']['options'][0]['next']='this'
        self.assertFalse(self.errors(self.run_model([{'class':'LookAt','direction':'left'}])))
        self.assert_code(self.run_model([{'class':'LookAt','direction':'bogus'}]),'action-enum')

    def test_explicit_cli_paths_and_failure_exit_from_another_directory(self):
        self.run_model([{'class':'Leave','scene':'absent'}])
        import project
        from unittest.mock import patch
        import contextlib,io
        with patch.object(project,'ROOT',self.root):
            out=io.StringIO()
            with contextlib.redirect_stdout(out):code=project.main(['chapters','assets/model/00.chapter.json','--json'])
        self.assertEqual(code,1)
        report=json.loads(out.getvalue());self.assertEqual(report['command'],'chapters')
        self.assert_code(report['checks'],'scene-ref')
        # Real independent-directory invocation against this project's read-only command.
        import shutil
        shutil.copytree(Path(__file__).resolve().parents[1],self.root/'ai-toolkit/tools',dirs_exist_ok=True,ignore=shutil.ignore_patterns('tests','__pycache__'))
        script=self.root/'ai-toolkit/tools/project.py'
        run=subprocess.run([sys.executable,'-B',str(script),'chapters','--json'],cwd=self.root.parent,capture_output=True,text=True,timeout=15)
        self.assertFalse(run.stderr);self.assertIn(run.returncode,(0,1));self.assertEqual(json.loads(run.stdout)['command'],'chapters')

    def test_missing_resource_page_and_atlas_region(self):
        (self.root/'assets/atlases/1/room.png').unlink()
        self.assert_code(self.run_model(),'atlas-page')
        self.write('assets/atlases/1/room.png','x')
        self.model['scenes']['room']['backgroundRegionId']='absent'
        self.assert_code(self.run_model(),'atlas-region')
        self.model['sounds']['pop']['filename']='absent.wav'
        self.assert_code(self.run_model(),'resource')

    def test_animation_declared_name_and_flipped_direction(self):
        self.assertFalse(self.errors(self.run_model([{'class':'Animation','animation':'Hero#stand.left'}])))
        self.assert_code(self.run_model([{'class':'Animation','animation':'Hero#missing.right'}]),'animation-ref')

    def test_resource_escape_and_symlink(self):
        self.model['sounds']['pop']['filename']='../private'
        self.assert_code(self.run_model(),'resource-path')
        outside=self.write('outside.wav','x');(self.root/'assets/sounds/pop.wav').unlink()
        (self.root/'assets/sounds/pop.wav').symlink_to(outside)
        self.model['sounds']['pop']['filename']='pop.wav';self.assert_code(self.run_model(),'resource-path')

    def test_geometry_empty_anchor_valid_odd_and_collinear_nav_invalid(self):
        self.assertFalse(self.errors(self.run_model()))
        anchor=self.model['scenes']['room']['actors']['anchor'];anchor['bbox']=[0,0,1]
        self.assert_code(self.run_model(),'geometry')
        anchor['bbox']=[];self.model['scenes']['room']['actors']['walkzone']['bbox']=[0,0,1,1,2,2]
        self.assert_code(self.run_model(),'geometry')

    def test_verb_hash_entry_verb_and_global_context(self):
        self.model['scenes']['room']['verbs']['use.READY']={'id':'use','state':'READY','actions':[]}
        self.assertFalse(self.errors(self.run_model()))
        self.assert_code(self.run_model([{'class':'Leave','scene':'room','initVerb':'absent'}]),'verb-ref')
        checks=self.run_model([{'class':'Leave','scene':'$PREVIOUS_SCENE','initVerb':'return'}])
        self.assertFalse(self.errors(checks));self.assert_code(checks,'verb-ref','warning')
        self.world['verbs']={'global':{'id':'global','actions':[{'class':'ActorState','actor':'Hero'}]}}
        self.assertFalse(self.errors(self.run_model([])))

    def test_malformed_shapes_fail_without_crashing(self):
        for scenes in ([],None,42,{'room':{'id':'room','actors':[], 'layers':False}}):
            self.model['scenes']=scenes
            self.assertTrue(self.errors(self.run_model()))

    def test_malformed_world_action_is_reported_without_crashing(self):
        self.world['verbs']={'bad':{'id':'bad','actions':[{'class':[]} ]}}
        self.assert_code(self.run_model(),'type')

    def test_inventory_declaration_and_unresolved_creation(self):
        checks=self.run_model([{'class':'SetPlayer','actor':'Hero','inventory':'hero'},
                               {'class':'PickUp','actor':'Hero','inventory':'hero'},
                               {'class':'DropItem','actor':'Hero','inventory':'typo'}])
        self.assertFalse(self.errors(checks));self.assert_code(checks,'inventory-dynamic','warning')
        self.assert_code(checks,'inventory-ref','warning')

    def test_nullable_editor_required_fields_are_valid_at_runtime(self):
        checks=self.run_model([{'class':'Say'},{'class':'PositionAnim','actor':'Hero','pos':'0,0'},
                               {'class':'Text','text':'Hola'}])
        self.assertFalse(self.errors(checks));self.assertFalse([c for c in checks if c['id']=='chapter.action-required'])

    def test_bundle_escaping_and_continuation(self):
        path=self.write('bundle.properties','a\\:b=value\nhello\\u0020world=ok\nkey\\\n  continuation=yes\n')
        self.assertEqual(Validator(self.root).bundle(path),{'a:b','hello world','keycontinuation'})


class ParserTests(unittest.TestCase):
    def test_minimal_strict_comments_and_duplicate_rejection(self):
        self.assertEqual(ModelJSON('/* x */ {foo: [bare, "quoted",], // c\n bar: true,}').parse(),{'foo':['bare','quoted'],'bar':True})
        for text in ('{"a":1,"a":2}','{a:','[1','{} extra','{a: NaN}','{a: 1e999}','{a 1}'):
            with self.subTest(text=text):
                with self.assertRaises(ModelSyntaxError):ModelJSON(text).parse()

    def test_json_pointer_escaping(self):
        parser=ModelJSON('{"a/b~c": [1]}');parser.parse()
        self.assertIn('/a~1b~0c/0',parser.lines)


if __name__=='__main__':unittest.main()

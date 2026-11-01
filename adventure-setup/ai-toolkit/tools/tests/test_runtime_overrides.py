"""External override bundle preparation preserves project sources and validates destinations."""
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure
from runtime_overrides import prepare_bundle


class OverridePreparationTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.base=Path(self.tmp.name);self.root=self.base/'project'
        model=self.root/'assets/model';model.mkdir(parents=True)
        self.chapter=model/'00.chapter.json';self.chapter.write_text(json.dumps({'scenes':{'room':{'actors':{'actor':{'pos':{'x':10.0,'y':15.0},'visible':True}}}}}))
        self.ink=model/'00.ink.json';self.ink.write_text('{"inkVersion":21,"root":[]}')
        self.original=self.chapter.read_bytes()
        self.spec={'schema_version':1,'id':'sample','chapter_changes':[{'resource':'model/00.chapter.json','path':'/scenes/room/actors/actor/pos/x','value':20.0}]}
    def tearDown(self):self.tmp.cleanup()
    def test_bundle_pins_original_and_output_without_editing_sources(self):
        out=self.base/'bundle';result=prepare_bundle(self.root,self.spec,out)
        self.assertEqual(self.chapter.read_bytes(),self.original)
        entry=result['manifest']['files'][0];self.assertEqual(entry['base_sha256'],hashlib.sha256(self.original).hexdigest())
        data=(out/'files'/entry['path']).read_bytes();self.assertEqual(entry['sha256'],hashlib.sha256(data).hexdigest())
        self.assertEqual(json.loads(data)['scenes']['room']['actors']['actor']['pos']['x'],20.0)
        self.assertIn('preview',result['boundary'])
    def test_existing_or_source_destinations_are_left_untouched(self):
        out=self.base/'bundle';out.mkdir();(out/'original').write_text('keep')
        for path in [out,self.root/'assets/new',self.root/'raw/new',self.root/'automation/new',self.root/'ai-toolkit/tools/new']:
            with self.assertRaises(RuntimeFailure):prepare_bundle(self.root,self.spec,path)
        self.assertEqual((out/'original').read_text(),'keep')
        self.assertEqual(self.chapter.read_bytes(),self.original)
    def test_escaping_unsupported_or_type_changing_edits_fail_before_output(self):
        for change in [
            {'resource':'../model/00.chapter.json','path':'/scenes/room/actors/actor/pos/x','value':20.0},
            {'resource':'model/00.chapter.json','path':'/scenes/room/actors/actor/id','value':'changed'},
            {'resource':'model/00.chapter.json','path':'/scenes/room/actors/actor/visible','value':1},
            {'resource':'model/00.chapter.json','path':'/scenes/room/actors/absent/pos/x','value':20.0},
        ]:
            with self.subTest(change=change),self.assertRaises(RuntimeFailure):
                prepare_bundle(self.root,{**self.spec,'chapter_changes':[change]},self.base/'out')
            self.assertFalse((self.base/'out').exists())
    def test_compiled_resource_copy_keeps_explicit_provenance(self):
        source=self.base/'replacement.ink.json';source.write_text(self.ink.read_text())
        out=self.base/'bundle';spec={**self.spec,'resources':[{'path':'model/00.ink.json','source':str(source)}]}
        result=prepare_bundle(self.root,spec,out)
        self.assertEqual(len(result['manifest']['files']),2)
        self.assertEqual((out/'files/model/00.ink.json').read_bytes(),source.read_bytes())
        self.assertEqual(self.ink.read_text(),source.read_text())
    def test_invalid_specification_has_no_filesystem_side_effect(self):
        for spec in [{**self.spec,'schema_version':True},{**self.spec,'resources':[{'path':'model/00.ink.json','source':None}]}, {**self.spec,'id':'../escape'}]:
            with self.assertRaises(RuntimeFailure):prepare_bundle(self.root,spec,self.base/'out')
            self.assertFalse((self.base/'out').exists())


if __name__=='__main__':unittest.main()

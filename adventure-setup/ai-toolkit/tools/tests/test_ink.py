"""Compiler transactions and literal integration checks; real runtimes are checked separately."""
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import subprocess

sys.dont_write_bytecode=True
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from ink_tools import compile_ink
from ink_links import validate_ink_links


class InkToolTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(prefix='ink-tools-');self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)/'project with spaces';self.root.mkdir()
        self.write('gradle.properties','bladeInkVersion=1.3.2\ngdxVersion=1.14.2\n')
        self.write('adventure.json',json.dumps({'schema_version':1,'ink':{'source':'raw/ink/00.ink','output':'assets/model/00.ink.json'}}))
        self.source=self.write('raw/ink/00.ink','Hola\n-> END')
        self.output=self.write('assets/model/00.ink.json','{"inkVersion":21,"root":[]}')
        self.cache=self.root/'cache'
        self.write('cache/com.bladecoder.ink/blade-ink/1.3.2/hash/blade-ink-1.3.2.jar','fake runtime for process-orchestration tests')
        self.compiler=self.executable('fake tools/inklecate', '''import sys,json
from pathlib import Path
source=Path(sys.argv[-1]).read_text()
if 'FAIL' in source: print('deliberate compiler failure');sys.exit(1)
Path(sys.argv[sys.argv.index('-o')+1]).write_text(json.dumps({'inkVersion':21,'root':[source]}))
''')
        self.java=self.executable('fake tools/java', '''import sys,json,os
from pathlib import Path
if '-jar' in sys.argv or any(x.endswith('InkCompiler.java') for x in sys.argv):
 if '-o' in sys.argv: out=Path(sys.argv[sys.argv.index('-o')+1]); source=Path(sys.argv[-1])
 else: source=Path(sys.argv[-2]);out=Path(sys.argv[-1])
 out.write_text(json.dumps({'inkVersion':21,'root':[source.read_text()]}))
else:
 if os.environ.get('INK_PROBE_FAIL'):print('deliberate runtime incompatibility');sys.exit(1)
 json.loads(Path(sys.argv[-1]).read_text())
''')
        self.config={'gradle_cache':str(self.cache),'java':str(self.java),'inklecate':str(self.compiler)}

    def write(self,name,content):
        p=self.root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);return p

    def executable(self,name,body):
        p=self.write(name,'#!'+sys.executable+'\n'+body);p.chmod(0o755);return p

    def errors(self,checks):return [c for c in checks if c['status']=='error']

    def test_compile_and_freshness_are_independent_of_cwd_and_read_only_when_checked(self):
        with patch('os.getcwd',return_value='/tmp'):
            checks=compile_ink(self.root,self.config)
        self.assertFalse(self.errors(checks),checks)
        before=self.output.read_bytes()
        self.assertFalse(self.errors(compile_ink(self.root,self.config,check_only=True)))
        self.assertEqual(before,self.output.read_bytes())
        self.source.write_text('Changed story\n-> END')
        self.assertTrue(self.errors(compile_ink(self.root,self.config,check_only=True)))
        self.assertEqual(before,self.output.read_bytes())
        self.assertFalse(list(self.output.parent.glob('.ink-*')))

    def test_compilation_failure_preserves_original(self):
        before=self.output.read_bytes();self.source.write_text('FAIL')
        checks=compile_ink(self.root,self.config)
        self.assertTrue(self.errors(checks));self.assertEqual(before,self.output.read_bytes())
        self.assertIn('deliberate compiler failure',checks[-1]['diagnostics'])

    def test_runtime_incompatibility_preserves_original(self):
        before=self.output.read_bytes()
        with patch.dict(os.environ,{'INK_PROBE_FAIL':'1'}):checks=compile_ink(self.root,self.config)
        self.assertTrue(self.errors(checks));self.assertEqual(before,self.output.read_bytes())

    def test_explicit_jar_compiler_and_matching_cached_compiler(self):
        jar=self.write('compiler with spaces.jar','fake configured compiler')
        configured={**self.config,'inklecate':str(jar)}
        self.assertFalse(self.errors(compile_ink(self.root,configured)))
        self.write('cache/com.bladecoder.ink/blade-ink-compiler/1.3.2/hash/blade-ink-compiler-1.3.2.jar','fake cached compiler')
        default={k:v for k,v in self.config.items() if k!='inklecate'}
        checks=compile_ink(self.root,default)
        self.assertFalse(self.errors(checks),checks);self.assertIn('cached blade-ink compiler',checks[0]['message'])

    def test_missing_dependencies_invalid_destination_and_timeout(self):
        before=self.output.read_bytes()
        self.assertTrue(self.errors(compile_ink(self.root,{**self.config,'java':'missing-java-command'})))
        self.assertTrue(self.errors(compile_ink(self.root,self.config,output='raw/ink/00.ink')))
        with patch('ink_tools.subprocess.run',side_effect=subprocess.TimeoutExpired('compiler',60)):
            self.assertTrue(self.errors(compile_ink(self.root,self.config)))
        self.assertEqual(before,self.output.read_bytes())


class InkLinkTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(prefix='ink-links-');self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        self.write('adventure.json',json.dumps({'schema_version':1,'ink':{'source':'raw/ink/00.ink','output':'assets/model/00.ink.json'}}))
        self.write('assets/model/world',json.dumps({'width':1920,'height':1080,'initChapter':'00','verbs':{}}))
        self.write('assets/model/00.chapter.json',json.dumps({'scenes':{'inventory':{'actors':{'item':{'id':'item','class':'SpriteActor'}}}}}))
        self.write('raw/ink/00.ink','INCLUDE dialog.ink\nEXTERNAL inInventory(p)\n-> END\n')
        self.write('raw/ink/dialog.ink','=== scene ===\n= talk\n> PickUp: actor=inventory.item\n-> END\n')

    def write(self,name,text):
        p=self.root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);return p

    def test_literal_commands_resolve_and_missing_qualified_actor_is_error(self):
        self.assertFalse([c for c in validate_ink_links(self.root) if c['status']=='error'])
        self.write('raw/ink/dialog.ink','> PickUp: actor=inventory.typo\n')
        errors=[c for c in validate_ink_links(self.root) if c['status']=='error']
        self.assertTrue(errors);self.assertEqual(errors[0]['path'],'raw/ink/dialog.ink');self.assertEqual(errors[0]['line'],1)

    def test_missing_external_binding_arity_and_bad_enum(self):
        self.write('raw/ink/dialog.ink','EXTERNAL inInventory(p,q)\n> Animation: animation=$P.stand.right, repeat=WRONG\n')
        errors=[c for c in validate_ink_links(self.root) if c['status']=='error']
        self.assertEqual(len(errors),2,errors)

    def test_dynamic_references_are_uncertain_not_missing(self):
        self.write('raw/ink/dialog.ink','> SetActorAttr: actor=$P, visible=true\n> PickUp: actor={object_name}\n')
        checks=validate_ink_links(self.root)
        self.assertFalse([c for c in checks if c['status']=='error']);self.assertTrue([c for c in checks if c.get('resolution')=='dynamic'])

    def test_include_cycle_missing_source_and_escape(self):
        for text in ['INCLUDE 00.ink','INCLUDE missing.ink','INCLUDE ../../outside.ink']:
            with self.subTest(text=text):
                self.write('raw/ink/dialog.ink',text)
                self.assertTrue([c for c in validate_ink_links(self.root) if c['id']=='ink.include' and c['status']=='error'])


if __name__=='__main__':unittest.main()

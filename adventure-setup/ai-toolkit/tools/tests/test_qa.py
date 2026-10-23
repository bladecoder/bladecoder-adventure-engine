"""Suite failure/availability semantics and bounded private-data-free diagnostic exports."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure,save_json
from qa_artifacts import export_evidence
from qa_tools import plan,run_qa,run_task


class ArtifactTests(unittest.TestCase):
    def test_private_records_profiles_executables_and_symlinks_never_export(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);source=root/'private';source.mkdir();(source/'profile').mkdir()
            token='actual-run-controller-token'
            save_json(source/'connection.json',{'token':token})
            (source/'profile'/'save.gamestate').write_text('personal-like isolated content')
            (source/'fault.jar').write_bytes(b'executable');(source/'uninspected.zip').write_bytes(b'zip')
            (source/'error.log').write_text('Unexpected error '+token)
            (source/'state.json').write_text('{"error":"original"}');(source/'last.png').write_bytes(b'png')
            outside=root/'outside.log';outside.write_text('do not export');(source/'linked.log').symlink_to(outside)
            result=export_evidence(source,root/'export')
            self.assertTrue(result['complete'])
            self.assertFalse((root/'export/connection.json').exists());self.assertFalse((root/'export/profile').exists())
            self.assertFalse((root/'export/fault.jar').exists());self.assertFalse((root/'export/linked.log').exists())
            self.assertNotIn(token,(root/'export/error.log').read_text())
            self.assertEqual((root/'export/state.json').read_text(),'{"error":"original"}')
            self.assertEqual((root/'export/last.png').read_bytes(),b'png')

    def test_budget_is_explicit_and_never_overwrites_source_or_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);source=root/'source';source.mkdir();(source/'error.log').write_text('original failure')
            result=export_evidence(source,root/'out',max_bytes=3)
            self.assertFalse(result['complete']);self.assertEqual(result['files'],0)
            self.assertEqual(result['entries'][0]['reason'],'budget')
            with self.assertRaises(RuntimeFailure):export_evidence(source,root/'out')
            with self.assertRaises(RuntimeFailure):export_evidence(source,source/'nested')
            self.assertEqual((source/'error.log').read_text(),'original failure')


class SuiteTests(unittest.TestCase):
    def test_native_without_display_is_explicit_and_does_not_start_a_process(self):
        with tempfile.TemporaryDirectory() as tmp,patch.dict('os.environ',{},clear=True),patch('spine_runtime.packaged_spine') as native:
            checks,payload=run_task(Path(tmp),{},Path(tmp),'spine','spine','neutral',Path(tmp)/'game.jar',None)
            self.assertTrue(payload['qa_unavailable']);self.assertEqual(checks[0]['status'],'warning')
            native.assert_not_called()

    def test_unavailable_prerequisite_blocks_comparison_without_inventing_missing_file_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            with patch('qa_tools.plan',return_value=[('visual-matrix','matrix',None),('visual-comparison','pixels','visual-matrix')]),patch('qa_tools.run_task',return_value=([{'status':'warning','message':'No GL'}],{'qa_unavailable':True})) as task:
                checks,result=run_qa(root,{},'fast',root/'out',allow_unavailable=True)
            self.assertEqual(task.call_count,1)
            self.assertTrue(result['passed']);self.assertFalse(result['complete'])
            self.assertEqual(result['tasks'][1]['status'],'unavailable')
            self.assertIn('Blocked by visual-matrix',result['tasks'][1]['checks'][0]['message'])

    def test_packaged_engine_identity_and_jar_change_are_checked_before_each_task(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);jar=root/'game.jar'
            metadata='engine_version=4.5.0-SNAPSHOT\nsource_sha256=actual-source-hash\n'
            with zipfile.ZipFile(jar,'w') as archive:archive.writestr('blade-engine-build.properties',metadata)
            executed=[]
            def task(*args):
                executed.append(args[3]);jar.write_bytes(b'replaced package')
                return [{'status':'ok','message':'first completed'}],{'passed':True}
            with patch('qa_tools.plan',return_value=[('first','tests',None),('second','tests',None)]),patch('qa_tools.run_task',side_effect=task):
                checks,result=run_qa(root,{},'desktop',root/'out',jar=jar)
            self.assertEqual(result['engine_build_metadata'],metadata)
            self.assertEqual(executed,['first'])
            self.assertFalse(result['passed'])
            self.assertEqual(result['tasks'][0]['status'],'failed')
            self.assertIn('changed during QA',result['tasks'][1]['error'])


    def test_failure_retains_original_diagnostic_and_later_tasks_continue(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            def task(root,config,private,identifier,*args):
                folder=private/identifier;folder.mkdir();(folder/'error.log').write_text('original engine error')
                if identifier=='first':raise RuntimeFailure('original engine error')
                return [{'status':'ok','message':'passed'}],{'passed':True}
            with patch('qa_tools.plan',return_value=[('first','tests',None),('second','tests',None)]),patch('qa_tools.run_task',side_effect=task):
                checks,result=run_qa(root,{},'fast',root/'out')
            self.assertFalse(result['passed']);self.assertEqual(result['tasks'][0]['error'],'original engine error')
            self.assertEqual(result['tasks'][1]['status'],'passed')
            self.assertEqual((root/'out/artifacts/first/error.log').read_text(),'original engine error')

    def test_allow_unavailable_never_claims_complete_or_masks_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            with patch('qa_tools.plan',return_value=[('one','tests',None)]),patch('qa_tools.run_task',return_value=([{'status':'warning','message':'GL unavailable'}],{'qa_unavailable':True})):
                checks,result=run_qa(root,{},'fast',root/'allowed',allow_unavailable=True)
                self.assertTrue(result['passed']);self.assertFalse(result['complete']);self.assertEqual(checks[0]['status'],'warning')
                checks,result=run_qa(root,{},'fast',root/'required')
                self.assertFalse(result['passed']);self.assertEqual(checks[0]['status'],'error')
            with patch('qa_tools.plan',return_value=[('one','tests',None)]),patch('qa_tools.run_task',return_value=([{'status':'error','message':'actual failure'}],{'qa_unavailable':True,'passed':False})):
                checks,result=run_qa(root,{},'fast',root/'failed',allow_unavailable=True)
                self.assertFalse(result['passed'])


if __name__=='__main__':unittest.main()

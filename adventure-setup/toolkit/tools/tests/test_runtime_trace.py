"""Trace normalization omits volatile identities but never erases semantic differences."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure, save_json
from runtime_trace import compare_traces, normalize


class TraceTests(unittest.TestCase):
    def make_run(self, root, name, value=False):
        folder=root/name;folder.mkdir()
        save_json(folder/'result.json',{'passed':True})
        save_json(folder/'manifest.json',{'setup':{'seed':1701,'scene':'bank'}})
        save_json(folder/'process.json',{'build':{'sha256':'same-build'}})
        save_json(folder/'event-recording.json',{'errors':[],'thread_stopped':True})
        events=[{'type':'ink.external.result','frame':12,'command_id':name,'data':{'call_id':name,'name':'inInventory','result':value}}]
        (folder/'events.jsonl').write_text('\n'.join(json.dumps(e) for e in events))
        return folder

    def test_volatile_ids_omitted_but_typed_results_and_order_remain(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);a=self.make_run(root,'a');b=self.make_run(root,'b')
            checks,result=compare_traces(root,a,b,root/'equal')
            self.assertTrue(result['passed'])
            c=self.make_run(root,'c',0)
            checks,result=compare_traces(root,a,c,root/'different')
            self.assertFalse(result['passed']);self.assertEqual(checks[0]['status'],'error')
            self.assertEqual(result['first_difference']['index'],0)
            self.assertTrue((root/'different/reference-trace.json').is_file())

    def test_different_build_or_incomplete_recording_cannot_certify_equality(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);a=self.make_run(root,'a');b=self.make_run(root,'b')
            save_json(b/'process.json',{'build':{'sha256':'different'}})
            with self.assertRaisesRegex(RuntimeFailure,'identical declared setup'):compare_traces(root,a,b,root/'out')
            self.assertFalse((root/'out').exists())
            save_json(b/'process.json',{'build':{'sha256':'same-build'}})
            save_json(b/'event-recording.json',{'errors':['event gap'],'thread_stopped':True})
            with self.assertRaisesRegex(RuntimeFailure,'Incomplete event'):compare_traces(root,a,b,root/'out')

    def test_model_pointer_and_ink_path_are_not_discarded(self):
        one={'type':'action.started','data':{'runner_id':'a','source_ref':{'source':'model#pointer','action_pointer':'/actions/0'},'action':'Goto'}}
        two={'type':'action.started','data':{'runner_id':'b','source_ref':{'source':'model#pointer','action_pointer':'/actions/1'},'action':'Goto'}}
        self.assertNotEqual(normalize([one]),normalize([two]))


if __name__=='__main__':unittest.main()

"""Declarative scenario safety and meaningful state predicate behavior."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch, MagicMock
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure
from runtime_scenario import matches, run_scenario, validate


def example():
    return {'schema_version':1,'id':'sample','setup':{'chapter':'00','scene':'bank','seed':7,'locale':'es','viewport':[960,540]},
            'steps':[{'id':'first','type':'assert','condition':{'path':'/paused','equals':False}}]}


class ScenarioTests(unittest.TestCase):
    def test_invalid_or_ambiguous_scenarios_rejected_before_launch(self):
        bad=[]
        for key,value in [('schema_version',True),('schema_version',2),('id','../escape'),('steps',[])]:
            s=example();s[key]=value;bad.append(s)
        s=example();s['steps']*=2;bad.append(s)
        s=example();s['setup']['seed']=True;bad.append(s)
        s=example();s['setup']['viewport']=[2000000,540];bad.append(s)
        s=example();s['steps'][0]['condition']={'path':'/missing','equals':None,'truthy':False};bad.append(s)
        s=example();s['steps']=[{'id':'click','type':'click','name':'menu.continue','actor':'Joe'}];bad.append(s)
        s=example();s['steps']=[{'id':'wait','type':'wait','timeout_ms':-1,'condition':{'path':'/x','equals':1}}];bad.append(s)
        for scenario in bad:
            with self.subTest(scenario=scenario),self.assertRaises(RuntimeFailure):validate(scenario)

    def test_capture_names_cannot_overwrite_result_or_session_evidence(self):
        for name in ['result','connection','setup','final','manifest']:
            scenario=example();scenario['steps']=[{'id':name,'type':'capture'}]
            with self.subTest(name=name),self.assertRaises(RuntimeFailure):validate(scenario)

    def test_types_and_missing_values_are_not_conflated(self):
        self.assertFalse(matches({}, {'path':'/missing','equals':None}))
        self.assertFalse(matches({}, {'path':'/missing','truthy':False}))
        self.assertFalse(matches({}, {'path':'/missing','not_equals':17}))
        self.assertTrue(matches({'value':None}, {'path':'/value','equals':None}))
        self.assertFalse(matches({'value':1}, {'path':'/value','equals':True}))
        self.assertTrue(matches({'value':False}, {'path':'/value','equals':False}))
        self.assertFalse(matches({'value':{'nested':[1]}}, {'path':'/value','equals':{'nested':[True]}}))
        self.assertFalse(matches({'value':[{'flag':0}]}, {'path':'/value','contains':{'flag':False}}))

    def test_queries_resolve_actual_collections_and_escaped_keys(self):
        state={'items':[{'id':'gum'}],'a/b':{'~x':'hello'}}
        self.assertTrue(matches(state,{'path':'/items','has_id':'gum'}))
        self.assertFalse(matches(state,{'path':'/items','has_id':'spray'}))
        self.assertTrue(matches(state,{'path':'/a~1b/~0x','contains':'ell'}))
        self.assertFalse(matches(state,{'path':'/items/-1/id','equals':'gum'}))

    def test_composite_conditions_require_all_or_any_and_bounded_depth(self):
        condition={'all':[{'path':'/paused','equals':False},{'any':[{'path':'/text','equals':None},{'path':'/choices','truthy':True}]}]}
        scenario=example();scenario['steps'][0]['condition']=condition;validate(scenario)
        self.assertTrue(matches({'paused':False,'text':None,'choices':[]},condition))
        self.assertFalse(matches({'paused':False,'text':'hello','choices':[]},condition))
        self.assertFalse(matches({'paused':False,'choices':[]},condition))
        for bad in ({'all':[]},{'any':[{}]},{'all':[condition]*33}):
            scenario['steps'][0]['condition']=bad
            with self.assertRaises(RuntimeFailure):validate(scenario)
        for _ in range(6):condition={'all':[condition]}
        scenario['steps'][0]['condition']=condition
        with self.assertRaises(RuntimeFailure):validate(scenario)

    def test_invalid_step_does_not_start_or_destroy_existing_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp);source=path/'scenario.json';out=path/'out';out.mkdir();(out/'original').write_text('keep')
            source.write_text(json.dumps(example()))
            with patch('runtime_scenario.start_runtime') as start,patch('runtime_scenario.stop_runtime') as stop:
                with self.assertRaises(RuntimeFailure):run_scenario(path,{},source,out)
                start.assert_not_called();stop.assert_not_called()
            self.assertEqual((out/'original').read_text(),'keep')

    def test_event_recorder_file_failure_is_reported(self):
        from runtime_scenarios import EventRecorder
        with tempfile.TemporaryDirectory() as tmp:
            out=Path(tmp);(out/'events.jsonl').mkdir()
            recorder=EventRecorder(MagicMock(),out);recorder.close()
            self.assertTrue(recorder.failures)
            self.assertFalse(recorder.thread.is_alive())
            self.assertTrue(json.loads((out/'event-recording.json').read_text())['errors'])

    def test_secondary_recorder_failure_preserves_primary_scenario_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);out=root/'out';source=root/'scenario.json';source.write_text(json.dumps(example()))
            client=MagicMock();client.debug.return_value={'capabilities':[]};client.evidence.return_value={'errors':[]};client.observation_errors=[]
            recorder=MagicMock();recorder.close.side_effect=OSError('secondary recording failure')
            def start(*args,**kwargs):out.mkdir();return client,out
            with patch('runtime_scenario.start_runtime',side_effect=start),patch('runtime_scenario.EventRecorder',return_value=recorder):
                checks,result=run_scenario(root,{},source,out)
            self.assertFalse(result['passed'])
            self.assertIn('Required scenario capabilities unavailable',result['error'])
            self.assertTrue(any('secondary recording failure' in str(v) for v in result['evidence_errors']))

    def test_startup_failure_without_output_does_not_hide_original_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp);source=path/'scenario.json';source.write_text(json.dumps(example()))
            with patch('runtime_scenario.start_runtime',side_effect=RuntimeFailure('original launch failure')):
                checks,result=run_scenario(path,{},source,path/'missing')
            self.assertFalse(result['passed']);self.assertEqual(result['error'],'original launch failure')
            self.assertEqual(checks[0]['status'],'error')


if __name__=='__main__':unittest.main()

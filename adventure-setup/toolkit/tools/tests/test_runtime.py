"""Client/runner safety without a graphics context or real process termination."""
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch, MagicMock
from urllib.error import HTTPError

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeClient, RuntimeFailure, stop_runtime


class Response(io.BytesIO):
    def __enter__(self):return self
    def __exit__(self,*args):self.close()


class ClientTests(unittest.TestCase):
    def setUp(self):
        self.info={'protocol':1,'url':'http://127.0.0.1:12345','session_id':'test-session','mode':'debug','token':'private-token'}
    def health(self):return {'session_id':'test-session','mode':'debug'}
    def transport(self,data):return Response(json.dumps(data).encode())
    def test_remote_addresses_and_missing_identity_rejected(self):
        for value in ['http://example.com:8080','https://127.0.0.1:8080','http://localhost:8080']:
            with self.assertRaises(RuntimeFailure):RuntimeClient({**self.info,'url':value})
        with self.assertRaises(RuntimeFailure):RuntimeClient({})
        with self.assertRaises(RuntimeFailure):RuntimeClient({**self.info,'protocol':99})
    def test_session_change_prevents_mutation(self):
        with patch('runtime_client.urlopen',side_effect=[self.transport(self.health()),self.transport({'session_id':'other','mode':'debug'})]) as opened:
            client=RuntimeClient(self.info)
            with self.assertRaisesRegex(RuntimeFailure,'session/mode changed'):client.command('fixture.variable',{'name':'x'})
            self.assertEqual(opened.call_count,2)
    def test_debug_operation_rejected_in_player_mode(self):
        with patch('runtime_client.urlopen',return_value=self.transport({'session_id':'test-session','mode':'player'})):
            client=RuntimeClient({**self.info,'mode':'player'})
        with self.assertRaisesRegex(RuntimeFailure,'debug session'):client.command('fixture.scene')
    def test_async_result_and_private_auth_header(self):
        health=self.health();accepted={'command_id':'command','status':'accepted'};completed={'command_id':'command','status':'completed','result':{'ok':True}}
        with patch('runtime_client.urlopen',side_effect=[self.transport(health),self.transport(health),self.transport(accepted),self.transport(health),self.transport(completed)]) as opened:
            client=RuntimeClient(self.info);result=client.command('wait',{'conditions':[{'path':'/x','equals':True}]})
            self.assertTrue(result['result']['ok'])
            request=opened.call_args_list[2].args[0]
            self.assertEqual(request.get_header('Authorization'),'Bearer private-token')
            self.assertEqual(json.loads(request.data)['operation'],'wait')
    def test_failed_command_is_not_silently_retried(self):
        failed={'command_id':'command','status':'failed','error':{'code':'bad_target'}}
        with patch('runtime_client.urlopen',side_effect=[self.transport(self.health()),self.transport(self.health()),self.transport(failed)]) as opened:
            client=RuntimeClient(self.info)
            with self.assertRaisesRegex(RuntimeFailure,'bad_target'):client.command('verb.run',{'actor':'absent'})
            self.assertEqual(opened.call_count,3)
    def test_capture_hash_failure_preserves_existing_files(self):
        with patch('runtime_client.urlopen',return_value=self.transport(self.health())):client=RuntimeClient(self.info)
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'capture.png';path.write_bytes(b'original')
            with patch.object(client,'command',return_value={'result':{'artifact_id':'id','sha256':'wrong'}}),patch.object(client,'debug',return_value=b'bad'):
                with self.assertRaisesRegex(RuntimeFailure,'hash mismatch'):client.capture(temporary)
            self.assertEqual(path.read_bytes(),b'original')
    def test_evidence_records_unavailable_fields_without_losing_prior_state(self):
        with patch('runtime_client.urlopen',return_value=self.transport(self.health())):client=RuntimeClient(self.info)
        with tempfile.TemporaryDirectory() as temporary:
            prior=Path(temporary)/'last-state.json';prior.write_text('previous good state')
            with patch.object(client,'state',side_effect=RuntimeFailure('dead')),patch.object(client,'events',side_effect=RuntimeFailure('dead')),patch.object(client,'capture',side_effect=RuntimeFailure('dead')):
                result=client.evidence(temporary,'failure')
            self.assertEqual(len(result['errors']),3);self.assertEqual(prior.read_text(),'previous good state')
    def test_tokens_not_in_reported_http_errors(self):
        health=self.transport(self.health())
        error=HTTPError(self.info['url'],401,'Unauthorized',{},io.BytesIO(b'{"error":"unauthorized"}'))
        with patch('runtime_client.urlopen',side_effect=[health,self.transport(self.health()),error]):
            client=RuntimeClient(self.info)
            with self.assertRaises(RuntimeFailure) as caught:client.debug('/state')
        self.assertNotIn('private-token',str(caught.exception))
    def test_private_discovery_permissions(self):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'connection.json';path.write_text(json.dumps(self.info));path.chmod(0o644)
            with self.assertRaisesRegex(RuntimeFailure,'private'):RuntimeClient.connect(path)
    def test_process_pid_reuse_prevents_termination(self):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'process.json';path.write_text(json.dumps({'pid':100,'start_identity':'before','owned':True}))
            with patch('runtime_client.process_identity',return_value='after'),patch('runtime_client.os.killpg') as kill:
                with self.assertRaisesRegex(RuntimeFailure,'ownership'):stop_runtime(temporary)
                kill.assert_not_called()
    def test_missing_process_is_already_stopped(self):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'process.json';path.write_text(json.dumps({'pid':100,'start_identity':'before','owned':True}))
            with patch('runtime_client.process_identity',return_value=None),patch('runtime_client.os.killpg') as kill:
                self.assertEqual(stop_runtime(temporary)['status'],'already_stopped');kill.assert_not_called()


class ScenarioEvidenceTests(unittest.TestCase):
    def test_existing_evidence_and_process_are_left_untouched(self):
        from runtime_scenario import run_scenario
        from test_runtime_scenario import example
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);out=root/'existing';out.mkdir()
            (out/'result.json').write_text('original');(out/'process.json').write_text('existing session')
            source=root/'scenario.json';source.write_text(json.dumps(example()))
            with patch('runtime_scenario.start_runtime') as start,patch('runtime_scenario.stop_runtime') as stop:
                with self.assertRaisesRegex(RuntimeFailure,'new directory'):run_scenario(root,{},source,out)
                start.assert_not_called();stop.assert_not_called()
            self.assertEqual((out/'result.json').read_text(),'original')

    def test_incomplete_evidence_cannot_report_success(self):
        from runtime_scenario import run_scenario
        from test_runtime_scenario import example
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);out=root/'new';source=root/'scenario.json';source.write_text(json.dumps(example()))
            client=MagicMock();client.debug.return_value={'capabilities':['input.events','state.ui','fixture.seed','fixture.locale','fixture.viewport']}
            client.state.return_value={'paused':False,'scene':{'id':'room'},'chapter':'00','frame':1,'event_cursor':0}
            client.evidence.return_value={'errors':[{'artifact':'capture','error':'unavailable'}]};client.observation_errors=[]
            def start(*args,**kwargs):out.mkdir();return client,out
            recorder=MagicMock();recorder.failures=[];recorder.thread.is_alive.return_value=False
            with patch('runtime_scenario.start_runtime',side_effect=start),patch('runtime_scenario.EventRecorder',return_value=recorder):
                checks,result=run_scenario(root,{},source,out)
            self.assertFalse(result['passed']);self.assertEqual(checks[0]['status'],'error')
            self.assertIn('Evidence/cleanup incomplete',result['error'])
            self.assertEqual(result['evidence_errors'][0]['artifact'],'capture')


if __name__=='__main__':unittest.main()

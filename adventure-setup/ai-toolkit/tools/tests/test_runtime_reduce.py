"""Reduction must retain the same failure, not just any failing subprocess."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure, save_json
from runtime_reduce import failure_signature, minimize, reduce_scenario


class ReductionTests(unittest.TestCase):
    def test_interacting_steps_keep_order_and_remove_irrelevant_actions(self):
        observed = []
        def oracle(items):
            observed.append(tuple(items))
            return 2 in items and 7 in items and items.index(2) < items.index(7)
        reduced, stats = minimize(list(range(10)), oracle, 128)
        self.assertEqual(reduced, [2, 7])
        self.assertTrue(stats['single_step_minimal'])
        self.assertFalse(stats['global_minimum_proven'])
        self.assertEqual(len(observed), len(set(observed)))

    def test_budget_never_claims_minimality(self):
        calls = []
        reduced, stats = minimize(list(range(8)), lambda items: calls.append(items) or False, 1)
        self.assertEqual(len(calls), 1)
        self.assertEqual(reduced, list(range(8)))
        self.assertTrue(stats['budget_exhausted'])
        self.assertFalse(stats['single_step_minimal'])
        with patch('runtime_reduce.time.monotonic', return_value=9):
            _, stats = minimize([1, 2], lambda items: self.fail('Expired budget ran a trial'), deadline=8)
        self.assertEqual(stats['trials'], 0)

    def test_nonmonotonic_failure_does_not_imply_global_minimum(self):
        reduced, stats = minimize([0, 1, 2, 3], lambda items: items in ([0, 1], [2]), 128)
        self.assertIn(reduced, ([0, 1], [2]))
        self.assertFalse(stats['global_minimum_proven'])

    def test_signature_rejects_pass_setup_timeout_and_incomplete_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp)
            result = {'passed': False, 'failed_step': 'fault', 'evidence_errors': []}
            job = {'status': 'failed', 'progress': {'step_id': 'fault'},
                   'error': {'code': 'command_failed', 'message': 'original'}}
            save_json(out / 'engine-job.json', job)
            save_json(out / 'failure-state.json', {'diagnostics': {'last_error': 'original cause'}})
            signature = failure_signature(result, out)
            self.assertEqual(signature['message'], 'original')
            save_json(out / 'failure-state.json', {'diagnostics': {'last_error': 'different cause'}})
            self.assertNotEqual(signature, failure_signature(result, out))
            (out / 'failure-state.json').unlink()
            self.assertIsNone(failure_signature(result, out))
            save_json(out / 'failure-state.json', {'diagnostics': {}})
            self.assertIsNone(failure_signature(dict(result, passed=True), out))
            self.assertIsNone(failure_signature(dict(result, evidence_errors=['capture failed']), out))
            self.assertIsNone(failure_signature(dict(result, failed_step='other'), out))
            for status in ('running', 'cancelled', 'timed_out'):
                save_json(out / 'engine-job.json', dict(job, status=status))
                self.assertIsNone(failure_signature(result, out))

    def test_final_divergence_never_publishes_reproducer(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); jar = root / 'game.jar'; jar.write_bytes(b'pinned')
            source = root / 'source.json'
            spec = {'schema_version': 1, 'id': 'failure', 'setup': {'chapter': '00', 'scene': 'bank',
                    'seed': 7, 'locale': 'es', 'viewport': [960, 540]},
                    'steps': [{'id': 'fault', 'type': 'assert', 'condition': {'path': '/paused', 'equals': True}}]}
            save_json(source, spec)
            calls = []
            def run(root, config, path, out, jar, engine_replay):
                out.mkdir(); calls.append(out.name)
                save_json(out / 'engine-job.json', {'status': 'failed', 'progress': {'step_id': 'fault'},
                    'error': {'code': 'command_failed', 'message': 'original' if len(calls) == 1 else 'different'}})
                save_json(out / 'failure-state.json', {'diagnostics': {}})
                return [], {'passed': False, 'failed_step': 'fault', 'error': 'failed', 'evidence_errors': []}
            with patch('runtime_reduce.run_scenario', side_effect=run):
                checks, result = reduce_scenario(root, {}, source, root / 'output', jar)
            self.assertEqual(calls, ['baseline', 'confirmation'])
            self.assertFalse(result['confirmed']); self.assertEqual(checks[0]['status'], 'error')
            self.assertFalse((root / 'output/reduced-scenario.json').exists())
            self.assertEqual(len(json.loads((root / 'output/trials.json').read_text())), 2)


if __name__ == '__main__':
    unittest.main()

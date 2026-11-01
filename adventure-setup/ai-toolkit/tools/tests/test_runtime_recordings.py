"""Legacy migration requires explicit intent rather than blindly replaying delays."""
from copy import deepcopy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure, save_json
from runtime_recordings import assess_entries, migrate_recording


class RecordingTests(unittest.TestCase):
    def setup_fixture(self, root):
        (root / 'assets/model').mkdir(parents=True)
        save_json(root / 'assets/model/00.chapter.json', {'scenes': {'bank': {'actors': {'Joe': {}}}}})
        (root / 'assets/tests').mkdir()
        source = root / 'assets/tests/route.verbs.rec'; source.write_text('[{verb: talkto, actorId: Joe}]')
        entries = [{'index': 0, 'kind': 'verb', 'actor': 'Joe', 'verb': 'talkto', 'target': None, 'legacy_wait_seconds': 2},
                   {'index': 1, 'kind': 'choice', 'legacy_option': 2, 'legacy_wait_seconds': .5}]
        spec = {'schema_version': 1, 'id': 'migrated', 'source': str(source),
                'setup': {'chapter': '00', 'scene': 'bank', 'seed': 7, 'locale': 'es', 'viewport': [960, 540]},
                'selected': [{'index': 0, 'id': 'talk', 'before': {'path': '/cutMode', 'equals': False},
                              'after': {'path': '/choices', 'truthy': True}},
                             {'index': 1, 'id': 'leave', 'choice_text': 'Adiós',
                              'before': {'path': '/choices', 'truthy': True},
                              'after': {'path': '/choices', 'equals': []}}]}
        return source, entries, spec

    def test_audit_reports_real_missing_actors_and_no_checkpoint_without_execution(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); source, entries, spec = self.setup_fixture(root)
            entries += [{'index': 2, 'kind': 'verb', 'actor': 'Absent', 'legacy_wait_seconds': 0}]
            result = assess_entries(root, source, entries)
            self.assertEqual(result['missing_actor_ids'], ['Absent'])
            self.assertEqual(result['index_only_choice_steps'], [1])
            self.assertIsNone(result['packaged_entry_checkpoint'])
            self.assertFalse(result['automatic_replay_approved'])
            self.assertEqual(result['compatibility'], 'requires_migration')

    def test_migration_replaces_delays_and_index_choices_with_explicit_conditions(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); source, entries, spec = self.setup_fixture(root); before = source.read_bytes()
            with patch('runtime_recordings.decode_recording', return_value=entries):
                migrate_recording(root, {}, spec, root / 'output')
            scenario = json.loads((root / 'output/scenario.json').read_text())
            steps = scenario['steps']
            self.assertEqual(len(steps), 10)
            self.assertEqual(steps[1]['operation'], 'gameplay')
            self.assertEqual(steps[1]['arguments'], {'type': 'actorVerb', 'actorId': 'Joe', 'verb': 'talkto'})
            self.assertEqual(steps[6], {'id': 'leave', 'type': 'choose', 'text': 'Adiós'})
            self.assertEqual(steps[3]['condition'], spec['selected'][0]['after'])
            self.assertEqual(source.read_bytes(), before)
            self.assertNotIn('legacy_option', json.dumps(scenario))

    def test_ambiguous_or_unsafe_migration_never_creates_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); source, entries, spec = self.setup_fixture(root)
            cases = []
            bad = deepcopy(spec); del bad['selected'][1]['choice_text']; cases.append(bad)
            bad = deepcopy(spec); bad['selected'].reverse(); cases.append(bad)
            bad = deepcopy(spec); bad['selected'][1]['id'] = 'talk'; cases.append(bad)
            bad = deepcopy(spec); bad['selected'][0]['after'] = {}; cases.append(bad)
            with patch('runtime_recordings.decode_recording', return_value=entries):
                for index, bad in enumerate(cases):
                    out = root / ('bad-%d' % index)
                    with self.assertRaises(RuntimeFailure): migrate_recording(root, {}, bad, out)
                    self.assertFalse(out.exists())
                entries[0] = {'index': 0, 'kind': 'save', 'target': '../../personal', 'legacy_wait_seconds': 0}
                with self.assertRaisesRegex(RuntimeFailure, 'Unsafe legacy save'):
                    migrate_recording(root, {}, spec, root / 'unsafe')
                self.assertFalse((root / 'unsafe').exists())


if __name__ == '__main__':
    unittest.main()

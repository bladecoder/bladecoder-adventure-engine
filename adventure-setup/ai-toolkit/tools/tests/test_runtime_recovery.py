"""Crash recovery must stay in the owned profile and never invent a capture."""
import hashlib
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure, save_json
from runtime_recovery import recover_captures

SESSION = '11111111-1111-1111-1111-111111111111'
ARTIFACT = '22222222-2222-2222-2222-222222222222'


class RecoveryTests(unittest.TestCase):
    def fixture(self, root):
        out = root / 'run'; out.mkdir(); profile = out / 'profile'; profile.mkdir()
        save = profile / '.BladeEngineAdventure'; directory = save / 'automation' / SESSION; directory.mkdir(parents=True)
        save_json(out / 'process.json', {'owned': True, 'profile': str(profile)})
        state = {'scenario_runs': [{'status': 'running', 'progress': {'id': 'route', 'step_id': 'last-good'}}], 'frame': 77}
        receipt = {'artifact_id': ARTIFACT, 'media_type': 'image/png', 'length': 3,
                   'sha256': hashlib.sha256(b'png').hexdigest(), 'state': state, 'frame': 77, 'event_cursor': 9}
        (directory / (ARTIFACT + '.png')).write_bytes(b'png'); save_json(directory / (ARTIFACT + '.json'), receipt)
        return out, {'session_id': SESSION, 'save_directory': str(save)}, {'id': 'route', 'steps': [{'id': 'last-good', 'type': 'capture'}]}, directory

    def test_exact_session_receipt_recovers_hash_and_correlated_state(self):
        with tempfile.TemporaryDirectory() as tmp:
            out, session, scenario, directory = self.fixture(Path(tmp))
            report = recover_captures(out, session, scenario)
            self.assertEqual(report['errors'], []); self.assertEqual(report['recovered'][0]['frame'], 77)
            self.assertEqual((out / 'last-good.png').read_bytes(), b'png')
            self.assertTrue((out / 'last-good-state.json').is_file())
            self.assertEqual(recover_captures(out, session, scenario)['recovered'], [])

    def test_personal_save_or_unowned_process_is_rejected_before_read(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); out, session, scenario, directory = self.fixture(root)
            with self.assertRaisesRegex(RuntimeFailure, 'outside'): recover_captures(out, dict(session, save_directory=str(root / 'personal')), scenario)
            save_json(out / 'process.json', {'owned': False, 'profile': str(out / 'profile')})
            with self.assertRaisesRegex(RuntimeFailure, 'owned isolated'): recover_captures(out, session, scenario)
            self.assertFalse((out / 'last-good.png').exists())

    def test_tampered_or_symlinked_artifact_does_not_overwrite_prior_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); out, session, scenario, directory = self.fixture(root)
            png = directory / (ARTIFACT + '.png'); png.write_bytes(b'bad')
            report = recover_captures(out, session, scenario)
            self.assertIn('hash/length', report['errors'][0]); self.assertFalse((out / 'last-good.png').exists())
            png.unlink(); personal = root / 'personal.png'; personal.write_bytes(b'png'); png.symlink_to(personal)
            report = recover_captures(out, session, scenario)
            self.assertIn('unsafe', report['errors'][0]); self.assertFalse((out / 'last-good.png').exists())
            png.unlink(); png.write_bytes(b'png'); (out / 'last-good.png').write_bytes(b'prior')
            report = recover_captures(out, session, scenario)
            self.assertIn('overwrite', report['errors'][0]); self.assertEqual((out / 'last-good.png').read_bytes(), b'prior')


if __name__ == '__main__': unittest.main()

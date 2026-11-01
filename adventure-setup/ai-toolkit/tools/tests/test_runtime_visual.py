"""Reviewed thresholds, provenance and explicit incompatible-environment reporting."""
from copy import deepcopy
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from runtime_client import RuntimeFailure, save_json
from runtime_visual import accept_baseline, compare_matrix, compare_pixels, sha, thresholds

LIMITS = {'channel_delta': 12, 'max_changed_ratio': .001, 'max_mean_error': .1, 'masks': []}


class ReviewTests(unittest.TestCase):
    def test_unsafe_or_nonfinite_thresholds_rejected(self):
        cases = []
        for key, value in [('channel_delta', True), ('channel_delta', -1), ('max_mean_error', float('nan')), ('max_changed_ratio', 2)]:
            bad = deepcopy(LIMITS); bad[key] = value; cases.append(bad)
        bad = deepcopy(LIMITS); bad['masks'] = [{'x': 0, 'y': 0, 'width': 1, 'height': 1, 'reason': ''}]; cases.append(bad)
        for bad in cases:
            with self.assertRaises(RuntimeFailure): thresholds(bad)

    def test_unreviewed_failed_tampered_or_escaping_captures_cannot_be_accepted(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); source = root / 'evidence'; source.mkdir(); png = source / 'image.png'; png.write_bytes(b'original')
            report = {'passed': True, 'errors': [], 'thresholds': LIMITS, 'captures': [{'id': 'image', 'file': 'image.png', 'sha256': sha(png)}]}
            filename = source / 'result.json'; save_json(filename, report)
            with self.assertRaisesRegex(RuntimeFailure, 'actual visual review'): accept_baseline(root, filename, root / 'out', None, 'review')
            self.assertFalse((root / 'out').exists())
            for bad in [dict(report, passed=False), dict(report, captures=[dict(report['captures'][0], sha256='changed')]),
                        dict(report, captures=[dict(report['captures'][0], file='../image.png')])]:
                save_json(filename, bad)
                with self.assertRaises(RuntimeFailure): accept_baseline(root, filename, root / 'out', 'reviewer', 'reviewed')
                self.assertFalse((root / 'out').exists())


@unittest.skipUnless(importlib.util.find_spec('PIL'), 'Optional Pillow environment required for actual pixel checks')
class PixelTests(unittest.TestCase):
    def fixture(self, root):
        from PIL import Image
        reference = root / 'reference.png'; candidate = root / 'candidate.png'
        Image.new('RGBA', (100, 100), (20, 40, 60, 255)).save(reference)
        Image.new('RGBA', (100, 100), (20, 40, 60, 255)).save(candidate)
        return reference, candidate

    def test_actual_changed_pixels_and_mean_error_cannot_hide_large_change(self):
        from PIL import Image
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); a, b = self.fixture(root)
            self.assertTrue(compare_pixels(a, b, LIMITS, root / 'same.png')['passed'])
            with Image.open(b) as image:
                image.paste((200, 40, 60, 255), (0, 0, 10, 10)); image.save(b)
            result = compare_pixels(a, b, LIMITS, root / 'diff.png')
            self.assertFalse(result['passed']); self.assertEqual(result['changed_pixels'], 100)
            self.assertEqual(result['changed_ratio'], .01); self.assertEqual(result['max_channel_delta'], 180)
            self.assertAlmostEqual(result['mean_channel_error'], .45)
            self.assertTrue((root / 'diff.png').is_file())

    def test_explicit_mask_applies_only_within_bounds_and_area_limit(self):
        from PIL import Image
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); a, b = self.fixture(root)
            with Image.open(b) as image:
                image.paste((255, 255, 255, 255), (0, 0, 10, 10)); image.save(b)
            limits = deepcopy(LIMITS); limits['masks'] = [{'x': 0, 'y': 0, 'width': 10, 'height': 10, 'reason': 'Test animated patch'}]
            result = compare_pixels(a, b, limits, root / 'masked.png')
            self.assertTrue(result['passed']); self.assertEqual(result['masked_pixels'], 100)
            limits['masks'][0]['width'] = 101
            with self.assertRaisesRegex(RuntimeFailure, 'outside'): compare_pixels(a, b, limits, root / 'outside.png')
            limits['masks'][0].update(width=100, height=100)
            with self.assertRaisesRegex(RuntimeFailure, '20%'): compare_pixels(a, b, limits, root / 'hidden.png')

    def test_candidate_cannot_loosen_reviewed_limits_and_environment_is_explicit(self):
        from PIL import Image
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp); evidence = root / 'evidence'; evidence.mkdir(); a, b = self.fixture(evidence)
            capture = {'id': 'screen', 'file': 'reference.png', 'sha256': sha(a), 'metadata': {'renderer': 'same'}}
            report = {'passed': True, 'errors': [], 'thresholds': LIMITS, 'captures': [capture]}
            save_json(evidence / 'result.json', report)
            accept_baseline(root, evidence / 'result.json', root / 'baseline', 'test-reviewer', 'Synthetic fixture reviewed')
            with Image.open(b) as image:
                image.paste((255, 0, 0, 255), (0, 0, 100, 100)); image.save(b)
            report['thresholds'] = {'channel_delta': 255, 'max_changed_ratio': 1, 'max_mean_error': 255, 'masks': []}
            report['captures'][0].update(file='candidate.png', sha256=sha(b))
            save_json(evidence / 'result.json', report)
            checks, result = compare_matrix(root, evidence / 'result.json', root / 'baseline', root / 'regression')
            self.assertFalse(result['passed']); self.assertEqual(checks[0]['status'], 'error')
            report['captures'][0]['metadata']['renderer'] = 'different'
            save_json(evidence / 'result.json', report)
            checks, result = compare_matrix(root, evidence / 'result.json', root / 'baseline', root / 'incompatible')
            self.assertFalse(result['passed']); self.assertEqual(checks[0]['status'], 'warning')
            self.assertEqual(result['captures'][0]['status'], 'incompatible_environment')


if __name__ == '__main__': unittest.main()

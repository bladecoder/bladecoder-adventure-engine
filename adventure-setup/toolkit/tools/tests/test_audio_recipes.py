"""Invalid authoring recipes must fail before replacing existing audio resources."""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from audio_tools import music, sfx
from asset_manifest import validate_manifests


class AudioRecipeSafety(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root/'raw/sounds').mkdir(parents=True)
        (self.root/'assets/sounds').mkdir(parents=True)
        self.target = self.root/'assets/sounds/click.wav'
        self.target.write_bytes(b'existing reviewed resource')
        self.recipe = {'schema_version': 1, 'provenance': 'Original DSP', 'sounds': {
            'click': {'seconds': .02, 'frequency': 600, 'sweep': 0, 'noise': 0, 'decay': 3, 'peak_dbfs': -9, 'seed': 2}}}

    def test_invalid_recipes_preserve_output_and_do_not_start_render(self):
        invalid = []
        for update in [{'schema_version': True}, {'schema_version': 2}, {'sounds': {}}, {'provenance': ''}, {'provenance': None}, {'sounds': []}]:
            invalid.append({**copy.deepcopy(self.recipe), **update})
        invalid.append({key:value for key,value in self.recipe.items() if key != 'provenance'})
        bad = copy.deepcopy(self.recipe);bad['sounds']['click']['seconds'] = True;invalid.append(bad)
        bad = copy.deepcopy(self.recipe);bad['sounds']['click']['freqency'] = 600;invalid.append(bad)
        for recipe in invalid:
            with self.subTest(recipe=recipe):
                (self.root/'raw/sounds/family.json').write_text(json.dumps(recipe))
                with patch('audio_tools.synthesize') as render, self.assertRaises(ValueError):
                    sfx(self.root, {}, 'family')
                render.assert_not_called()
                self.assertEqual(b'existing reviewed resource', self.target.read_bytes())
                self.assertFalse((self.root/'raw/sounds/family.verification.json').exists())

    def test_duplicate_header_keys_rejected(self):
        (self.root/'raw/sounds/family.json').write_text('{"schema_version":1,"schema_version":2,"provenance":"DSP","sounds":{}}')
        with self.assertRaisesRegex(ValueError, 'Duplicate'):
            sfx(self.root, {}, 'family')

    def test_sfx_generation_preserves_declared_channel_counts_and_pcm16(self):
        self.recipe['sounds']['signal'] = {**self.recipe['sounds']['click'], 'channels': 2, 'stereo_pan': [-.5, .5]}
        (self.root/'raw/sounds/family.json').write_text(json.dumps(self.recipe))
        (self.root/'tools').mkdir();(self.root/'tools/audio_tools.py').write_text('test generator identity')
        result = sfx(self.root, {}, 'family')
        self.assertEqual({'click','signal'}, set(result['outputs']))
        for name, channels in [('click',1),('signal',2)]:
            metrics = result['outputs'][name]['metrics']
            self.assertEqual(channels, metrics['channels'])
            self.assertEqual(16, metrics['bits_per_sample'])

    def test_invalid_music_header_never_imports_composition(self):
        folder = self.root/'raw/music/cue';folder.mkdir(parents=True)
        (folder/'render.json').write_text('{"schema_version":2}')
        with patch('audio_tools.importlib.util.spec_from_file_location') as load, self.assertRaises(ValueError):
            music(self.root, {}, 'cue')
        load.assert_not_called()

    def test_missing_svg_manifests_are_not_successful_coverage(self):
        checks = validate_manifests(self.root,{})
        self.assertEqual('warning', checks[0]['status'])
        self.assertIn('No SVG export manifests',checks[0]['message'])


if __name__ == '__main__':
    unittest.main()

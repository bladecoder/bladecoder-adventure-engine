"""Spine fixtures derive from explicit metadata, with no game character dependency."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from project_config import load_project
from spine_runtime import fixture_model, packaged_spine
from spine_tools import canonical_animation_defaults


class SpineRuntime(unittest.TestCase):
    def test_roundtrip_defaults_do_not_hide_changed_animation(self):
        explicit = {'wave': {'bones': {'arm': {'rotate': [{'time': 0, 'value': 0}, {'time': .5, 'value': 30}]}}}}
        omitted = {'wave': {'bones': {'arm': {'rotate': [{}, {'time': .5, 'value': 30}]}}}}
        changed = {'wave': {'bones': {'arm': {'rotate': [{}, {'time': .5, 'value': 31}]}}}}
        self.assertEqual(canonical_animation_defaults(explicit), canonical_animation_defaults(omitted))
        self.assertNotEqual(canonical_animation_defaults(explicit), canonical_animation_defaults(changed))
        self.assertEqual(explicit['wave']['bones']['arm']['rotate'][0], {'time': 0, 'value': 0})

    def test_fixture_has_only_declared_skeleton_and_animations(self):
        model = fixture_model('neutral-rig', ['idle', 'wave'], [1280, 720], '4.5.0-SNAPSHOT')
        self.assertEqual(set(model['scenes']), {'spine-preview'})
        actor = model['scenes']['spine-preview']['actors']['preview-character']
        self.assertEqual(actor['pos'], {'x': 640, 'y': 144})
        self.assertEqual(set(actor['renderer']['fanims']), {'idle', 'wave'})
        self.assertEqual(actor['renderer']['fanims']['idle']['source'], 'neutral-rig')
        self.assertNotIn('inkManager', model)

    def test_invalid_profiles_rejected_before_native_start(self):
        base = {'scene': 'room', 'actor': 'hero', 'source': 'rig', 'atlas': 'rig', 'editor_version': '4.3.23', 'animations': ['idle']}
        with tempfile.TemporaryDirectory() as directory:
            for change in ({'source': '../rig'}, {'animations': []}, {'animations': ['idle', 'idle']}, {'editor_version': 'latest'}, {'motion_bones': 'head'}):
                Path(directory, 'adventure.json').write_text(json.dumps({'schema_version': 1, 'tests': {'spine': {'neutral': {**base, **change}}}}))
                with self.subTest(change=change), self.assertRaises(ValueError): load_project(directory)

    def test_missing_profile_never_starts_game(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, 'adventure.json').write_text('{"schema_version":1}')
            with patch('spine_runtime.start_runtime') as start, self.assertRaises(ValueError):
                packaged_spine(directory, {}, Path(directory) / 'evidence', 'game.jar', 'absent')
            start.assert_not_called()


if __name__ == '__main__': unittest.main()

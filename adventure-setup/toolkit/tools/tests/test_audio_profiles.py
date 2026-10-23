"""Audio tests are explicit, portable and bounded before starting native processes."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from project_config import load_project
from audio_runtime import test_audio


class AudioProfiles(unittest.TestCase):
    def load(self, directory, profile):
        path = Path(directory) / 'adventure.json'
        path.write_text(json.dumps({'schema_version': 1, 'tests': {'audio': {'neutral': profile}}}))
        return load_project(directory)

    def test_malformed_probes_rejected(self):
        invalid = [
            {'scene': '00.room'},
            {'scene': '00.room', 'music': '../outside.mp3'},
            {'scene': '00.room', 'music': 'cue.ogg'},
            {'scene': '00.room', 'music': 'cue.mp3', 'capture_seconds': 3600},
            {'scene': '00.room', 'music': 'cue.mp3', 'min_music_wraps': True},
            {'scene': '00.room', 'sounds': [{'id': '../escape', 'resource': 'sound.wav'}]},
            {'scene': '00.room', 'sounds': [{'id': 'sound', 'resource': 'sound.mp3'}]},
        ]
        with tempfile.TemporaryDirectory() as directory:
            for profile in invalid:
                with self.subTest(profile=profile), self.assertRaises(ValueError): self.load(directory, profile)

    def test_unavailable_private_sink_fails_without_claiming_capture(self):
        with tempfile.TemporaryDirectory() as directory:
            self.load(directory, {'scene': '00.room', 'music': 'neutral.mp3'})
            with patch('audio_runtime.shutil.which', return_value='/available/ffmpeg'), patch('audio_runtime.isolated_sink', side_effect=ValueError('private sink unavailable')):
                result = test_audio(directory, {}, Path(directory) / 'evidence', cue='neutral')
            self.assertFalse(result['passed'])
            self.assertIn('private sink unavailable', result['error'])
            self.assertTrue(result['qa_unavailable'])
            self.assertNotIn('music', result)

    def test_missing_profile_never_starts_game(self):
        with tempfile.TemporaryDirectory() as directory:
            Path(directory, 'adventure.json').write_text('{"schema_version":1}')
            with patch('audio_runtime.start_runtime') as start, self.assertRaises(ValueError):
                test_audio(directory, {}, Path(directory) / 'evidence', cue='absent')
            start.assert_not_called()


if __name__ == '__main__': unittest.main()

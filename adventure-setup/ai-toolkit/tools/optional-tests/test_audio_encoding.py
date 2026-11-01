"""Optional actual FFmpeg import/encoding contracts; select explicitly for audio authoring."""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tests'))
import shutil
import tempfile
import unittest
from test_audio import sound_recipe
from audio_tools import synthesize, pcm_metrics, normalize_audio


class AudioEncodingTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which('ffmpeg') and shutil.which('ffprobe'),'FFmpeg/FFprobe required for real encoding')
    def test_stereo_sfx_survives_import_and_explicit_mono_conversion_is_available(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=root/'Stereo Signal.wav'
            params=sound_recipe()['sounds']['stereo-signal']
            self.assertEqual(params['channels'],2)
            synthesize(params,source)
            self.assertEqual(pcm_metrics(source)['bits_per_sample'],16)
            self.assertGreater(pcm_metrics(source)['stereo_nonidentical_frames'],100)
            original=source.read_bytes()
            result=normalize_audio(root,{},source,kind='sfx')
            self.assertEqual(result['metrics']['channels'],2)
            result=normalize_audio(root,{},source,kind='sfx',channel_mode='mono')
            self.assertEqual(result['metrics']['channels'],1)
            self.assertEqual(source.read_bytes(),original)
    @unittest.skipUnless(shutil.which('ffmpeg') and shutil.which('ffprobe'),'FFmpeg/FFprobe required for real encoding')
    def test_imports_enforce_mp3_music_and_wav_sfx_without_changing_source(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=root/'Original Input.wav'
            params=sound_recipe()['sounds']['click']
            synthesize(params,source);original=source.read_bytes()
            music_result=normalize_audio(root,{},source,kind='music')
            sfx_result=normalize_audio(root,{},source,kind='sfx')
            self.assertEqual(music_result['metrics']['codec'],'mp3')
            self.assertEqual(music_result['metrics']['channels'],2)
            self.assertEqual(sfx_result['metrics']['codec'],'pcm_s16le')
            self.assertEqual(sfx_result['metrics']['channels'],1)
            self.assertEqual(sfx_result['metrics']['sample_rate'],48000)
            self.assertEqual(source.read_bytes(),original)
            self.assertEqual(sorted(str(p.relative_to(root)) for p in root.rglob('*') if p.is_file()),
                             ['Original Input.wav','assets/music/original-input.mp3','assets/sounds/original-input.wav'])

if __name__=='__main__':unittest.main()

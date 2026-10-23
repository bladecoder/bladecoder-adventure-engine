"""Audio timing, deterministic sources, failure preservation and scratch policy."""
import copy
import array
import importlib.util
import json
import math
import os
import shutil
from pathlib import Path
import sys
import tempfile
import unittest
import wave
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from audio_tools import workspace, sfx, synthesize, sha, pcm_metrics, music, publish, program, normalize_music, normalize_audio, validate_formats
from runtime_client import start_runtime, RuntimeFailure
from music_score import validate, write_midi
from audio_match import match

ROOT=Path(__file__).resolve().parents[2]


def score():
    from music_score import track, add
    first = track('neutral first', 0, 0, 64, 30, 'test harmony')
    second = track('neutral second', 1, 10, 60, 90, 'test motif')
    add(first, 1, 1, 1, 'C4', 70);add(second, 2, 1, .5, 'G4', 65)
    return {'title':'Neutral contract fixture','meter':'4/4','loopBars':2,'bpm':120,
            'sampleRate':44100,'seconds':4,'loopFrames':176400,'tracks':[first,second]}


def sound_recipe():
    click = {'seconds':.15,'frequency':600,'sweep':-.2,'noise':.1,'decay':4,'seed':1701,'peak_dbfs':-9,'channels':1}
    return {'schema_version':1,'provenance':'Original synthetic test DSP','sounds':{
        'click':click,'click-soft':{**click,'frequency':500,'seed':1702},
        'stereo-signal':{**click,'channels':2,'stereo_pan':[-.7,.7]}}}


def music_recipe():
    return {'schema_version': 1, 'soundfont': 'TimGM6mb', 'soundfont_sha256': 'c5378b62028c920cb11e4803327983fee2f2cdff5dc89c708e39da417e51c854', 'target_peak_dbfs': -9, 'warmup_cycles': 2, 'output_cycle': 3, 'renderer_settings': {'sample_rate': 44100, 'gain': 0.45, 'reverb_room_size': 0.3, 'reverb_damp': 0.5, 'reverb_width': 55, 'reverb_level': 0.15, 'chorus': False}, 'provenance': 'Original neutral starter composition. External TimGM6mb SoundFont: 2004 Tim Brechbill / 2010 David Bolton; GPL-2 per installed package copyright. SoundFont is not distributed in the setup bundle.', 'format': 'mp3', 'mp3_bitrate': '192k'}


class AudioTests(unittest.TestCase):

    def test_project_audio_policy_rejects_8_and_24_bit_wav_and_ogg(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);sounds=root/'assets/sounds';sounds.mkdir(parents=True)
            music_dir=root/'assets/music';music_dir.mkdir()
            path=sounds/'effect.wav'
            for width in (1,3,4):
                with wave.open(str(path),'wb') as wav:
                    wav.setparams((1,width,48000,0,'NONE','not compressed'));wav.writeframes(bytes(width*100))
                self.assertEqual(validate_formats(root)[0]['status'],'error')
            with wave.open(str(path),'wb') as wav:
                wav.setparams((2,2,44100,0,'NONE','not compressed'));wav.writeframes(bytes(400))
            self.assertEqual(validate_formats(root)[0]['status'],'ok')
            (music_dir/'unsupported.ogg').write_bytes(b'not an approved runtime format')
            self.assertEqual(validate_formats(root)[0]['status'],'error')

    def test_waveform_matching_distinguishes_mixed_effect_from_background(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);reference=root/'reference.wav';capture=root/'capture.wav'
            params=sound_recipe()['sounds']['click']
            synthesize(params,reference)
            with wave.open(str(reference),'rb') as wav:
                template=array.array('h',wav.readframes(wav.getnframes()))
            data=array.array('h',(round(700*math.sin(i*.073)) for i in range(48000*6)))
            def save():
                with wave.open(str(capture),'wb') as wav:
                    wav.setparams((1,2,48000,0,'NONE','not compressed'));wav.writeframes(data.tobytes())
            save();self.assertFalse(match(capture,reference,3.3)['matched'])
            offset=48000*3+733
            for i,x in enumerate(template):data[offset+i]+=round(x*.45)
            save();result=match(capture,reference,3.3)
            self.assertTrue(result['matched'],result)
            self.assertLess(abs(result['capture_seconds']-offset/48000),.001)

    def test_capture_cannot_target_default_or_other_app_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);out=root/'run'
            for sink in ('default','alsa_output.monitor','bonasera_test_other','x;echo unsafe'):
                with self.subTest(sink=sink),self.assertRaises(RuntimeFailure):
                    start_runtime(root,{},out,audio_sink=sink)
            self.assertFalse(out.exists())

    def test_audio_paths_resolve_from_project_and_keep_bare_programs(self):
        self.assertEqual(program(Path('/project'),{'ffmpeg':'tools/bin/ffmpeg'},'ffmpeg'),'/project/tools/bin/ffmpeg')
        self.assertEqual(program(Path('/project'),{},'ffmpeg'),'ffmpeg')

    def test_normalization_input_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);source=root/'assets/music/example.mp3';source.parent.mkdir(parents=True)
            source.write_bytes(b'original')
            with self.assertRaises(ValueError):normalize_music(root,{},source)
            self.assertEqual(source.read_bytes(),b'original')

    def test_music_midi_preserves_all_notes_and_tempo(self):
        cue=score()
        self.assertEqual(validate(cue),2)
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'test.mid';write_midi(cue,path);data=path.read_bytes()
            self.assertTrue(data.startswith(b'MThd'))
            self.assertIn(b'\xff\x51\x03'+round(60_000_000/120).to_bytes(3,'big'),data)
            self.assertIn(b'LOOP_START',data);self.assertIn(b'LOOP_END',data)
            self.assertEqual(data.count(b'MTrk'),3)

    def test_bad_timing_velocity_and_nonfinite_notes_rejected(self):
        for key,value in [('bar',33),('dur',0),('vel',2),('beat',float('nan'))]:
            cue=score();cue['tracks'][0]['notes'][0][key]=value
            with self.subTest(key=key),self.assertRaises(ValueError):validate(cue)

    def test_duplicate_channels_and_same_pitch_overlap_rejected(self):
        cue=score();cue['tracks'][1]['channel']=0
        with self.assertRaises(ValueError):validate(cue)
        cue=score();cue['tracks'][0]['notes'].append(copy.deepcopy(cue['tracks'][0]['notes'][0]))
        with self.assertRaises(ValueError):validate(cue)

    def test_inconsistent_loop_length_rejected(self):
        cue=score();cue['loopFrames']+=1
        with self.assertRaises(ValueError):validate(cue)

    def test_temporary_music_ignores_repo_tmpdir_and_cleans_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            with patch.dict(os.environ,TMPDIR=str(root)):
                with self.assertRaisesRegex(ValueError,'induced'):
                    with workspace(root) as scratch:
                        self.assertFalse(scratch.is_relative_to(root))
                        (scratch/'large.wav').write_bytes(b'pcm')
                        raise ValueError('induced')
            self.assertFalse(scratch.exists())
            self.assertEqual(list(root.iterdir()),[])

    def test_keep_is_explicit_new_directory_and_survives_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);keep=root/'retained'
            with self.assertRaises(ValueError):
                with workspace(root,keep) as scratch:
                    (scratch/'failed.wav').write_bytes(b'original failure')
                    raise ValueError('induced')
            self.assertTrue((keep/'failed.wav').is_file())
            with self.assertRaises(FileExistsError):
                with workspace(root,keep):pass

    def test_keep_cannot_contaminate_sources_or_runtime_resources(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            for part in ('raw','assets/music','tools/intermediates','skills/demo'):
                with self.subTest(part=part),self.assertRaises(ValueError):
                    with workspace(root,root/part):pass

    def test_seeded_sfx_byte_reproducible_and_variant_differs(self):
        recipe=sound_recipe()['sounds']
        with tempfile.TemporaryDirectory() as directory:
            a,b=Path(directory)/'a.wav',Path(directory)/'b.wav'
            synthesize(recipe['click'],a);synthesize(recipe['click'],b)
            self.assertEqual(sha(a),sha(b))
            synthesize(recipe['click-soft'],b)
            self.assertNotEqual(sha(a),sha(b))
            metrics=pcm_metrics(a)
            self.assertEqual(metrics['channels'],1);self.assertEqual(metrics['sample_rate'],48000)
            self.assertEqual(metrics['clipped_samples'],0)
            self.assertLess(abs(metrics['peak_dbfs']+9),.02)

    def test_failed_family_preserves_existing_runtime_assets(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);(root/'raw/sounds').mkdir(parents=True)
            (root/'assets/sounds').mkdir(parents=True)
            existing=root/'assets/sounds/click.wav';existing.write_bytes(b'old')
            recipe=sound_recipe()
            recipe['sounds']['click']['seconds']=-1
            (root/'raw/sounds/test.json').write_text(json.dumps(recipe))
            with self.assertRaises(ValueError):sfx(root,{},'test')
            self.assertEqual(existing.read_bytes(),b'old')
            self.assertEqual(list((root/'assets/sounds').iterdir()),[existing])

    def test_missing_font_does_not_replace_music_or_create_intermediates(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);folder=root/'raw/music/test';folder.mkdir(parents=True)
            recipe=json.dumps(music_recipe())
            (folder/'render.json').write_text(recipe)
            final=root/'assets/music/test.mp3';final.parent.mkdir(parents=True);final.write_bytes(b'old')
            with self.assertRaises(ValueError):music(root,{'soundfont':'absent.sf2'},'test')
            self.assertEqual(final.read_bytes(),b'old')
            self.assertEqual(sorted(p.suffix for p in root.rglob('*') if p.is_file()),['.json','.mp3'])

    def test_atomic_copy_failure_keeps_original_file(self):
        with tempfile.TemporaryDirectory() as directory:
            target=Path(directory)/'music.mp3';target.write_bytes(b'original')
            with self.assertRaises(FileNotFoundError):publish(Path(directory)/'absent',target)
            self.assertEqual(target.read_bytes(),b'original')
            self.assertEqual(list(Path(directory).iterdir()),[target])


if __name__=='__main__':unittest.main()

"""Versioned audio sources, transient rendering, measured final resources."""
import array
from contextlib import contextmanager
import hashlib
import importlib.util
import json
import math
from pathlib import Path
import random
import re
import shutil
import subprocess
import sys
import tempfile
import wave

from music_score import validate, write_midi, render
from project_config import toolkit_path
from project_config import toolkit_path


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def dump(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False)+'\n')


def run(command):
    result = subprocess.run([str(p) for p in command], stdin=subprocess.DEVNULL,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=240)
    if result.returncode:
        raise ValueError('Audio subprocess failed: '+result.stderr.decode(errors='replace')[-1500:])
    return result


def program(root, config, key):
    value = config.get(key,key)
    path = Path(value).expanduser()
    if '/' in value or '\\' in value:
        return str(path if path.is_absolute() else root/path)
    return value


@contextmanager
def workspace(root, keep=None):
    """Ignore TMPDIR: it can point into the checkout. Clean up even on failure."""
    if keep:
        path = Path(keep).expanduser().resolve()
        # Explicit retention may use an ignored project folder, never runtime assets/sources.
        if any(path == root/p or (root/p) in path.parents for p in ('assets', 'raw', 'tools', 'skills', 'ai-toolkit', 'automation', 'docs')):
            raise ValueError('Retained intermediates must be separate from assets and source folders.')
        path.mkdir(parents=True, exist_ok=False)
        yield path
    else:
        base = Path('/tmp').resolve()
        if base == root or root in base.parents:
            raise ValueError('Temporary audio storage must be outside the repository.')
        with tempfile.TemporaryDirectory(prefix='adventure-audio-', dir=base) as directory:
            yield Path(directory)


def publish(source, target):
    """Copy checked output atomically, including when /tmp is another filesystem."""
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, name = tempfile.mkstemp(prefix='.'+target.name+'-', dir=target.parent)
    import os
    os.close(fd)
    try:
        shutil.copyfile(source, name)
        Path(name).replace(target)
    finally:
        Path(name).unlink(missing_ok=True)


def pcm_metrics(path):
    with wave.open(str(path), 'rb') as wav:
        if wav.getsampwidth() != 2 or wav.getcomptype() != 'NONE':
            raise ValueError('Metrics require PCM16.')
        rate, channels, frames = wav.getframerate(), wav.getnchannels(), wav.getnframes()
        data = array.array('h', wav.readframes(frames))
    if sys.byteorder != 'little':
        data.byteswap()
    if not data:
        raise ValueError('Empty audio.')
    peak = max(abs(x) for x in data)
    rms = math.sqrt(sum(x*x for x in data)/len(data))
    threshold = 32768 * 10**(-60/20)
    window = max(1, round(rate*.05))*channels
    longest = current = 0
    for i in range(0, len(data), window):
        if max(abs(x) for x in data[i:i+window]) < threshold:
            current += len(data[i:i+window])
            longest = max(longest, current)
        else:
            current = 0
    stereo_difference=sum(data[i]!=data[i+1] for i in range(0,len(data),2)) if channels==2 else None
    return dict(frames=frames, sample_rate=rate, channels=channels, seconds=frames/rate,
                bits_per_sample=16,
                stereo_nonidentical_frames=stereo_difference,
                peak_dbfs=20*math.log10(max(peak,1)/32768), rms_dbfs=20*math.log10(max(rms,1)/32768),
                clipped_samples=sum(abs(x) >= 32767 for x in data),
                longest_silence_seconds=longest/channels/rate,
                seam_delta=max(abs(data[-channels+c]-data[c]) for c in range(channels))/32768)


def analyze(path, scratch, ffmpeg, ffprobe, expected=None, loop=False):
    probe = json.loads(run([ffprobe,'-v','error','-show_streams','-show_format','-of','json',path]).stdout)
    streams = probe['streams']
    if len(streams) != 1 or streams[0]['codec_type'] != 'audio':
        raise ValueError('Expected exactly one audio stream.')
    decoded = scratch/'analysis.wav'
    run([ffmpeg,'-v','error','-y','-i',path,'-c:a','pcm_s16le',decoded])
    metrics = pcm_metrics(decoded)
    analysis_bits=metrics.pop('bits_per_sample')
    loudness = run([ffmpeg,'-hide_banner','-i',path,'-af','ebur128=peak=true','-f','null','-']).stderr.decode()
    summary = loudness[loudness.rfind('Summary:'):]
    for key, pattern in [('integrated_lufs',r'I:\s*([-\d.]+) LUFS'),('true_peak_dbfs',r'Peak:\s*([-\d.]+) dBFS')]:
        found = re.search(pattern, summary)
        if found:
            metrics[key] = float(found.group(1))
    if expected is not None and abs(metrics['seconds']-expected) > .03:
        raise ValueError('Unexpected audio duration: '+str(metrics['seconds']))
    if metrics['clipped_samples'] or metrics['peak_dbfs'] > -.5 or metrics['rms_dbfs'] < -55:
        raise ValueError('Clipped, insufficient headroom, or inaudible audio: '+str(metrics))
    if loop and (metrics['longest_silence_seconds'] > .25 or metrics['seam_delta'] > .03):
        raise ValueError('Loop has a long silence or discontinuity: '+str(metrics))
    return dict(codec=streams[0]['codec_name'], bits_per_sample=streams[0].get('bits_per_sample') or None,
                analysis_pcm_bits=analysis_bits, bit_rate=int(streams[0].get('bit_rate') or 0), **metrics)


def validate_formats(root):
    """Read-only project export policy; WAV headers establish actual PCM depth."""
    issues=[]
    count=0
    for directory,extension in [('music','.mp3'),('sounds','.wav')]:
        for path in sorted((root/'assets'/directory).rglob('*')):
            if not path.is_file() or path.suffix.lower() in ('.md','.txt','.json'):
                continue
            count+=1
            relative=str(path.relative_to(root))
            if path.suffix.lower()!=extension:
                issues.append(relative+': expected '+extension)
                continue
            if extension=='.wav':
                try:
                    with wave.open(str(path),'rb') as wav:
                        if (wav.getsampwidth()!=2 or wav.getcomptype()!='NONE'
                            or wav.getframerate() not in (44100,48000) or wav.getnchannels() not in (1,2)):
                            issues.append(relative+': expected PCM16 mono/stereo at 44.1 or 48 kHz')
                except (OSError,wave.Error,EOFError):
                    issues.append(relative+': unreadable PCM WAV header')
    return [{'id':'audio.formats','status':'error' if issues else 'ok',
             'message':'; '.join(issues) if issues else f'{count} audio resources follow MP3 music / PCM16 WAV SFX policy.'}]


def read_recipe(path, required):
    def unique(pairs):
        value = {}
        for key, item in pairs:
            if key in value:
                raise ValueError('Duplicate audio recipe key: '+key)
            value[key] = item
        return value
    data = json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique)
    fields = {'schema_version', 'provenance'} | set(required)
    if not isinstance(data, dict) or set(data) != fields:
        raise ValueError('Audio recipe must declare exactly: '+', '.join(sorted(fields)))
    if type(data['schema_version']) is not int or data['schema_version'] != 1:
        raise ValueError('Use audio recipe schema_version 1.')
    if not isinstance(data['provenance'], str) or not data['provenance'].strip():
        raise ValueError('Audio recipe requires nonempty provenance before publishing outputs.')
    return data


def music(root, config, cue, keep=None):
    if not re.fullmatch(r'[a-z][a-z0-9-]*', cue):
        raise ValueError('Invalid cue ID.')
    folder = root/'raw/music'/cue
    recipe = read_recipe(folder/'render.json', {'soundfont', 'soundfont_sha256', 'target_peak_dbfs', 'warmup_cycles', 'output_cycle', 'renderer_settings', 'format', 'mp3_bitrate'})
    values = recipe['renderer_settings']
    if (not isinstance(values, dict) or set(values) != {'sample_rate', 'gain', 'reverb_room_size', 'reverb_damp', 'reverb_width', 'reverb_level', 'chorus'}
        or type(values['sample_rate']) is not int or values['sample_rate'] not in (44100,48000)
        or not isinstance(recipe['soundfont'], str) or not recipe['soundfont'].strip()
        or not isinstance(recipe['soundfont_sha256'], str) or not re.fullmatch('[a-f0-9]{64}',recipe['soundfont_sha256'])
        or type(recipe['target_peak_dbfs']) not in (int,float) or not math.isfinite(recipe['target_peak_dbfs'])
        or type(recipe['warmup_cycles']) is not int or type(recipe['output_cycle']) is not int):
        raise ValueError('Invalid music recipe metadata or renderer settings.')
    for key, upper in [('gain', 2), ('reverb_room_size',1), ('reverb_damp',1), ('reverb_width',100), ('reverb_level',1)]:
        value = values[key]
        if type(value) not in (int,float) or not math.isfinite(value) or not 0 <= value <= upper:
            raise ValueError('Invalid renderer setting: '+key)
    if (type(values['chorus']) is not bool or recipe['warmup_cycles'] != 2 or recipe['output_cycle'] != 3
        or not -12 <= recipe['target_peak_dbfs'] <= -3 or recipe['format'] != 'mp3'
        or recipe['mp3_bitrate'] not in ('128k','160k','192k','256k','320k')):
        raise ValueError('Invalid rendering recipe.')
    sf = Path(config.get('soundfont','/usr/share/sounds/sf2/TimGM6mb.sf2')).expanduser()
    if not sf.is_absolute():
        sf = root/sf
    if not sf.is_file() or sha(sf) != recipe['soundfont_sha256']:
        raise ValueError('Missing or changed SoundFont; configure paths.soundfont with the pinned source.')
    spec = importlib.util.spec_from_file_location('adventure_cue_'+cue.replace('-','_'), folder/'compose.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    score = module.compose()
    count = validate(score)
    if values['sample_rate'] != score['sampleRate']:
        raise ValueError('Renderer and score sample rates differ.')
    ffmpeg, ffprobe = program(root,config,'ffmpeg'), program(root,config,'ffprobe')
    with workspace(root, keep) as scratch:
        dump(scratch/(cue+'.score.json'),score)
        write_midi(score,scratch/(cue+'.mid'))
        raw, master, final = scratch/'three-cycles.wav', scratch/'master.wav', scratch/(cue+'.mp3')
        library=config.get('fluidsynth_library')
        if library and ('/' in library or '\\' in library):
            library=Path(library).expanduser()
            library=str(library if library.is_absolute() else root/library)
        fluid_version = render(score,sf,raw,library,settings_values=values)
        metrics = pcm_metrics(raw)
        if metrics['clipped_samples'] or metrics['peak_dbfs'] < -55:
            raise ValueError('Clipped or silent raw synthesis; reduce synth gain before normalizing.')
        gain = recipe['target_peak_dbfs']-metrics['peak_dbfs']
        frames = score['loopFrames']
        run([ffmpeg,'-v','error','-y','-i',raw,'-af',
             f'atrim=start_sample={2*frames}:end_sample={3*frames},asetpts=PTS-STARTPTS,volume={gain:.8f}dB',
             '-c:a','pcm_s16le',master])
        run([ffmpeg,'-v','error','-y','-i',master,'-ac','2','-ar',str(score['sampleRate']),
             '-c:a','libmp3lame','-b:a',recipe['mp3_bitrate'],
             '-metadata','title='+score['title'],'-metadata','LOOPSTART=0','-metadata',f'LOOPEND={frames}',final])
        checked = analyze(final,scratch,ffmpeg,ffprobe,score['seconds'],loop=True)
        if keep:
            run([ffmpeg,'-v','error','-y','-stream_loop','1','-i',master,'-c:a','libmp3lame','-b:a','192k',scratch/'listening-two-loops.mp3'])
        report = dict(schema_version=1,cue=cue,notes=count,source_sha256=sha(folder/'compose.py'),
                      recipe_sha256=sha(folder/'render.json'),soundfont_sha256=sha(sf),
                      renderer_sha256=sha(toolkit_path(root, "music_score.py")),gain_db=gain,
                      pipeline_sha256=sha(toolkit_path(root, "audio_tools.py")),
                      fluidsynth_version=fluid_version,renderer_settings=values,
                      ffmpeg_version=run([ffmpeg,'-version']).stdout.decode().splitlines()[0],
                      output_sha256=sha(final),metrics=checked,
                      intermediate_policy='retained explicitly' if keep else 'temporary outside repository; removed on exit',
                      listening_review='Not evaluated by renderer; contextual review is recorded separately.')
        # Every output is checked before replacing the existing runtime resource.
        for suffix in ('.mid','.score.json'):
            publish(scratch/(cue+suffix),folder/(cue+suffix))
        publish(final,root/'assets/music'/(cue+'.mp3'))
        dump(folder/'verification.json',report)
    return report


def synthesize(params, target):
    """Seeded damped resonances share a soft wooden/metallic cartoon palette."""
    rate = 48000
    seconds, frequency = params['seconds'], params['frequency']
    channels=params.get('channels',1)
    pan_start,pan_end=params.get('stereo_pan',[0,0])
    rng = random.Random(params['seed'])
    samples = []
    phase = 0
    noise = 0
    for i in range(round(rate*seconds)):
        t = i/rate
        phase += 2*math.pi*frequency*(1+params['sweep']*t/seconds)/rate
        noise = .65*noise + .35*rng.uniform(-1,1)
        attack = min(1,t/.004)
        release = min(1,(seconds-t)/.012)
        env = attack*release*math.exp(-params['decay']*t/seconds)
        value = (math.sin(phase)+.28*math.sin(phase*2.7)+params['noise']*noise)*env
        if channels==2:
            pan=pan_start+(pan_end-pan_start)*t/seconds
            samples.extend((value*math.sqrt((1-pan)/2),value*math.sqrt((1+pan)/2)))
        else:
            samples.append(value)
    gain = 10**(params['peak_dbfs']/20)*32760/max(abs(x) for x in samples)
    pcm = array.array('h',(round(x*gain) for x in samples))
    if sys.byteorder != 'little':
        pcm.byteswap()
    with wave.open(str(target),'wb') as wav:
        wav.setparams((channels,2,rate,0,'NONE','not compressed'))
        wav.writeframes(pcm.tobytes())


def sfx(root, config, family, keep=None):
    if not re.fullmatch(r'[a-z][a-z0-9-]*',family):
        raise ValueError('Invalid SFX family ID.')
    recipe_path = root/'raw/sounds'/(family+'.json')
    recipe = read_recipe(recipe_path, {'sounds'})
    if not isinstance(recipe['sounds'], dict) or not 1 <= len(recipe['sounds']) <= 64:
        raise ValueError('SFX recipe requires one to 64 explicitly named sounds.')
    numeric = {'seconds', 'frequency', 'sweep', 'noise', 'decay', 'peak_dbfs'}
    required = numeric | {'seed'}
    for name, params in recipe['sounds'].items():
        if (not isinstance(params, dict) or not required <= set(params) or set(params) - required - {'channels','stereo_pan'}
            or any(type(params[key]) not in (int,float) or not math.isfinite(params[key]) for key in numeric)):
            raise ValueError('SFX recipe has missing, unsupported or untyped synthesis settings for '+name)
    outputs = {}
    with workspace(root,keep) as scratch:
        for name,params in recipe['sounds'].items():
            if not re.fullmatch(r'[a-z][a-z0-9-]*', name):
                raise ValueError('Invalid SFX ID.')
            if (not .02 <= params['seconds'] <= 5 or not 40 <= params['frequency'] <= 8000
                or not -1 < params['sweep'] <= 4 or not 0 <= params['noise'] <= 5
                or not 0 < params['decay'] <= 20 or not -24 <= params['peak_dbfs'] <= -3
                or type(params['seed']) is not int):
                raise ValueError('Invalid synthesis parameters for '+name)
            if type(params.get('channels',1)) is not int or params.get('channels',1) not in (1,2):
                raise ValueError('SFX channels must be one or two: '+name)
            pan=params.get('stereo_pan',[0,0])
            if (not isinstance(pan,list) or len(pan)!=2
                or any(type(v) not in (int,float) or not math.isfinite(v) or not -1<=v<=1 for v in pan)
                or (params.get('channels',1)==1 and pan!=[0,0])):
                raise ValueError('Stereo pan requires two channels and bounded start/end values: '+name)
            target = scratch/(name+'.wav')
            synthesize(params,target)
            outputs[name] = dict(sha256=sha(target),metrics=pcm_metrics(target))
        for name in outputs:
            publish(scratch/(name+'.wav'),root/'assets/sounds'/(name+'.wav'))
    result = dict(schema_version=1,family=family,recipe_sha256=sha(recipe_path),
                  generator_sha256=sha(toolkit_path(root, "audio_tools.py")),provenance=recipe['provenance'],outputs=outputs)
    dump(root/'raw/sounds'/(family+'.verification.json'),result)
    return result


def normalize_audio(root, config, source, keep=None, kind='music', channel_mode='source'):
    """MP3 music is stereo; PCM16 effects preserve mono/stereo unless selected."""
    source=Path(source).expanduser().resolve()
    if not source.is_file():raise ValueError('Audio input file does not exist.')
    name=re.sub(r'[^a-z0-9-]+','-',source.stem.lower()).strip('-')
    if not name:raise ValueError('Audio input requires a usable filename.')
    if kind not in ('music','sfx'):raise ValueError('Audio kind must be music or sfx.')
    if channel_mode not in ('source','mono','stereo'):raise ValueError('Invalid channel mode.')
    ffmpeg,ffprobe=program(root,config,'ffmpeg'),program(root,config,'ffprobe')
    suffix,rate,channels,peak = ('.mp3',44100,2,-3.5) if kind=='music' else ('.wav',48000,1,-9)
    if kind=='sfx':
        if channel_mode=='source':
            streams=json.loads(run([ffprobe,'-v','error','-select_streams','a:0',
                                    '-show_entries','stream=channels','-of','json',source]).stdout).get('streams',[])
            if not streams or streams[0].get('channels') not in (1,2):
                raise ValueError('Source must be mono/stereo, or explicitly choose --channels mono|stereo.')
            channels=streams[0]['channels']
        else:
            channels=1 if channel_mode=='mono' else 2
    codec = ['-c:a','libmp3lame','-b:a','192k'] if kind=='music' else ['-c:a','pcm_s16le']
    target=root/('assets/music' if kind=='music' else 'assets/sounds')/(name+suffix)
    if source==target.resolve():raise ValueError('Normalization requires a separate source file.')
    with workspace(root,keep) as scratch:
        raw=scratch/'input.wav';final=scratch/(name+suffix)
        run([ffmpeg,'-v','error','-y','-i',source,'-ar',str(rate),'-ac',str(channels),'-c:a','pcm_s16le',raw])
        metrics=pcm_metrics(raw)
        if metrics['clipped_samples'] or metrics['peak_dbfs'] < -55:
            raise ValueError('Input is clipped or silent; correct the source before normalization.')
        gain=peak-metrics['peak_dbfs']
        run([ffmpeg,'-v','error','-y','-i',raw,'-af',f'volume={gain:.8f}dB',*codec,final])
        checked=analyze(final,scratch,ffmpeg,ffprobe,metrics['seconds'])
        result=dict(source_name=source.name,source_sha256=sha(source),gain_db=gain,
                    output=str(target.relative_to(root)),output_sha256=sha(final),metrics=checked,
                    provenance='User-supplied input; preserve its original attribution separately.')
        publish(final,target)
    return result


def normalize_music(root, config, source, keep=None):
    return normalize_audio(root,config,source,keep,'music')


def execute(root, config, args):
    try:
        if args.channels!='source' and args.action!='normalize-sfx':
            raise ValueError('--channels applies only to normalize-sfx; procedural SFX use recipe channels.')
        if args.action in ('normalize-music','normalize-sfx'):
            kind='music' if args.action=='normalize-music' else 'sfx'
            result=normalize_audio(root,config,args.name,args.keep_intermediates,kind,args.channels)
            return [{'id':'audio.'+args.action,'status':'ok','message':'Normalized into '+result['output']+'.'}],result
        if args.action == 'test':
            if not args.out:
                raise ValueError('audio test requires --out NEW_DIRECTORY.')
            from audio_runtime import test_audio
            result = test_audio(root,config,args.out,args.keep_intermediates,args.jar,cue=args.name)
            return [{'id':'audio.test','status':'warning' if result.get('qa_unavailable') else 'ok' if result['passed'] else 'error',
                     'message':'Desktop audio evidence captured.' if result['passed'] else result.get('error','Audio verification failed.')}],result
        result = music(root,config,args.name,args.keep_intermediates) if args.action == 'music' else sfx(root,config,args.name,args.keep_intermediates)
        return [{'id':'audio.'+args.action,'status':'ok','message':'Generated and measured '+args.name+'.'}],result
    except (OSError,ValueError,KeyError,TypeError,subprocess.SubprocessError) as exc:
        return [{'id':'audio.'+args.action,'status':'error','message':str(exc)}],{}

"""Editable MIDI score helpers and offline FluidSynth rendering (stdlib only).

MIDI/renderer adapted from musicgen's intro-vapores; see audio-workflow.md
and tools/licenses/compose-game-music-MIT.txt for upstream helper attribution.
"""
import ctypes as C
import ctypes.util
import math
import struct
import wave
TPQ = 480
NAMES = ['C', 'C#', 'D', 'Eb', 'E', 'F', 'F#', 'G', 'Ab', 'A', 'Bb', 'B']

def pitch(name):
    if isinstance(name, int):
        return name
    names = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'D#': 3, 'Eb': 3,
             'E': 4, 'F': 5, 'F#': 6, 'Gb': 6, 'G': 7, 'G#': 8,
             'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11}
    return 12 * (int(name[-1]) + 1) + names[name[:-1]]


def track(name, channel, program, volume, pan, role):
    return dict(name=name, channel=channel, program=program, volume=volume,
                pan=pan, role=role, notes=[])


def add(t, bar, beat, dur, note, vel=65):
    p = pitch(note)
    t['notes'].append(dict(bar=bar, beat=beat, dur=dur,
                           note=NAMES[p % 12] + str(p // 12 - 1), vel=vel / 127))


def phrase(t, bar, events, transpose=0, velocity=70):
    for event in events:
        offset, note, duration = event[:3]
        accent = event[3] if len(event) == 4 else (3 if offset % 4 == 0 else -3)
        add(t, bar + int(offset // 4), 1 + offset % 4, duration,
            pitch(note) + transpose, velocity + accent)


def vlq(n):
    out = [n & 127]
    while n >> 7:
        n >>= 7
        out.insert(0, (n & 127) | 128)
    return bytes(out)


def meta(kind, payload):
    return bytes([255, kind]) + vlq(len(payload)) + payload


def chunk(events, end):
    data, prev = bytearray(), 0
    for tick, _, value in sorted(events):
        data.extend(vlq(tick - prev) + value)
        prev = tick
    data.extend(vlq(end - prev) + meta(47, b''))
    return b'MTrk' + struct.pack('>I', len(data)) + data


def events(t, bars):
    out = []
    c = t['channel']
    for n in t['notes']:
        start = (n['bar'] - 1) * 4 + n['beat'] - 1
        if start >= bars * 4:
            continue
        finish = min(start + n['dur'], bars * 4 - .01)
        out.extend([(round(start * TPQ), 1, bytes([144 | c, pitch(n['note']), round(n['vel'] * 127)])),
                    (round(finish * TPQ), 0, bytes([128 | c, pitch(n['note']), 0]))])
    return sorted(out)


def control_events(t, bars):
    out = []
    for point in t.get('expression', []):
        beat = (point['bar'] - 1) * 4 + point['beat'] - 1
        if beat < bars * 4:
            assert 0 <= point['value'] <= 127
            out.append((round(beat * TPQ), -1,
                        bytes([176 | t['channel'], point['controller'], point['value']])))
    return out


def validate(score):
    """Reject invalid timing, ranges, duplicate channels and same-pitch overlaps."""
    def require(ok, message):
        if not ok:
            raise ValueError(message)
    require(score.get('meter') == '4/4', 'Only explicit 4/4 scores are supported.')
    bars, bpm, rate = score['loopBars'], score['bpm'], score['sampleRate']
    require(type(bars) is int and 1 <= bars <= 256, 'Invalid bar count.')
    require(isinstance(bpm, (int, float)) and math.isfinite(bpm) and 20 <= bpm <= 300, 'Invalid tempo.')
    require(rate in (44100, 48000), 'Unsupported sample rate.')
    require(abs(score['seconds'] - bars*4*60/bpm) < 1e-6, 'Inconsistent duration.')
    require(score['loopFrames'] == round(score['seconds']*rate), 'Inconsistent loop frames.')
    channels = set()
    require(bool(score['tracks']), 'Empty score.')
    for t in score['tracks']:
        c = t['channel']
        require(type(c) is int and 0 <= c <= 15 and c not in channels, 'Invalid or duplicate channel.')
        channels.add(c)
        for key in ('program', 'volume', 'pan'):
            require(type(t[key]) is int and 0 <= t[key] <= 127, 'Invalid MIDI '+key)
        for n in t['notes']:
            values = [n[k] for k in ('bar', 'beat', 'dur', 'vel')]
            require(all(isinstance(v, (int, float)) and math.isfinite(v) for v in values), 'Nonfinite note.')
            start = (n['bar']-1)*4 + n['beat']-1
            require(type(n['bar']) is int and 1 <= n['beat'] < 5 and 0 <= start < bars*4
                    and n['dur'] > 0 and start+n['dur'] <= bars*4+1e-6, 'Note outside loop.')
            require(0 < n['vel'] <= 1 and 0 <= pitch(n['note']) <= 127, 'Invalid note pitch/velocity.')
        for p in t.get('expression', []):
            require(type(p['controller']) is int and 0 <= p['controller'] <= 127
                    and type(p['value']) is int and 0 <= p['value'] <= 127
                    and 1 <= p['bar'] <= bars and 1 <= p['beat'] < 5, 'Invalid controller.')
        active = {}
        for tick, order, data in events(t, bars):
            note = data[1]
            if order:
                require(note not in active, 'Overlapping same MIDI pitch: '+t['name'])
                active[note] = tick
            else:
                require(note in active and tick > active.pop(note), 'Unbalanced/zero-length MIDI note.')
        require(not active, 'Stuck MIDI notes.')
    return sum(len(t['notes']) for t in score['tracks'])


def write_midi(score, path):
    validate(score)
    bars, end = score['loopBars'], score['loopBars']*4*TPQ
    conductor = [(0, -2, meta(3, score['title'].encode())),
                 (0, -1, meta(81, round(60_000_000/score['bpm']).to_bytes(3, 'big'))),
                 (0, -1, meta(88, bytes([4, 2, 24, 8]))),
                 (0, 0, meta(6, b'LOOP_START')), (end, 0, meta(6, b'LOOP_END'))]
    chunks = [chunk(conductor, end)]
    for t in score['tracks']:
        c = t['channel']
        setup = [(0, -4, meta(3, t['name'].encode())), (0, -3, bytes([192|c, t['program']]))]
        setup += [(0, -2, bytes([176|c, num, val])) for num, val in
                  [(7,t['volume']), (10,t['pan']), (91,30), (93,0)]]
        chunks.append(chunk(setup+events(t,bars)+control_events(t,bars), end))
    path.write_bytes(b'MThd'+struct.pack('>IHHH', 6, 1, len(chunks), TPQ)+b''.join(chunks))


def render(score, soundfont, target, library=None, cycles=3, settings_values=None):
    bars, RATE = score["loopBars"], score["sampleRate"]
    BEAT = 60 / score["bpm"]
    library = library or ctypes.util.find_library('fluidsynth')
    if not library:
        raise ValueError('FluidSynth shared library is unavailable; configure paths.fluidsynth_library.')
    lib = C.CDLL(library)
    def api(name, result, args):
        fn = getattr(lib, name)
        fn.restype, fn.argtypes = result, args
        return fn
    def require(ok):
        if not ok:
            raise ValueError("FluidSynth offline rendering failed.")
    ptr, integer, string = C.c_void_p, C.c_int, C.c_char_p
    version = api('fluid_version_str', string, [])().decode()
    settings = api('new_fluid_settings', ptr, [])()
    synth = None
    try:
        values = settings_values or dict(sample_rate=RATE, gain=.55, reverb_room_size=.34,
                                        reverb_damp=.55, reverb_width=55, reverb_level=.20, chorus=False)
        for k, v in [('synth.sample-rate', RATE), ('synth.gain', values['gain']),
                     ('synth.reverb.room-size', values['reverb_room_size']), ('synth.reverb.damp', values['reverb_damp']),
                     ('synth.reverb.width', values['reverb_width']), ('synth.reverb.level', values['reverb_level'])]:
            require(api('fluid_settings_setnum', integer, [ptr, string, C.c_double])(settings, k.encode(), v) == 0)
        require(api('fluid_settings_setint', integer, [ptr, string, integer])(settings, b'synth.chorus.active', int(values['chorus'])) == 0)
        synth = api('new_fluid_synth', ptr, [ptr])(settings)
        require(synth)
        require(api('fluid_synth_sfload', integer, [ptr, string, integer])(synth, str(soundfont).encode(), 1) >= 0)
        cc = api('fluid_synth_cc', integer, [ptr, integer, integer, integer])
        program = api('fluid_synth_program_change', integer, [ptr, integer, integer])
        on = api('fluid_synth_noteon', integer, [ptr, integer, integer, integer])
        off = api('fluid_synth_noteoff', integer, [ptr, integer, integer])
        write = api('fluid_synth_write_s16', integer, [ptr, integer, ptr, integer, integer, ptr, integer, integer])
        for t in score['tracks']:
            c = t['channel']
            require(program(synth, c, t['program']) == 0)
            for num, val in [(7, t['volume']), (10, t['pan']), (91, 30), (93, 0)]:
                require(cc(synth, c, num, val) == 0)
        length = round(bars * 4 * BEAT * RATE)
        timeline = []
        for cycle in range(cycles):
            for t in score['tracks']:
                for tick, order, data in events(t, bars) + control_events(t, bars):
                    timeline.append((cycle * length + round(tick / TPQ * BEAT * RATE), order, data))
        timeline.append((length * cycles, 2, None))
        buf = (C.c_short * (2048 * 2))()
        with wave.open(str(target), 'wb') as wav:
            wav.setparams((2, 2, RATE, 0, 'NONE', 'not compressed'))
            cursor = 0
            for sample, _, data in sorted(timeline):
                while cursor < sample:
                    count = min(2048, sample - cursor)
                    require(write(synth, count, buf, 0, 2, buf, 1, 2) == 0)
                    wav.writeframesraw(bytes(buf)[:count * 4])
                    cursor += count
                if data:
                    c, p = data[0] & 15, data[1]
                    if data[0] & 240 == 144:
                        require(on(synth, c, p, data[2]) == 0)
                    elif data[0] & 240 == 128:
                        require(off(synth, c, p) == 0)
                    elif data[0] & 240 == 176:
                        require(cc(synth, c, p, data[2]) == 0)
    finally:
        if synth:
            api('delete_fluid_synth', None, [ptr])(synth)
        api('delete_fluid_settings', None, [ptr])(settings)
    return version

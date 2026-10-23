"""Locate a known one-shot waveform in an actual PCM16 game-output recording.

Receipt times only narrow a six-second search: they are not playback timestamps.
Positive normalized correlation establishes the waveform, beyond a play request.
"""
import array
import math
from pathlib import Path
import sys
import wave


def samples(path, start=0, seconds=None):
    with wave.open(str(path),'rb') as wav:
        if wav.getsampwidth()!=2 or wav.getframerate()!=48000:
            raise ValueError('Waveform matching requires 48 kHz PCM16.')
        channels=wav.getnchannels()
        first=min(wav.getnframes(),max(0,round(start*48000)))
        wav.setpos(first)
        count=wav.getnframes()-first if seconds is None else min(wav.getnframes()-first,round(seconds*48000))
        data=array.array('h',wav.readframes(count))
    if sys.byteorder!='little':data.byteswap()
    return [sum(data[i:i+channels])/channels/32768 for i in range(0,len(data),channels)]


def reduced(data, stride=16):
    return [sum(data[i:i+stride])/stride for i in range(0,len(data)-stride+1,stride)]


def match(capture, reference, receipt_seconds):
    start=max(0,receipt_seconds-3)
    data=samples(capture,start,6)
    template=reduced(samples(reference))
    mean=sum(template)/len(template)
    template=[x-mean for x in template]
    energy=sum(x*x for x in template)
    if energy<1e-10:raise ValueError('Silent SFX template.')
    count=len(template)
    def coefficient(values):
        total=sum(values)
        variance=sum(x*x for x in values)-total*total/count
        dot=sum(x*y for x,y in zip(template,values))
        return (dot/math.sqrt(energy*variance),dot/energy) if variance>1e-12 else (0,0)
    coarse=reduced(data)
    best=(-1,0,0)
    for offset in range(0,len(coarse)-count+1,4):
        correlation,gain=coefficient(coarse[offset:offset+count])
        if correlation>best[0]:best=(correlation,gain,offset*16)
    center=best[2]
    for offset in range(max(0,center-512),min(len(data)-count*16,center+512)+1):
        correlation,gain=coefficient(reduced(data[offset:offset+count*16]))
        if correlation>best[0]:best=(correlation,gain,offset)
    return dict(reference=Path(reference).name,correlation=best[0],estimated_gain=best[1],
                capture_seconds=start+best[2]/48000,receipt_seconds=receipt_seconds,
                matched=best[0]>=.55 and best[1]>.02)

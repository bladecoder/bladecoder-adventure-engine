"""Original neutral two-bar starter cue; no existing game's melody or samples."""
from music_score import track, add


def compose():
    rate, bpm, bars = 44100, 120, 2
    seconds = bars * 4 * 60 / bpm
    pad = track('soft harmony', 0, 88, 50, 42, 'quiet continuous harmony')
    melody = track('small motif', 1, 10, 55, 88, 'short neutral example')
    for bar, chord in ((1, ('C4', 'E4', 'G4')), (2, ('A3', 'C4', 'E4'))):
        for note in chord: add(pad, bar, 1, 4, note, 50)
    for bar, notes in ((1, ('G5', 'E5', 'D5', 'E5')), (2, ('C5', 'E5', 'A5', 'G5'))):
        for beat, note in enumerate(notes, 1): add(melody, bar, beat, .8, note, 60)
    return {'title': 'Neutral starter loop', 'meter': '4/4', 'loopBars': bars,
            'bpm': bpm, 'sampleRate': rate, 'seconds': seconds,
            'loopFrames': round(seconds * rate), 'tracks': [pad, melody]}

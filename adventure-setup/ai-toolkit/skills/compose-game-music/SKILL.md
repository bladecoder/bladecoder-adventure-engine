---
name: compose-game-music
description: Compose or revise scene music as editable score/MIDI sources and render measured looping MP3 audio.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read [the audio workflow](../../docs/audio-workflow.md), the brief's musical direction and the scene's dialogue/exploration rhythm. Specify the cue's dramatic job, motif, meter/tempo, phrase/harmonic return, density and instrument/register roles. Do not synchronize essential story effects to how quickly a player reads.

Keep musical decisions in `raw/music/<cue>/compose.py`; its `compose()` returns a score without writing files. Reuse `music_score` for note validation, MIDI and rendering instead of importing another cue's script or mutating module globals. Current scores support explicit 4/4 and GM channels/programs; extend the schema deliberately for another meter.

Pin external sound-source hash/provenance, renderer settings, loop and bitrate in `render.json`. Generate with `audio music <cue>`. Final music is MP3, normally 44.1 kHz stereo; SFX use PCM16 WAV. Retain code/MIDI/score/reports and final output; large intermediates stay temporary outside the repository unless explicitly retained.

Check duration, headroom, silence and seam measurements, then actual native repeats through a declared audio profile. Set scene `musicDesc.loop` explicitly; MP3 loop metadata alone does not enable it. Listen with dialogue and SFX for fatigue, continuity and priority when listening review is in scope. Measurements and successful backend handles are separate from musical/listening approval.

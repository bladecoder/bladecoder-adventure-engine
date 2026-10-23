# Music, SFX and actual audio verification

Music uses stereo MP3; SFX use PCM16 WAV with per-effect mono/stereo selection.
The optional audio starter includes an original quiet two-bar cue and two seeded
procedural effects. Source code, score, MIDI, rendering recipe and measured outputs
are included. The external SoundFont is identified by hash and attribution; it is
not distributed. No existing game's melody or sound recording is included.

```sh
python3 -B ai-toolkit/tools/project.py audio music starter-loop --json
python3 -B ai-toolkit/tools/project.py audio sfx starter --json
```

Regeneration needs FFmpeg/FFprobe; music also requires FluidSynth and the pinned
SoundFont. Set installations in ignored `ai-toolkit/tools/local.json`. The rendering recipe
pins the source SoundFont hash, synthesis settings and encoding. Imported files
can use `normalize-music` or `normalize-sfx`; SFX preserve the source channel count
unless `--channels mono|stereo` is explicit. Procedural SFX use recipe channel counts.
Large synthesis intermediates default to temporary storage outside the repository;
retain them only with `--keep-intermediates NEW_DIRECTORY`.

Generated audio-capable projects declare `tests.audio.starter` in `adventure.json`
and include it in the `media` QA profile:

```sh
./gradlew :desktop:dist
python3 -B ai-toolkit/tools/project.py qa --profile media --jar desktop/build/libs/desktop-0.1.0.jar --out /new/media-evidence --json
```

Or select an audio profile directly with `audio test starter --jar GAME.jar
--out NEW_DIRECTORY`. Each profile declares a literal `scene` ID and optional
`chapter` (default `00`), optional MP3 `music`, a list of SFX `{id,resource}`, a
bounded `capture_seconds` and `min_music_wraps`. Optional `scenario` selects the
project's existing declarative scenario, replayed through the same engine while
music is captured. The starter uses its dialogue/inventory/save-load scenario.

Native capture currently uses Linux Pulse/PipeWire and FFmpeg. It creates a
private null sink and routes only the owned game child there. It never records
a microphone/default desktop monitor or changes device defaults. Captures,
backend state, loop positions, waveforms, transcripts and diagnostics are retained
in the evidence directory. An unavailable capture backend remains unavailable;
`--allow-unavailable` never makes a QA run complete.

SFX identity probes require one-shot, normal-pitch resources of at most five
seconds and compare recorded output to the declared source. Looping/pitched SFX
need project scenario assertions and appropriately designed measurement cases;
the one-shot probe explicitly rejects those cases rather than certifying them.
Measurements do not approve musical style, listening quality, contextual mix or
mobile device behavior. Keep those reviews separate and visible.


## Optional authoring integration checks

Core helper contracts run with `python3 -B -m unittest discover -s ai-toolkit/tools/tests -v`.
Real encoder/renderer/atlas-reader checks are separate:
`python3 -B -m unittest discover -s ai-toolkit/tools/optional-tests -v`. They use synthetic
sources and require the corresponding installed authoring tools, cached pinned
packer/reader JARs and JDK. Missing prerequisites produce explicit skips; a skipped
integration is unavailable coverage, not a verified export. Selecting the minimal
starter does not require these applications. Retain logs and inspect actual
preview/apply/runtime output when editing production assets.

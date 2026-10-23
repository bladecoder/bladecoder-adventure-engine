# Project automation tools

Run `python3 -B tools/project.py --help` from any working directory using the
script's actual path. Python 3.10+ is required. Project declarations live in
`adventure.json`; personal authoring-tool and art-root paths belong in ignored
`tools/local.json`, based on `local.example.json`. Environment overrides use
`ADVENTURE_` prefixes. No specific Drive directory is assumed.

`chapters`, `ink-links`, `ink-compile`, SVG preview/apply and atlas commands reuse
the common validators and explicit recipes. Edit sources first; review preview
reports before applying generated assets. Music outputs are MP3; SFX outputs are
PCM16 WAV with mono/stereo selected by the recipe. Optional authoring tools are
required only for the corresponding discipline.

`runtime` uses the engine's HTTP controller, private discovery and owned isolated
sessions. `qa --profile fast --out NEW_DIRECTORY` runs the declared static profile;
`qa --profile desktop --jar GAME.jar --out NEW_DIRECTORY` runs the declared native
profile. Add project-specific suites/profiles to `adventure.json`. Unknown or empty
profiles are unavailable; `--allow-unavailable` keeps `complete: false`.

Retain local evidence under a new output directory. Share only the root QA reports
and `artifacts/`; `private/` contains session records and isolated saves. Failed
runs retain diagnostics. Capture/semantic/pixel measurements do not automatically
approve visual art, translations, listening quality or device behavior.

Use `art-root --path DIRECTORY --seed-starter` to initialize a new local art root,
or omit `--seed-starter` to adopt existing sources. See
[the graphics workflow](../docs/graphics-workflow.md) for preview/apply and atlas commands.

Audio- and Spine-capable starters include original examples, editable sources
and declared native `media` QA profiles; see [audio](../docs/audio-workflow.md)
and [Spine](../docs/spine-workflow.md). Native audio capture currently requires
Linux Pulse/PipeWire. Packaged Spine QA needs no editor installation.

This toolkit is being extracted. Canonical skills and portable discovery entries are included. Use `skills-check`
and `skills-sync --dry-run` after workflow changes; independently edited entries
are conflicts and stay untouched. Environment bootstrap/distribution and remaining
copied-module compatibility reviews are still unfinished.
Commands that rely on those unfinished parts are not accepted as operational yet.

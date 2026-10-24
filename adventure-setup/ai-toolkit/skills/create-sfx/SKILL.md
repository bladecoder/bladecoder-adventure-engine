---
name: create-sfx
description: Design, generate, import and integrate short sound effects with editable recipes, PCM16 WAV output and runtime evidence.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read [the audio workflow](../../docs/audio-workflow.md), the relevant event and the brief's sound direction. Choose duration, material/envelope, timing, priority, spatial image and variation/loop needs. Keep event IDs meaningful and puzzle logic independent of audio completion.

Use `audio sfx <family>` with seeded procedural recipes under `raw/sounds/` when that palette fits. Use suitable source material or specialized generators for voices, ambience or Foley; preserve source hash, parameters/tool version, attribution and terms. The optional starter is a small example, not a mandatory style.

All final effects use PCM16 WAV. Choose mono or stereo per effect. Procedural recipes declare `channels` and optional `stereo_pan`; `normalize-sfx INPUT` preserves imported channels unless `--channels mono|stereo` is explicit. Final music uses MP3; do not introduce OGG runtime alternatives. Intermediates are temporary unless explicitly retained.

Register files in chapter `sounds`, with deliberate volume/pitch/pan, and trigger normal `PlaySound` actions or supported Ink commands. Validate references and compile changed Ink. For explicitly requested QA, verify trigger/backend events and the real interaction through the shared controller. Declared one-shot audio probes also compare captured output to source; looping/pitched cases need appropriate project probes. A successful Sound handle does not prove natural completion or listening quality.

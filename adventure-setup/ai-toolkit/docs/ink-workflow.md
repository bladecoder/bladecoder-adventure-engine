# Ink integration and narrative checks

`adventure.json` declares the canonical Ink source/output, or disables Ink. The
chapter uses `inkManager: {storyName: "main"}` to select `model/main.ink.json`;
`InkRun`/fully qualified actions select named paths. The top-level text preview
is not a production menu. Dependency versions are in `gradle.properties`.

## Source organization

Use `raw/ink/main.ink` as the include-only production entrypoint, with `INCLUDE
state.ink`, an include for every scene file and an explicit `-> END` preventing
accidental fallthrough. `state.ink` owns global narrative VAR/LIST declarations
and shared EXTERNAL declarations. Do not duplicate engine inventory or actor state.
Keep preview menus in an explicitly selected development entrypoint.

Each playable scene has `raw/ink/<scene_id>.ink`, one `=== <scene_knot> ===`
knot and `= <section>` stitches for intro, characters, responses and puzzle
logic. Support scenes also get their own file; an unused support knot can end
immediately with `-> DONE` until it owns text/logic. Use `scene.section`
from chapter actions, scenario paths and cross-scene diverts; local stitch
diverts stay relative. Normalize a scene ID only if Ink requires it (for example
`00.bank-ext` to `bank_ext`) and record the mapping in
`docs/ink/narrative-structure.md`. End scene-knot entry before its first stitch
so entering the knot does not accidentally run a conversation.

This takes Bonasera 5's scene-knot/stitch pattern as a reference. Its entrypoint
also contains global lists/externals and its `sewer.ink` serves several locations;
the toolkit convention deliberately centralizes globals in `state.ink` and
requires a separate source file per scene. Cross-scene shared dialogue can live
in an explicitly named shared module instead of being copied between scenes.

When splitting a monolithic story, map every old knot to its new scene/stitch,
update MODEL, all diverts and automation cases, regenerate the same declared
compiled output and check links. Knot/stitch renames can invalidate existing
Ink save paths/visit counts; use isolated fresh saves for the migration and
document release-save compatibility rather than claiming old saves still work.

```sh
python3 -B ai-toolkit/tools/project.py ink-compile --json
python3 -B ai-toolkit/tools/project.py ink-compile --check --json
python3 -B ai-toolkit/tools/project.py ink-links --json
```

`ink-test` and runtime checks follow the [opt-in QA policy](qa-workflow.md).
Run `python3 -B ai-toolkit/tools/project.py ink-test --json` only when that
verification is explicitly requested. Compile/load and link validation remain
normal source-generation operations.

Compilation prefers the matching cached Java compiler unless an explicit
`paths.inklecate` override is supplied. It constructs the actual matching Java
runtime Story before atomic replacement; failures preserve the existing output.
`--check` compares parsed compiled JSON without replacement. Use the explicit
Gradle compiler-preparation command from the README when its JARs are not cached.
Language documentation is bundled in the write-game-ink skill with attribution;
load relevant syntax topics rather than the whole manual.

Engine inventory, location, actor visibility/state and active player are
authoritative. Ink owns knowledge, dialogue progress and narrative flags/lists.
Use actual external queries instead of mirrored inventory flags. Runtime primitive
types matter: a boolean is not the string `"true"`. A sticky `+` choice can repeat;
gate irreversible effects and verify save/load continuation and early attempts.

Protect MODEL entry verbs with Cutmode true, awaited InkRun and Cutmode false.
Internal physical helpers inherit the caller's lock; they do not release it.
Ink temporarily clears cut mode for choices and restores the previous flag on
selection. Preserve that protocol rather than adding competing false commands
to stitches. See [verb ownership](scene-authoring-patterns.md#player-facing-verbs-and-cut-mode).

Blade narration/speaker/command parsing differs from standalone Ink hosting:
`Actor: text` selects a known actor, `$P:` selects the player, and `> Action:
key=value` produces an engine action. A colon in unprefixed narration can be
mistaken for a speaker prefix. Inspect delimiters/tags before embedding vectors,
commas, `#`, `:` or `=` in commands. Consult the bundled action contract and actual
bindings when changing integration, rather than assuming C# hosting examples.

Narrative JSON cases live in `automation/ink-scenarios`. They select a compiled `story`,
explicit simulated `world` answers and steps (`path`, `choose`, typed `set`/`lists`,
`save_load`, `assert`). Assertions can check text, choices, variables, list members
and recorded command counts. `ink-test` executes the real Ink runtime but records
engine command lines instead of executing them. Missing cases are errors.

When runtime QA is requested, verify commands/effects with a declared HTTP scenario and isolated save
profile. Collect state, transcript/action events and captures. Do not infer real
pickup, navigation or UI behavior from a permissive text fallback or simulated
external. The starter has no general Ink translation extraction pipeline;
introducing one requires explicit key/source ownership and verification.

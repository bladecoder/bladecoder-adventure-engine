---
name: write-game-ink
description: Write and debug game-integrated Ink dialogue, commands and narrative state, compiling and testing against the project runtime.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read [the project Ink contract](../../docs/ink-workflow.md), the relevant design and entrypoint from `adventure.json`. For unfamiliar syntax use the bundled [language index](references/ink/index.md), loading only relevant topics. Language examples do not establish compatibility with the selected Java runtime or engine bindings.

Author under `raw/ink/`: `main.ink` includes `state.ink` and one file per scene.
Each scene file has one scene knot and stitches for introductions, characters,
interaction sections and puzzle responses. Global narrative state belongs in
`state.ink`; use `scene.stitch` paths from MODEL and scenarios. Follow the
contract's scene-splitting/migration rules and record the mapping in
`docs/ink/narrative-structure.md`. Update every caller when migrating flat knots.

Write player-facing text in the brief's language. Preserve existing knots/stitches/variables and actor IDs unless their references are updated. Chapters select named Ink paths; a top-level text preview is not automatically the production entrypoint. Ink owns narrative knowledge and dialogue progress; engine queries own inventory, scene/player and actor state.

Speaker prefixes and commands use Blade Engine conventions, including `Actor: text` and `> Action: key=value`. A colon in unprefixed narration can be interpreted as a speaker separator. Check the contract before embedding delimiters, vectors or tags. Sticky choices can run again after save/load; gate irreversible effects by authoritative state.

Call player-facing Ink from a MODEL verb wrapped in Cutmode true/false with
`InkRun(wait=true)`. Called physical helpers inherit that lock and must not
release it before remaining text. Let Ink manage its temporary choice phase;
do not add false commands at stitch endings to bypass the outer owner. See
[cut-mode ownership](../../docs/scene-authoring-patterns.md#player-facing-verbs-and-cut-mode).

Edit `.ink` sources, then run `ink-compile`/`--check` and `ink-links`. Run focused `ink-test` cases only for explicitly requested QA. Compilation replaces output only after a matching runtime load. Narrative tests bind explicit simulated engine queries and record commands; they do not execute those commands. For explicitly requested runtime QA, verify effects through [debug-game](../debug-game/SKILL.md), including relevant failed/repeated attempts and save/load. Never patch only compiled `.ink.json`.

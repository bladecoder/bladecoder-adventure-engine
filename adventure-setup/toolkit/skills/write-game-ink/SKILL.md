---
name: write-game-ink
description: Write and debug game-integrated Ink dialogue, commands and narrative state, compiling and testing against the project runtime.
---

Read [the project Ink contract](../../docs/ink-workflow.md), the relevant design and entrypoint from `adventure.json`. For unfamiliar syntax use the bundled [language index](references/ink/index.md), loading only relevant topics. Language examples do not establish compatibility with the selected Java runtime or engine bindings.

Write player-facing text in the brief's language. Preserve existing knots/stitches/variables and actor IDs unless their references are updated. Chapters select named Ink paths; a top-level text preview is not automatically the production entrypoint. Ink owns narrative knowledge and dialogue progress; engine queries own inventory, scene/player and actor state.

Speaker prefixes and commands use Blade Engine conventions, including `Actor: text` and `> Action: key=value`. A colon in unprefixed narration can be interpreted as a speaker separator. Check the contract before embedding delimiters, vectors or tags. Sticky choices can run again after save/load; gate irreversible effects by authoritative state.

Edit `.ink` sources, then run `ink-compile`/`--check`, `ink-links` and focused `ink-test` cases. Compilation replaces output only after a matching runtime load. Narrative tests bind explicit simulated engine queries and record commands; they do not execute those commands. Verify actual effects through [debug-game](../debug-game/SKILL.md), including relevant failed/repeated attempts and save/load. Never patch only compiled `.ink.json`.

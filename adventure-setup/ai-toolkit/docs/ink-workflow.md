# Ink integration and narrative checks

`adventure.json` declares the canonical Ink source/output, or disables Ink. The
chapter uses `inkManager: {storyName: "main"}` to select `model/main.ink.json`;
`InkRun`/fully qualified actions select named paths. The top-level text preview
is not a production menu. Dependency versions are in `gradle.properties`.

```sh
python3 -B ai-toolkit/tools/project.py ink-compile --json
python3 -B ai-toolkit/tools/project.py ink-compile --check --json
python3 -B ai-toolkit/tools/project.py ink-links --json
python3 -B ai-toolkit/tools/project.py ink-test --json
```

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

Verify actual commands/effects with a declared HTTP scenario and isolated save
profile. Collect state, transcript/action events and captures. Do not infer real
pickup, navigation or UI behavior from a permissive text fallback or simulated
external. The starter has no general Ink translation extraction pipeline;
introducing one requires explicit key/source ownership and verification.

# Developing an adventure from a brief

Read `../../AGENTS.md`, `../../adventure.json`, the brief and work log. Inspect current
models/Ink/assets before assuming work is missing. Resolve identity, primary
language, controls and capability availability from project data. The default
neutral story/art/music are small replaceable examples, not creative constraints.

Use foundation diagnostics and relevant domain profiles. Missing optional apps
should be reported for dependent work while independent work continues. Generation
does not download authoring applications or launch a production agent. Explicitly
prepare Maven dependencies/build when needed; preserve useful output after errors.

Design a bounded first slice with observable acceptance criteria and state owners.
A useful slice connects a scene interaction, appropriate feedback/dialogue,
progression state and an observable result. Follow the brief's puzzle policy.
Keep dependencies, wrong/premature attempts, repeated use and persistence visible.
Use [the canonical project layout](project-layout.md): design belongs in
`docs/story/` and `docs/design/`, executable Ink in `raw/ink/`, and editable art
in `raw/art/`. Code/model/Ink files are implementation authority. Before the first
slice, apply [scene and inventory patterns](scene-authoring-patterns.md), including
scene atlases/backgrounds, explicit player, bottom origins and inventory icons.

Declare each exit with the native `leave` verb, a background hotspot/real door,
a reachable threshold and an awaited approach. Select a destination entry when
arrival placement or door motion is needed; follow the shared
[exit patterns](scene-authoring-patterns.md#scene-exits-background-hotspots-and-doors).

Implement through the relevant local skills and shared `../tools/project.py`
commands. Use reviewed art/Spine preview/apply reports. Compile/load source Ink,
validate models/references. Build the requested concrete package. Follow the
[opt-in QA policy](qa-workflow.md): run narrative tests, runtime scenarios,
playthroughs and other suites only when explicitly requested by the user.
Otherwise record QA as not_run; do not launch a game to verify each edit. Keep simulated
commands distinct from real effects, and debug preconditions distinct from a
reachable player route. Extend catalogue coverage rather than replacing an
existing test just to obtain a pass.

Update the work log after a meaningful delivery: exact scope, changed files,
source/package identity, checks/evidence, remaining uncertainty and next action.
Resume by inspecting that state; do not infer completion from a checkbox or intent.
Expand only within the requested scope and acceptance criteria. A completed slice
is progress toward the game, not proof that the whole brief is fulfilled.

Skills are canonical in `../skills/`; `../../.agents/skills` is a relative symlink from the project root to `../ai-toolkit/skills`. Edit skills and references under `../skills/` and validate
them with `skills-check`. Discovery reads the same files directly.

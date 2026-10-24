---
name: implement-puzzle
description: Implement or repair documented puzzles across Blade Engine scene models and Ink, verifying observable behavior and persistence.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Compare the relevant design with current chapter/Ink/Java behavior before editing. Record entry state, authoritative owners, preconditions, trigger, expected effects and repeat/interruption policy. Preserve working interactions outside the requested scope.

Use [scene editing](../edit-scenes/SKILL.md) and [Ink integration](../write-game-ink/SKILL.md). Engine inventory/location/actor state must not be mirrored by independent Ink flags. Consult [the Ink contract](../../docs/ink-workflow.md) before changing state ownership or external bindings.

For acquisition/use/return, follow the [inventory and pickup pattern](../../docs/scene-authoring-patterns.md).
Keep world props separate from support-scene icons, gate the physical MODEL verb
from the scene's Ink stitch, and verify both representations after acquisition,
repeated attempts and save/load. Use the [canonical layout](../../docs/project-layout.md).

For gated passages, keep the public exit as `leave`/`leave.<state>` with guarded
feedback, awaited approach and door actions before terminal Leave. A successful
use/unlock should enable the same exit, not introduce a pickup-only transition.
Follow the [exit/door patterns](../../docs/scene-authoring-patterns.md#scene-exits-background-hotspots-and-doors).

Player-facing verbs acquire cut mode before acting and release it only after the
whole interaction finishes, including awaited feedback on failed attempts. Track
one outer owner across Ink/RunVerb helpers; a nested pickup must not unlock its
caller. Apply [cut-mode ownership and exit handoff](../../docs/scene-authoring-patterns.md#player-facing-verbs-and-cut-mode).

Compile changed Ink and validate model/references. When QA is explicitly requested, run focused narrative cases and the project-declared runtime scenario for the changed route, including premature/wrong/repeated attempts and save/load where relevant. Use [debug-game](../debug-game/SKILL.md) and [QA](../../docs/qa-workflow.md); distinguish debug preconditions from player reachability and simulated commands from actual effects.

Update the design/work log with implemented behavior, evidence and gaps. Do not rename unrelated IDs or change other puzzles merely to make a broad checker pass.

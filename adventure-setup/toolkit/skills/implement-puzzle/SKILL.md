---
name: implement-puzzle
description: Implement or repair documented puzzles across Blade Engine scene models and Ink, verifying observable behavior and persistence.
---

Compare the relevant design with current chapter/Ink/Java behavior before editing. Record entry state, authoritative owners, preconditions, trigger, expected effects and repeat/interruption policy. Preserve working interactions outside the requested scope.

Use [scene editing](../edit-scenes/SKILL.md) and [Ink integration](../write-game-ink/SKILL.md). Engine inventory/location/actor state must not be mirrored by independent Ink flags. Consult [the Ink contract](../../docs/ink-workflow.md) before changing state ownership or external bindings.

Compile changed Ink and run model/reference and focused narrative checks. Add a project-declared runtime scenario for the changed route, including premature/wrong/repeated attempts and save/load where relevant. Use [debug-game](../debug-game/SKILL.md) and [QA](../../docs/qa-workflow.md); distinguish debug preconditions from player reachability and simulated commands from actual effects.

Update the design/work log with implemented behavior, evidence and gaps. Do not rename unrelated IDs or change other puzzles merely to make a broad checker pass.

---
name: edit-scenes
description: Edit Blade Engine chapter/world scenes, actors, verbs, geometry and resource references using the MODEL contract.
---

Read [the MODEL contract](../../docs/chapter-format.md), the relevant design and current `assets/model/` implementation. Preserve unrelated formatting and IDs. MODEL definitions and serialized save STATE have different fields; use the bundled schema/action metadata, not a saved snapshot as a scene template.

Keep map keys consistent with IDs and verb keys `id[.target][.state]`. Actor references `scene#actor` and animation references `actor#animation` are different; scene IDs can contain dots. `$PLAYER`, inventory and previous-scene references depend on runtime context. Do not replace dynamic references with guessed constants just to remove warnings.

Worlds, scenes and interactive actors need explicit verb maps, including `verbs: {}` when empty. Verify actual packaged renderer resources/regions and animation duration, loop and alignment. A staged PNG alone is not an atlas region. Empty anchor bounds, negative positions and flipped scale can be intentional.

Keep engine-owned inventory/navigation/visibility/state authoritative. Check asynchronous `wait`, text queues and cut mode when changing a verb. Run `chapters`, relevant Ink checks and a focused [runtime scenario](../../docs/runtime-workflow.md). Record actual behavior and isolated save/load evidence, not only a successful build.

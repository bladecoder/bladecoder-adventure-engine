---
name: edit-scenes
description: Edit Blade Engine chapter/world scenes, actors, verbs, geometry and resource references using the MODEL contract.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read [the MODEL contract](../../docs/chapter-format.md), the relevant design and current `assets/model/` implementation. Preserve unrelated formatting and IDs. MODEL definitions and serialized save STATE have different fields; use the bundled schema/action metadata, not a saved snapshot as a scene template.

Read [scene authoring patterns](../../docs/scene-authoring-patterns.md) when
creating or restructuring scenes/items. Default to one scene atlas with a
scene-level background, `player: "<CharacterActor ID>"` on playable scenes,
renderer `orgAlign: 4`, and a separate `inventory` atlas/support scene. Implement
each character with its own animation atlas (or its own Spine cutout atlas),
referenced consistently in every scene. Implement
classic acquisition as a guarded world pickup sequence obtaining the inventory
icon; verify its Goto/refPoint, forward/reverse animation and cut mode. Do not
serialize an unsupported actor-level `player: true` or transfer world sprites
into inventory by default.

Declare scene exits with `leave` on a background-type `InteractiveActor` (no
renderer) over the painted passage, or on the actual animated door sprite. Use
an awaited Goto before Leave, real door-opening actions when needed, and a
protected destination entry. See [exit/door patterns](../../docs/scene-authoring-patterns.md#scene-exits-background-hotspots-and-doors);
do not replace the native arrow with a pickup/use-only sign sprite.

Keep map keys consistent with IDs and verb keys `id[.target][.state]`. Actor references `scene#actor` and animation references `actor#animation` are different; scene IDs can contain dots. `$PLAYER`, inventory and previous-scene references depend on runtime context. Do not replace dynamic references with guessed constants just to remove warnings.

Wrap player-facing verbs (lookat/lookup, pickup, dialog/talkto, use/combine and
leave, including qualified variants and world fallbacks) with Cutmode true/false.
Await their blocking actions. One outer interaction owns cleanup: nested helpers
inherit the lock, and terminal Leave transfers cleanup to the destination entry.
Follow the detailed [cut-mode ownership pattern](../../docs/scene-authoring-patterns.md#player-facing-verbs-and-cut-mode).

Worlds, scenes and interactive actors need explicit verb maps, including `verbs: {}` when empty. Verify actual packaged renderer resources/regions and animation duration, loop and alignment. A staged PNG alone is not an atlas region. Empty anchor bounds, negative positions and flipped scale can be intentional.

Keep engine-owned inventory/navigation/visibility/state authoritative. Check asynchronous `wait`, text queues and cut mode when changing a verb. Run `chapters` and relevant Ink compilation/reference checks. Run a focused [runtime scenario](../../docs/runtime-workflow.md) and save/load checks only when the user requests QA; otherwise report their status as not_run.

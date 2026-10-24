---
name: create-game-art
description: Create or edit scene/background/UI art and frame animations with explicit SVG exports, reviewed previews and atlas packing.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read [the graphics workflow](../../docs/graphics-workflow.md), the brief's style references and the current scene/source. Use configured `art_root`; repository-local and external art are supported. Keep personal roots in local configuration, never recipes, and do not create release archives of artist sources.

Use [canonical source/output folders](../../docs/project-layout.md). Default
scene exports to an atlas group containing `background` and local object regions;
inventory icons use their own `inventory` atlas and consistent canvas. Each
character has its own atlas with all animations, reused across scenes; Spine
characters use their own cutout atlas from their rig export. Follow [scene resource and origin
patterns](../../docs/scene-authoring-patterns.md), including bottom alignment,
ground reference points and explicit scene background properties. Standalone
images require a concrete use such as UI, a diagnostic or an exceptional asset.

Coordinate painted passages with renderer-less background exit hotspots and the
native `leave` arrow. Export a separate door sprite only when it must animate;
match its open/closed states and threshold to the background. See the
[exit/door patterns](../../docs/scene-authoring-patterns.md#scene-exits-background-hotspots-and-doors).

For existing SVG art, prefer direct vector edits preserving useful shapes, palette, perspective and line weight. Declare independent drawable IDs, excluded guides, crop/canvas, scale, alpha and runtime destination in a manifest. Historical export hints and editor visibility are not export instructions. New raster elements may use available image-generation tools when appropriate; preserve their source/provenance and editable assembly rather than claiming a bitmap is vector-editable.

Frame families share a canvas/reference point and contiguous zero-based indices; set model timing, loops and alignment explicitly. Run `manifests`, `art-preview ... --out NEW_DIRECTORY`, inspect changed images/contact sheets and then `art-apply` the reviewed report. Use `atlas-preview GROUP` and a separately reviewed apply for atlas groups. Changed inputs or destinations require a fresh preview.

Validate model/Ink references and inspect generated previews. When the user requests runtime/visual QA, verify rendering and interaction in the actual game and use a declared visual matrix when it helps; do not reuse another environment's baseline as automatic approval. Character cutout rigging uses [animate-spine](../animate-spine/SKILL.md), with different attachment naming and packing rules.

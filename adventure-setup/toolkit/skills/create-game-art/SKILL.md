---
name: create-game-art
description: Create or edit scene/background/UI art and frame animations with explicit SVG exports, reviewed previews and atlas packing.
---

Read [the graphics workflow](../../docs/graphics-workflow.md), the brief's style references and the current scene/source. Use configured `art_root`; repository-local and external art are supported. Keep personal roots in local configuration, never recipes, and do not create release archives of artist sources.

For existing SVG art, prefer direct vector edits preserving useful shapes, palette, perspective and line weight. Declare independent drawable IDs, excluded guides, crop/canvas, scale, alpha and runtime destination in a manifest. Historical export hints and editor visibility are not export instructions. New raster elements may use available image-generation tools when appropriate; preserve their source/provenance and editable assembly rather than claiming a bitmap is vector-editable.

Frame families share a canvas/reference point and contiguous zero-based indices; set model timing, loops and alignment explicitly. Run `manifests`, `art-preview ... --out NEW_DIRECTORY`, inspect changed images/contact sheets and then `art-apply` the reviewed report. Use `atlas-preview GROUP` and a separately reviewed apply for atlas groups. Changed inputs or destinations require a fresh preview.

Validate model/Ink references and verify the changed rendering and interaction in the actual game. Add a declared visual matrix when it helps; do not reuse another environment's baseline as automatic approval. Character cutout rigging uses [animate-spine](../animate-spine/SKILL.md), with different attachment naming and packing rules.

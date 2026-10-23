---
name: animate-spine
description: Create or adapt cutout rigs, edit Spine animations and export compatible skeletons/atlases with actual renderer verification.
---

Read [the Spine workflow](../../docs/spine-workflow.md) and the declared recipe before choosing a template. Use a project-provided rig or the optional original starter example; no external game character is implicit. Pin the installed editor version and compatible runtime family. The editor is an authoring prerequisite, not a runtime/debugging dependency.

Choose `derive` to create another editable project with explicit SVG pieces/edits; retain untouched bones, slots, skins and timelines. Existing animation replacement must be declared. Choose `export` for a manually maintained project so its work is not reconstructed from an old template. Preserve piece dimensions, attachment transforms/pivots, slot order and expression variants.

Use the declared JSON/binary/atlas settings: nonessential editable JSON, exact attachment names, straight alpha for the current renderer. Region cutouts are supported; unsupported meshes/constraints require an explicit adapter extension, not silent data loss. Known omitted default animation values are normalized during round-trip checks; unexplained differences stop export.

Generate a new preview, review pieces and run `spine test REPORT --out NEW_DIRECTORY` through the shared engine. Inspect captures and bone/slot/track state, then `spine apply REPORT`. Update scene references to reviewed outputs explicitly. Packaged regression uses project-declared `tests.spine` profiles in QA, without the editor. Add project-specific movement/dialogue assertions where needed; the generic rig probe does not prove full character behavior.

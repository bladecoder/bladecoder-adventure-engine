---
name: animate-spine
description: Create or adapt cutout rigs, edit Spine animations and export compatible skeletons/atlases with actual renderer verification.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read [the Spine workflow](../../docs/spine-workflow.md) and the declared recipe before choosing a template. Use a project-provided rig or the optional original starter example; no external game character is implicit. Pin the installed editor version and compatible runtime family. The editor is an authoring prerequisite, not a runtime/debugging dependency.

Choose `derive` to create another editable project with explicit SVG pieces/edits; retain untouched bones, slots, skins and timelines. Existing animation replacement must be declared. Choose `export` for a manually maintained project so its work is not reconstructed from an old template. Preserve piece dimensions, attachment transforms/pivots, slot order and expression variants.

Each Spine character owns a separate atlas containing all images used by its
cutout attachments/skins, paired with that character's skeleton and reused across
scenes. Do not mix cutout pieces into scene, inventory or other character atlases.
Use [the canonical layout](../../docs/project-layout.md) and the declared export
recipe to keep editable sources, attachment staging and packaged atlas distinct.

Use the declared JSON/binary/atlas settings: nonessential editable JSON, exact attachment names, straight alpha for the current renderer. Region cutouts are supported; unsupported meshes/constraints require an explicit adapter extension, not silent data loss. Known omitted default animation values are normalized during round-trip checks; unexplained differences stop export.

Generate a new preview, review pieces and export resources. Run `spine test REPORT --out NEW_DIRECTORY` through the shared engine only when the user requests QA. Inspect captures and bone/slot/track state before claiming runtime approval; if application requires that proof, retain the unverified preview and report the pending check. Update scene references to reviewed outputs explicitly. Packaged regression uses project-declared `tests.spine` profiles in QA, without the editor. Add project-specific movement/dialogue assertions where needed; the generic rig probe does not prove full character behavior.

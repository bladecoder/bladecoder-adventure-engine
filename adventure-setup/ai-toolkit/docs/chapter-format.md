# Scene model validation and project actions

`assets/model/world` selects the initial chapter and global verbs. Each
`assets/model/<chapter>.chapter.json` declares scenes, actors, layers, renderers,
resources and verb/action sequences. The runtime consumes Blade MODEL JSON,
including its existing relaxed syntax; do not reformat every model during edits.
`ai-toolkit/tools/model-contract/chapter.schema.json` describes structure and
`ai-toolkit/tools/chapter_validate.py` verifies references and runtime-sensitive constraints.

Run `python3 -B ai-toolkit/tools/project.py chapters --json` after model edits. Findings
identify resource, JSON pointer and source line. Dynamic identifiers and unknown
custom actions remain explicit warnings; successful static validation does not
prove runtime effects. Run the relevant declared HTTP scenario against the built
package to verify actual behavior and retain state, events and screenshots.

The shared action contract was reviewed against the Blade Engine 4.5.0 source
line and contains only engine actions, including the optional Spine plugin's action metadata. Its source
hashes and engine revision are recorded in `ai-toolkit/tools/model-contract/actions.json`.
Its `engine_version` targets the exact engine paired with this bundle; a release
version rebinding retains `source_engine_version` and the original reviewed
source hashes. This is artifact version binding, not an engine migration.
The MODEL serializers were reviewed against the former 4.4.0 contract: controller
changes add runtime events/diagnostics without changing the MODEL structure.
The source extractor supports reviewed versions only and is not a runtime import.
Editor `required` annotations alone are hints, not proof of a runtime error.

Declare custom actions explicitly in `adventure.json`:

```json
{"model_contract":"ai-toolkit/tools/model-contract/project-actions.json"}
```

That extension has `schema_version: 1`, optional matching `engine_version`, and an
`actions` map from serialized class/alias to `{class, fields}`. `class` is qualified;
each field records `java_type`, `semantic_type`, `required_hint`, `initializer`
and optional `enum`/`default_hint`. It can add actions but cannot override engine
entries. Missing, malformed, escaping or incompatible extension files produce
validation errors. Uncatalogued qualified actions still warn rather than being
mistaken for verified engine behavior.

When maintaining custom Java actions and an explicit engine source checkout is
available, refresh reviewable metadata without executing Java or downloading:

```sh
python3 -B ai-toolkit/tools/engine_contract.py --engine /local/bladecoder-adventure-engine --project . --custom-only --output ai-toolkit/tools/model-contract/project-actions.json
```

Review the extracted contract before committing it. Ordinary validation reads
bundled metadata and needs neither the engine checkout nor the editor. Custom
runtime semantics and side effects need their own scenario assertions.


Worlds, scenes and interactive actors (`InteractiveActor`, `SpriteActor`,
`CharacterActor`) must declare `verbs: {}` even when they have no behavior. The
MODEL reader otherwise leaves a null map that can fail during sound caching or
verb lookup. The validator reports `chapter.verbs-required` before startup.
Anchors and walk zones do not need interactive verb maps.

## Layer ordering and visible output

The `layers` array is stored front to back: for example `foreground`, `dynamic`,
then `background`. `Scene.draw` traverses that array in reverse, painting the
background before the actors closer to the viewer. An opaque background sprite
in the first array entry can cover interactive actors in later entries, even
though hit tests and controller state still report those actors as visible.
`visible` defaults to true and `parallax` to 1; changing those fields does not
repair an incorrect layer order. A scene's separate `backgroundAtlas` renders
before all scene layers.

After changing scene composition, inspect an actual UI capture with the game
running. Actor flags, bounds, interaction success and static model validation
alone do not prove that the intended art is visible. Verify both source images
and final compositing before accepting a scene.

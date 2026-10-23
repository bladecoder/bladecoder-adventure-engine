# Optional Spine cutout workflow

Selecting Spine adds the runtime plugin and a tiny original two-bone envelope rig
with `idle` and `wave` animations, its editable project/JSON/PNG, runtime skeleton
and atlas, and `tools/spine-recipes/neutral-rig.json`. The scene's optional `courier`
actor exercises the rig; the note interaction stays independent. This is a small
replaceable example, not a production humanoid animation library.

The installed licensed Spine editor is needed only for authoring/export. Neither
the editor nor a license is bundled. The example pins editor 4.3.23 and runtime
family 4.3. Use an explicitly compatible editor/runtime pair for another template.
Canonical sources default to `raw/art`, or the selected external art root.

```sh
python3 -B tools/project.py spine export tools/spine-recipes/neutral-rig.json --out /new/spine-preview --json
python3 -B tools/project.py spine test /new/spine-preview/report.json --out /new/spine-runtime --json
python3 -B tools/project.py spine apply /new/spine-preview/report.json --json
```

Review preview images and runtime captures before applying. Export-only recipes
preserve manually maintained projects. Derivation recipes name a different output
project, an SVG pieces manifest and explicit edits; source template projects are
not overwritten. Supported cutouts use region attachments; mesh/constraint rigs
require a deliberate adapter extension. Required existing animations remain
present, and replacing existing animation names must be explicit.

JSON round trips preserve bones, slots and skins exactly. Animation comparison
allows Spine to omit known default frame values (`time:0`, zero translation/
rotation, unit scale); nondefault values, curves and other semantics remain
compared. Unexplained round-trip changes stop export. Runtime tests load the actual
binary/atlas, observe bones/slots/tracks and replay animations with captures.

Project-declared `tests.spine` profiles let `qa --profile media --jar GAME.jar
--out NEW_DIRECTORY` test a packaged actor without installing or launching the
editor. A profile declares chapter, scene, actor, skeleton source, atlas, exported
editor version and selected animation names. Optional `motion_bones` asserts that
those bones change pose across samples. Save/load and engine diagnostics are
checked. Tests use the shared HTTP controller and isolated saves; they do not
approve animation artistry, full character behavior or mobile device rendering.


## Derive another rig without changing the template

Spine-capable output includes `tools/spine-recipes/derived-courier.json`, its
edits and an explicit SVG pieces manifest. This original example shifts one bone
and adds `salute`, preserving `idle` and `wave`. It produces a different editable
project under `characters/derived-courier` in the selected art root:

```sh
python3 -B tools/project.py spine preview tools/spine-recipes/derived-courier.json --out /new/derived-preview --json
python3 -B tools/project.py spine test /new/derived-preview/report.json --out /new/derived-runtime --json
python3 -B tools/project.py spine apply /new/derived-preview/report.json --json
```

Review the piece/contact images and actual renderer captures before apply. The
example does not automatically replace the scene actor; model edits should select
the reviewed skeleton/atlas explicitly and rerun the project's scenario. Export
and derivation remain separate operations, with hashes rejecting stale previews.

# Editable art and packaged graphics

The legacy starter may have an original SVG at `raw/art/starter.svg`, with explicit
`room` and `note` export groups. `automation/asset-manifests/starter.json` selects each
group, its page/canvas bounds, pixel dimensions and standalone runtime destination.
No inference from an SVG's visible layers or old Inkscape export hints is needed.
When adopting the canonical layout, move that shared sheet to
`raw/art/shared/starter.svg` and update its manifest source together.
The starter uses standalone images only as a minimal bootstrap. New scenes use
scene atlas groups by default; the bootstrap is not the production convention.
Follow [project layout](project-layout.md) and [scene/inventory
patterns](scene-authoring-patterns.md). Pack a scene's background and local props
together, use scene background properties, and pack inventory icons separately.
Each character owns its own atlas containing all frame animations and reuses it
across scenes. Spine characters likewise own a cutout atlas paired with their
skeleton. Scene atlases contain the background and local props, not character
regions; inventory icons have their own atlas.

A passage painted into the background normally needs only a renderer-less
InteractiveActor hotspot with `leave`, not a separate sign/arrow sprite. A door
that opens needs matching SpriteActor frames in the scene atlas. Coordinate
thresholds, reach points and open/closed states with the
[exit/door patterns](scene-authoring-patterns.md#scene-exits-background-hotspots-and-doors).

With repository art storage, tools default to `raw/art`. With external storage,
select an existing root explicitly, or initialize a new root from staged sources:

```sh
python3 -B ai-toolkit/tools/project.py art-root --path /local/art/new-adventure --seed-starter --dry-run --json
python3 -B ai-toolkit/tools/project.py art-root --path /local/art/new-adventure --seed-starter --json
```

Omit `--seed-starter` to adopt existing artist sources without copying or changing
them. Initialization refuses any existing destination, including empty directories
and symlinks. The root is saved only in ignored `ai-toolkit/tools/local.json`; recipes remain
root-relative. `--config` and environment overrides cannot redirect this write.
Staged starter originals remain in `raw/art`; remove them deliberately only after
verifying the selected external copy. There are no per-release raw source archives.
Large artist originals can remain outside Git and be backed up independently.

Inkscape and ImageMagick are optional authoring prerequisites, configured locally
or found on PATH. Regenerate into a new preview directory, review the before/after
images and dimensions, then apply the exact reviewed report:

```sh
python3 -B ai-toolkit/tools/project.py manifests --json
python3 -B ai-toolkit/tools/project.py art-preview automation/asset-manifests/starter.json --out /new/art-preview --json
python3 -B ai-toolkit/tools/project.py art-apply /new/art-preview/report.json --json
```

The apply step checks source, candidate and destination hashes before replacement.
It rejects stale previews and preserves original SVGs. Animation families declare
a common canvas and reference point; frame indices and locale variants are explicit.
A scene viewport change scales the starter room/position in the model rather than
silently rewriting source art or recipe dimensions.

For atlas content, declare `destination.kind: atlas`, a logical `group`, output
`raw/images/<group>/<region>.png` and optional pack settings in its SVG manifest.
Then use `atlas-preview <group> --out NEW_DIRECTORY`, review and `art-apply` its
report. The packer is hash-pinned by `ai-toolkit/tools/asset-settings/packer-lock.json` and
verifies output geometry using the runtime parser. Resolve its JARs explicitly:

```sh
./gradlew -I ai-toolkit/tools/automation.gradle :desktop:automationPackerClasspath
```

That resolution may need network access on first use; generation itself does not.
Editable Spine characters have separate project-provided recipes and pinned editor
versions. Neither proprietary editor installations nor existing game characters
are supplied by the starter.


## Optional authoring integration checks

Execute the following test suites only when the user explicitly selects QA,
according to the [execution policy](qa-workflow.md). Export previews, manifest
validation and atlas packing/parser checks remain normal authoring operations.

Core helper contracts run with `python3 -B -m unittest discover -s ai-toolkit/tools/tests -v`.
Real encoder/renderer/atlas-reader checks are separate:
`python3 -B -m unittest discover -s ai-toolkit/tools/optional-tests -v`. They use synthetic
sources and require the corresponding installed authoring tools, cached pinned
packer/reader JARs and JDK. Missing prerequisites produce explicit skips; a skipped
integration is unavailable coverage, not a verified export. Selecting the minimal
starter does not require these applications. Retain logs and inspect actual
preview/apply/runtime output when editing production assets.

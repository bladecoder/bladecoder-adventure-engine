# Project layout and source ownership

Use these paths for new adventures and new artifacts. Keep an existing declared
layout until a migration is requested; migrate references and recipes together.
Create directories only when they contain selected work. Paths, structured keys,
fixed labels, identifiers and code/comments use English; narrative/design prose
and player text use the brief's language.

| Path | Contents and authority |
| --- | --- |
| `docs/brief.md` | Creative scope, language, capabilities and acceptance criteria. |
| `docs/work-log.md` | Implemented behavior, current evidence, gaps and next action. |
| `docs/story/` | Story creation, editorial review and revised story; use `01-story-creation.md`, `02-editorial-review.md`, `03-revised-story.md`. |
| `docs/design/` | `puzzle-dialogue-design.md`, `puzzle-dependencies.md` and its PNG export, `puzzle-dialogue-review.md`, `skill-gap-review.md`. |
| `docs/art/` | `art-direction.md`, visual references and composition decisions; editable artwork belongs in `raw/art/`. |
| `docs/ink/` | `narrative-structure.md`: scene/file/knot mapping, state owners and integration notes; executable Ink belongs in `raw/ink/`. |
| `docs/audio/` | Music and SFX direction/cue documentation. |
| `docs/localization/` | Locale decisions, terminology and linguistic review. |
| `raw/ink/` | `main.ink`, `state.ink` and one `<scene_id>.ink` per scene. Declare `main.ink` in `adventure.json`. |
| `raw/art/scenes/` | Editable scene/background sources, normally `<scene_id>.svg`. |
| `raw/art/characters/` | Editable character sheets, rig artwork and per-character Spine projects/attachments. |
| `raw/art/inventory/` | Editable inventory icon artwork. |
| `raw/art/shared/` | Explicitly shared sheets used by several export groups. |
| `raw/images/<atlas_group>/` | Generated flat PNG inputs: one group per scene, one per character with all its animations, and `inventory`; scene groups include `background.png`. Spine attachment staging follows its declared character recipe. |
| `raw/music/`, `raw/sounds/` | Editable scores, sources and recipes; shipped outputs are MP3 and PCM16 WAV respectively. |
| `automation/asset-manifests/` | Reproducible SVG export and atlas-group recipes. |
| `automation/ink-scenarios/`, `automation/runtime-scenarios/` | Narrative simulations and real engine/input scenarios respectively. |
| `assets/model/` | World/chapter MODEL and regenerated compiled Ink output. |
| `assets/atlases/1/` | Packed scene and inventory atlases, plus one atlas per character; a Spine character's atlas contains its cutout images. |
| `artifacts/qa/<run>/`, `artifacts/art/<run>/` | Retained reports/captures and reviewed art previews; never authoritative editable sources. |

An external art root uses the same relative `scenes/`, `characters/`,
`inventory/`, `shared/` organization. Keep its machine path in ignored local
configuration. Avoid alternate `design/`, `ink-src/` or `docs/raw/` roots in new
games. Inventory is a support scene, not an extra playable story location.

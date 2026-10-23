# Independent adventure setup

Implementation status: the HTTP template baseline is committed as `b9f10d86`
on `controller-srv`; the setup implementation is on the
`feat/adventure-setup` stack. The shared generator, editor integration, neutral
starter, project-local tools and skills, optional media examples, safe updater,
and local bundle are implemented. Local acceptance includes five native fresh
consumers, packaged HTTP contracts, an independent brief-driven game and
representative Bonasera compatibility. The phase 1 Gradle build passed for
`b9f10d86` ([CI run](https://github.com/bladecoder/bladecoder-adventure-engine/actions/runs/39959816579)).
Both workflows passed for setup commit `4a84bf22`: [consumer validation](https://github.com/bladecoder/bladecoder-adventure-engine/actions/runs/39958487264)
and [Gradle build](https://github.com/bladecoder/bladecoder-adventure-engine/actions/runs/39958487302).
Hosted publication and device certification have not been executed.

## Generation contract (version 1)

```json
{
  "schema_version": 1,
  "id": "my-adventure",
  "title": "My Adventure",
  "package": "org.example.adventure",
  "platforms": ["desktop"],
  "engine_version": "4.5.0-SNAPSHOT",
  "game_version": "0.1.0",
  "language": "en",
  "viewport": [960, 540],
  "interaction_mode": "two_buttons",
  "art_storage": "repository",
  "ink": true,
  "spine": false,
  "audio": false
}
```

The toolkit is selected by the versioned setup bundle, rather than by a mutable
global installation. `--version` reports the bundle/dependency identities.
Fields other than `schema_version` have the defaults shown; omitting
`engine_version` selects the version paired with the generator. Version 1 rejects other engine versions
before writing; use a matching bundle rather than generating incompatible HTTP
launchers. Unknown/duplicate
settings, invalid typed values, invalid Java packages and incompatible options
fail before writing a project. `id` is a portable lowercase identifier; `title`
is independent Unicode display text. Version 1 requires desktop and permits
optional Android/iOS scaffolding. Interaction modes are `two_buttons`, `pie` and `single_click`.
Art storage is `repository` or `external`; personal roots are local configuration,
not versioned recipe/spec inputs. Capability selections are declared setup inputs. Audio and Spine now add original
editable examples, runtime resources and declared native media profiles. Optional
authoring applications are required only for regeneration/export, not creation
or ordinary packaged playback.

```sh
./gradlew :adventure-setup:check :adventure-setup:dist :adventure-editor:compileJava
java -jar adventure-setup/build/libs/adventure-setup-4.5.0-SNAPSHOT-all.jar create --spec /path/to/spec.json --out /new/project --dry-run --json
java -jar adventure-setup/build/libs/adventure-setup-4.5.0-SNAPSHOT-all.jar create --spec /path/to/spec.json --out /new/project --json
```

The input and output paths are explicit. Destinations must not already exist,
including empty directories or symlinks. Generation uses a sibling staging
directory, reports failures with exit code 1, and does not overwrite a developed
game. Dry-run validates/renders and lists output files without creating the
output directory. Results/errors are JSON on stdout; `--help` is readable text.
The API is `AdventureSetup.generate(SetupSpec, Path, dryRun, optionalSdk)`.

Generation requires a Java runtime but no display, GL backend, editor installation
or Android SDK. The only parser library is LibGDX core; the standalone JAR has no
engine/editor or native-backend dependency. It copies the canonical bundled
resources without resolving Maven dependencies or invoking Gradle. Optional
`--sdk PATH` is valid only when Android is selected, writes ignored
`local.properties`, and does not enter the portable manifest.

The generated `setup-manifest.json` records normalized inputs, setup version and
hashes of generated files. It excludes personal destination/SDK paths. Files
are deterministic for a selected spec and bundle. Actual engine build identity
must additionally be checked from the compiled package/runtime, especially when
using a snapshot. Explicit toolkit updates use this provenance to detect authored
changes; regenerating over an existing project is deliberately rejected.

The standalone setup CLI delivers file generation. Generated project
tools expose diagnostics, domain operations and declared QA profiles. The standalone CLI also provides `plan-update` and `apply-update`; see below. Optional `git-init` and `github-create` are separate explicit operations; they are never called by generation. Hosted agent launch remains independently selected. A failed later build must preserve the project
and its evidence rather than report file generation as failed or delete it.

## Shared editor integration

The editor depends on `adventure-setup` and calls the same generator through its
existing `BladeEngineSetup` adapter. The canonical templates live only in
`adventure-setup/src/main/resources/projectTmpl/`. The editor retains its separate
project loading/UI lifecycle. Its existing all-platform creation adapter selects
desktop/Android/iOS; desktop-only CLI output does not include Android plugin or
SDK configuration. Platform scaffolding is not device verification.

See [implementation evidence](adventure-setup-validation.md) for executed checks
and temporary artifact locations. The standalone contract harness is attached
to `:adventure-setup:check`, separate from JUnit discovery.


## Generated project QA catalogue

The generated `adventure.json` separates portable declarations from ignored
`tools/local.json` installation paths. `tests.suites` maps project-defined suite
names to versioned HTTP scenarios. `tests.profiles` selects QA tasks; no Bonasera
scenario IDs, rigs or audio cues are implied. The starter declares `fast` and
`desktop` profiles. Run from the generated project:

```sh
python3 -B tools/project.py qa --profile fast --out /new/fast-evidence --json
./gradlew :desktop:dist
python3 -B tools/project.py qa --profile desktop --jar desktop/build/libs/desktop-0.1.0.jar --out /new/desktop-evidence --json
```

Use the generated game's actual versioned JAR filename. Python tooling requires
Python 3.10+; visual assertions/tests additionally need Pillow. Tasks run in order,
retain failures, identify the package hash and export bounded diagnostic artifacts.
Only `artifacts/` and the root reports are intended for sharing; `private/` includes
local session records and isolated saves. Native tasks require real desktop GL.
Missing profiles are unavailable, never empty successes. `--allow-unavailable`
can permit an incomplete run but does not set `complete` to true.

A profile is a list of tasks with unique portable `id`, `kind` and optional
`value`. Supported kinds are `tests`, `formats`, read-only `cli`, declared `suite`,
`scenario`, visual `matrix`/`pixels`, `trace`, `explore` and `reduce`. Scenario,
matrix and exploration values are project-relative specification paths. `pixels`
references an earlier matrix task and an explicitly reviewed `--baseline`;
`trace` declares `value: {"reference":"earlier-task","candidate":"later-task"}`
with both tasks preceding the comparison. There are no automatic visual approvals.
Project-declared `audio` and `spine` QA task kinds now select corresponding
`tests.audio` and `tests.spine` profiles. Media-capable starters include a `media`
profile with their selected examples; authoring and probe limitations remain
explicit in the generated workflow guides.

The default two-button starter uses left click to examine and right click to pick
up the note, following `SceneGestureListener.mouseToAction`. Both operations reach
the actual input processor in the smoke scenario. Ink-enabled output also checks
a real dialogue choice and typed story state; no-Ink output uses an engine text
action. Save/load and final UI capture are verified for both configurations.
The radial-menu starter adds clicks on `pie.lookat` and `pie.pickup`; the
single-click starter initially exposes examination, then enables pickup after
reading through an actor state. Both alternate modes pass packaged native smoke
through their own generated QA catalogue. External-art initialization, starter
SVG recipes, optional audio/Spine examples and native probes have local
verification evidence. The brief-driven pilot and representative Bonasera
compatibility run are recorded in their separate evidence files.


## Art sources and project action extensions

Generated projects include `docs/graphics-workflow.md`, explicit starter SVG
export recipes and `art-root --path DIRECTORY [--seed-starter] [--dry-run]`.
Root selection is a separate explicit operation, storing personal paths only in
ignored `tools/local.json`. Generation does not create or modify Drive folders.
Repository-local art defaults to `raw/art`; external storage uses the selected
root. Starter files remain staged until deliberately removed after verification.

`docs/chapter-format.md` documents the neutral 4.5.0-SNAPSHOT model/action contract
and project-provided action metadata. No Bonasera actions are bundled as engine
capabilities. `adventure.json` may declare a project-relative `model_contract`;
additions are type-checked and cannot replace the shared engine entries.


## Optional media examples

`audio: true` adds an original MP3 loop and two PCM16 WAV effects with their
editable sources. `spine: true` adds an original small envelope cutout rig with
editable project/JSON and `idle`/`wave` animations, plus a separate derivation
example adding `salute`. Selected capabilities are combined in the generated
`media` QA profile. Minimal output omits these sources/assets when disabled.

Packaged Spine verification uses the runtime plugin and never opens the editor.
Native audio capture uses a private Linux Pulse/PipeWire sink; unavailable capture
prerequisites cannot be treated as complete coverage. See the generated workflow
guides and [executed media checks](adventure-setup-media-validation.json).


## Local distribution and toolkit updates

Build `:adventure-setup:bundle` to obtain
`adventure-setup/build/distributions/adventure-setup-VERSION.zip`. It contains the
standalone JAR, `SHA256SUMS`, bundle identity metadata and the tested/unavailable
[compatibility matrix](adventure-setup-compatibility.md). Generation and updates
perform no downloads. First consumer builds need their own dependency resolution.
A local artifact is not a published release or a remote CI result.

```sh
java -jar /path/to/setup.jar plan-update --project /path/to/game --out /new/plan.json --json
# Review changes/conflicts; only edit row resolutions: keep-project / use-bundle.
java -jar /path/to/setup.jar apply-update --project /path/to/game --plan /path/to/reviewed-plan.json --json
```

The three-way comparison preserves authored changes, rejects unresolved/stale or
tampered plans and incompatible current engine declarations, and rolls back
installed files on failure. Project recipes, game sources/assets, build modules,
launchers, metadata, personal settings, brief and work log are excluded. Original
generation provenance remains intact; the latest toolkit identity, expected
hashes and kept project edits are recorded separately. Partial adoption remains
explicit. Full generated instructions live in `docs/toolkit-workflow.md`.


## Optional Git and hosted publication

`git-init --project DIRECTORY [--branch main]` initializes only the selected local
project and creates no commit. `github-create --project DIRECTORY --repository
OWNER/NAME --visibility private|public --out NEW_REPORT [--push]` is a separate
explicit remote operation requiring GitHub CLI authentication. Existing origins
are rejected. Push requires a committed clean tree; no commit is implicit.
A report outside the repository journals preparation, potential remote mutation
and success/failure. Partial remote state remains visible for deliberate recovery;
there is no automatic remote deletion. Generated projects include
`docs/repository-workflow.md`. No real hosted repository was created to test the
interface; its fault tests use an injected command transport.

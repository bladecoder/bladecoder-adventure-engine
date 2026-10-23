# Setup compatibility

The setup bundle records its paired engine and dependency versions, along with
source fingerprints for the generator, templates and toolkit. Generation and
toolkit updates require the matching engine version. Toolkit updates do not
migrate engine dependencies, launchers, game sources or project recipes.

| Capability | Behavior | Boundary |
| --- | --- | --- |
| Desktop generation | Produces a runnable desktop project without requiring a display, editor installation or Android SDK | The generated project resolves its own build dependencies |
| Interaction modes | Supports two-button, radial-menu and single-click starters | Starter behavior does not certify arbitrary game content |
| Ink | Can be enabled or omitted; viewport and language are configurable | Ink story behavior is project-specific |
| External art | Supports an explicitly selected local art root and starter SVG export recipes | The artist's storage root must be configured on each workstation |
| Audio | Optional stereo MP3 music and PCM16 mono/stereo WAV sound effects | Native capture depends on an available audio backend; listening quality needs human review |
| Cutout Spine | Optional starter rig, atlas and animations for the runtime plugin | Mesh/constraint authoring requires separate Spine tooling and is outside the included adapter |
| Android / iOS | Generates platform scaffolding through the shared project generator | Device builds and playback require platform-specific SDKs and review |
| Toolkit updates | Applies reviewed three-way updates to managed toolkit files | Does not update authored game content or engine dependencies |

The generator targets Java 8 bytecode. Snapshot bundles and games must use the
matching engine and dependency versions recorded in their manifests. Visual
baselines require project-specific review.

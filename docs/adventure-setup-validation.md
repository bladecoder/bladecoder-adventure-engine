# Adventure setup implementation evidence

Current local acceptance is summarized in
[`adventure-setup-toolkit-validation.json`](adventure-setup-toolkit-validation.json).
The chronological notes below include intermediate failures and superseded
pending states. The five-consumer native matrix, editor create/load flow,
independent brief pilot and representative Bonasera regression have since
passed. Their evidence is bound to the artifact and source identities recorded
in the current summary; remote CI, hosted publication and device builds have
not been run.

## Phase 1 — HTTP template baseline (2026-10-22)

The current `controller-srv` templates now expose player/debug selection,
private discovery, explicit external overrides and an unattended real-GL window.
Port zero requires explicit debug mode; missing/invalid values and HTTP settings
without server activation fail with a nonzero exit. General debug logging does
not elevate player mode. Interactive fullscreen/visibility defaults remain.

Generated builds use the current snapshot repository, optional Maven local
resolution, the installed compatible Gradle 9.3.1 wrapper and matching controller
version. An obsolete unused minimalcode dependency was removed after a fresh
package build exposed it. No generator extraction is included in this baseline.

A temporary harness invoked the existing `BladeEngineSetup.build` with the real
Android SDK, exactly as the editor delegates generation. It produced a neutral
project under `/tmp/adventure-template-phase1-fixed`. The existing generator still
requires editor implementation dependencies and invokes Gradle; those are phase 3
work, not capabilities claimed here.

Engine/editor checks and local engine publication pass
(`/tmp/adventure-setup-phase1-build.log`). The generated project's
`:core:compileJava :desktop:dist` passes offline
(`/tmp/adventure-template-phase1-fixed-package.log`). Running the packaged game
from isolated directories outside its checkout passes all 32 launcher/controller
checks in `.github/scripts/verify_template_http.py`: documented flags, invalid arguments,
player mode despite logging, hidden debug routes, explicit authenticated debug,
available-port discovery, neutral scene loading/focused query, content-store
observation, hash-checked native capture and normal startup with no listening
sockets. All three owned processes stop. Evidence is under
`/tmp/adventure-template-http-smoke-run`.

The initial generation/build trials are retained under
`/tmp/adventure-template-phase1*`; they do not replace the passing fixed build.
This is desktop smoke, not mobile or native OS input verification.

Reproduce the package smoke with a freshly generated template project:

```sh
python3 -B .github/scripts/verify_template_http.py --jar /absolute/path/to/desktop.jar --out /new/evidence/directory
```

A real desktop/virtual GL context is required. The script uses private isolated
profiles and records no token in its public result. Discovery records under the
evidence directory remain private and must not be published.

Packaged game SHA-256: `ca3ad76d3780e7073e90f49ec21607186f1a3906fd454ec031425c852aa4957d`.

The next implementation branch must be created from the independently committed
server/template baseline. Later setup work must retain this verification gate.


## Shared headless generator extraction (in progress)

Branch: `feat/adventure-setup`; exact stack parent: `b9f10d86`.
The new `adventure-setup` module owns the only template directory and exports a
Java API plus a 4.3 MiB standalone JAR. The editor delegates its existing adapter
to that API. Old file-copy helper classes were removed. Generation has no build
invocation or SDK/GL initialization and defaults to desktop-only output.

`:adventure-setup:check :adventure-setup:dist :adventure-editor:compileJava` passes
with 57 contract controls, including schema types/unknown and duplicate keys,
integer bounds, destination/symlink preservation, deterministic hashes and
Unicode properties. Final log: `/tmp/adventure-setup-shared-api-check.log`.
The packaged generator records its source fingerprint independently of its
snapshot version. Outputs/errors are standard JSON; no Java type wrappers are
emitted in the manifest.

With DISPLAY, WAYLAND_DISPLAY, ANDROID_HOME and ANDROID_SDK_ROOT removed, the
standalone JAR generated `/tmp/adventure setup café`. That project's desktop
package builds offline without an Android plugin dependency
(`/tmp/adventure-setup-headless-package.log`). Its actual packaged launcher passes
the same 32 HTTP checks (`/tmp/adventure-setup-extracted-http`), with owned cleanup.
Equivalent all-platform inputs through the existing editor adapter and through
the standalone CLI produce byte-identical manifests and file hashes
(`/tmp/adventure-editor-adapter-probe`, `/tmp/adventure-cli-adapter-probe`).
This exercises the shared API; it does not claim a manual editor UI playthrough
or mobile build/device execution.

Generation now rejects any existing destination, including an empty directory;
SDK paths, when explicitly supplied for Android scaffolding, are local ignored
settings outside portable provenance. At this initial extraction checkpoint the generator still shipped the old
blank scene; subsequent starter verification is recorded below. Generic
project tools/skills, updates and CI remain unfinished; extraction success is not full setup-plan
acceptance.


## Playable starter and portable QA — 2026-10-23

- `/tmp/adventure-catalogue-correct-buttons/result.json`: the Ink starter passes
  all 16 scenario steps, including actual left-click examination, actual
  right-click pickup, Ink choice/state, isolated save/load, diagnostics and
  screenshot evidence. Earlier failed trials remain available and are not passes.
- `/tmp/adventure-noink-native/result.json`: a separately generated no-Ink
  starter at 1280×720 passes all 13 steps with no evidence errors. Generation and
  package build logs: `/tmp/adventure-noink-create.json` and
  `/tmp/adventure-noink-package.log`.
- `/tmp/adventure-toolkit-catalogue-tests.log`: 71 Python tests pass with the
  bundled Python/Pillow runtime, without skips. Coverage includes malformed
  metadata, absent QA profiles, task failures and retained subsequent results.
- `/tmp/adventure-qa-fast-evidence/result.json`: a fresh generated consumer runs
  its own fast QA profile successfully. Chapter dynamic-reference warnings
  remain explicit rather than hidden. Its package was built separately before
  starting native QA (`/tmp/adventure-qa-consumer-package.log`).

The initial actual-input scenario used the wrong mouse button and picked up the
note before its dialogue. Inspection of the engine's input mapping established
the correction; the engine input implementation required no change for this fix.
The generic fixture loader did require respecting the chapter's configured Ink
story name instead of assuming it matched the chapter ID. That engine fix is on
the setup stack and still needs representative Bonasera regression coverage.
These checks do not complete optional interaction modes, external-art setup,
media adapters, skills, distribution, updates, CI or the agent-from-brief pilot.

- `/tmp/adventure-qa-desktop-evidence/result.json`: the same fresh consumer also
  passes its declared desktop smoke through the QA orchestrator, with complete
  diagnostic export. Summary: [starter validation](adventure-setup-starter-validation.json).


### Alternate controls

Fresh generated packages pass their own desktop QA profile in both remaining
interaction modes: `/tmp/adventure-pie-qa/result.json` (20 scenario steps) and
`/tmp/adventure-single-click-qa/result.json` (16 steps). Radial-menu tests click
the actual named menu buttons. Single-click output uses a state-qualified pickup
verb so the initial click examines the note and a subsequent click collects it.
Both verify Ink choice/state, inventory, save/load, diagnostic export and capture.
The module contract also checks these generated structures; current build log:
`/tmp/adventure-setup-interaction-modes-build.log`. No native tests overlapped,
and each package build was confirmed terminal before its execution.


## Portable art and neutral model contract — 2026-10-23

A generated external-art project initializes its SVG root at a path with spaces
and Unicode using the explicit `art-root` operation. Dry-run writes nothing;
existing artist folders can be adopted without modification. Seeding refuses
occupied destinations and symlinks. Local configuration is ignored by Git, while
recipes and the generation manifest contain no local absolute root.

The starter manifest exports `room` and `note` independently. Actual Inkscape
previews at `/tmp/adventure-external-art-preview` reproduce both shipped PNGs
with zero pixel differences. The reviewed apply succeeds. A neutral atlas is
packed and parsed, reviewed, applied and loaded through AtlasRenderer in a fresh
package; `/tmp/adventure-neutral-atlas-qa/result.json` passes its actual-input
Ink/inventory/save-load smoke with complete evidence export.

The action contract was re-extracted from the reviewed 4.5.0-SNAPSHOT engine
sources, removing the three Bonasera custom actions and project source hashes.
There are 118 engine action aliases/classes. Project additions use an explicit
`model_contract` declaration; malformed metadata and attempts to replace engine
actions fail validation. Ordinary model validation requires no source checkout.
The neutral consumer passes model validation with zero warnings/errors and
matching compiler/runtime Ink freshness checks.

79 Python tests pass without skips (`/tmp/adventure-neutral-contract-verified-tests.log`).
The last package-generation contract build passes 132 checks
(`/tmp/adventure-neutral-contract-build.log`). Summary:
[art and contract validation](adventure-setup-art-validation.json).
The Ink doctor now detects the cached Java compiler used by `ink-compile`; its
full profile still fails explicitly on absent skills, which remain unfinished.


## Native media adapters and original optional starters — 2026-10-23

Audio-capable output now includes an original two-bar stereo MP3 cue, original
mono/stereo PCM16 WAV effects, source score code/MIDI/recipes and measured outputs.
Spine-capable output includes an original envelope rig, editable project/JSON/PNG,
binary/atlas, export and derivation recipes. No Bonasera music, characters or rig
projects are required. Authoring applications are not invoked during generation.

A fresh combined consumer at `/tmp/adventure-final-media-consumer` builds and
passes its declared media QA profile. Audio performs 21 assertions, including
native backend controls, music capture during the project's own actual-input Ink
scenario, loop wraps and source waveform matches. Spine performs 45 assertions,
including actual loaded bones/slots/tracks, animated bone changes and save/load.
Both owned child processes terminate and evidence export completes. Sources are
also available through the explicitly configured external art root.

The generic preview test additionally exercises an actual Spine CLI export and
renderer fixture (`/tmp/adventure-neutral-spine-runtime-verbs`). Derivation adds
`salute` and a requested three-unit bone offset, retaining `idle`/`wave`; actual
runtime pose evidence observes the offset. Reviewed apply creates a separate
editable project and leaves the source template unchanged
(`/tmp/adventure-derived-courier-verification.json`). Known omitted default frame
values are normalized for round-trip comparison; changed nondefault keys still
fail. The initial failed derivation and startup runs remain diagnostic evidence.

Startup fixture failures now retain the original error and state instead of
retrying until a generic watchdog. That exposed an omitted empty verb map in the
diagnostic fixture. Both fixture generation and static validation now account for
explicit `verbs` objects required by MODEL readers; tests cover the omission.

87 Python tests pass without skips; the setup/editor build passes with 147 setup
contract checks. The bundle rejects incompatible engine versions before creating
output. A read-only Bonasera model probe reports zero errors and 413 warnings;
this is not a substitute for the still-pending native compatibility regression.

Summary: [media validation](adventure-setup-media-validation.json). Supported probe
limits and prerequisites are documented in generated `docs/audio-workflow.md` and
`docs/spine-workflow.md`. Skills/discovery, the remaining compatibility review,
update/distribution/CI work and the bounded agent pilot remain unfinished.


## Portable skills, independent handoff and safe updates

Eleven neutral skills are generated with canonical sources in `skills/` and thin
portable discovery entries under `.agents/skills/`. Local reference checks,
metadata/discovery synchronization, conflict preservation and failure rollback
are covered. Fresh-consumer foundation checks pass. An actual normal-entry
player-mode route verifies dialogue, inventory and save/load without debug
fixtures; the owned process stops. The independent brief pilot remains open.

The pilot found a real hardcoded Ink entry in `ink-links`. The canonical helper
now reads `adventure.json`, handles disabled Ink explicitly and bounds includes
to the selected entry directory. Four regression tests and the full 97-test
Python suite pass without skips. The pilot retains its original failure.

`plan-update` and `apply-update` use baseline/local/incoming hashes and reviewed
resolutions. Conflicts/staleness/tampering, managed symlinks, engine mismatch,
concurrent locks, permissions and rollback are exercised. Authored game content
is excluded. A partial adoption is recorded rather than claimed fully upgraded.
The setup/editor build passes 271 controls (including file provenance checks).

A checksum-bearing local ZIP is consumed outside the engine checkout. Its JAR
generates without display/SDK variables and preserves authored helper/brief
content during a reviewed update. A separate five-consumer static matrix passes
29 operations, including packaging of desktop, alternate controls/no Ink,
external art/mobile scaffolding, audio and Spine variants. `complete` is false
because that matrix did not select native execution. Remote CI and publication
have not run. CI code now defines the native matrix and packaged player/debug
contracts, with bounded retained reports. See
`adventure-setup-toolkit-validation.json` for identities, evidence and open gates.

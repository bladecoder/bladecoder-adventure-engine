# Adventure setup implementation evidence

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
checks in `tools/verify_template_http.py`: documented flags, invalid arguments,
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
python3 -B tools/verify_template_http.py --jar /absolute/path/to/desktop.jar --out /new/evidence/directory
```

A real desktop/virtual GL context is required. The script uses private isolated
profiles and records no token in its public result. Discovery records under the
evidence directory remain private and must not be published.

Packaged game SHA-256: `ca3ad76d3780e7073e90f49ec21607186f1a3906fd454ec031425c852aa4957d`.

The next implementation branch must be created from the independently committed
server/template baseline. Later setup work must retain this verification gate.

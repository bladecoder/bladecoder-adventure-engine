# Project-declared verification

`adventure.json` declares runtime `tests.suites`, ordered QA `tests.profiles` and
optional media profiles. The generated `fast` profile validates tools/models,
formats and selected Ink references. `desktop` exercises the starter's actual
input/dialogue/inventory/save-load scenario; optional `media` selects original
audio/Spine examples. These are initial coverage, not full-game certification.

```sh
python3 -B tools/project.py qa --profile fast --out /new/fast --json
python3 -B tools/project.py qa --profile desktop --jar /absolute/game.jar --out /new/desktop --json
```

Build first and use the actual JAR/version. A run records its hash and engine
metadata and refuses a changed package during verification. Do not rebuild a JAR
while its native test runs. Run owned native sessions sequentially, retain failed
attempts and confirm process completion before retries. New output directories
preserve earlier evidence. Static checks can run without a display; native GL
checks require a functioning graphical environment.

Task kinds include tooling `tests`, read-only `cli`, audio `formats`, declared
`suite`, explicit `scenario`, visual `matrix`/`pixels`, `trace`, `explore`, `reduce`
and selected `audio`/`spine` profiles. Paths are project-relative and comparisons
refer to preceding task evidence. Missing profiles and unavailable environments
remain incomplete; `--allow-unavailable` can permit an incomplete run but never
turn it into complete coverage. Skipped dependency tests remain unavailable.

Inspect root `result.json`/`report.md` and the assertions, transcript, states,
commands and captures. Share root reports plus bounded `artifacts/`; `private/`
contains discovery/process records, profiles and stores. Export budgets and
redactions are recorded. Failure collection is not permission to publish private
records or copy uninspected executable fixtures into reports.

Declare focused scenarios for new behavior: entry/owners, wrong and premature
attempts, actual input or semantic control as appropriate, repeats and persistence.
Debug fixtures establish conditions, not a reachable playthrough. Narrative tests
simulate engine queries and record commands. Test actual engine effects separately.
Reviewed pixel baselines are explicit and environment-compatible; changes require
actual image review. Listening, linguistic, artistic and device reviews must be
recorded as performed, failed or unavailable rather than inferred from metrics.

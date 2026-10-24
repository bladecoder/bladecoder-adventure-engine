# Blade Engine adventure

Project identity, language and selected capabilities are in `adventure.json`.
The generated starter is a replaceable example; write the intended game's
premise, style and acceptance criteria in [the brief](../docs/brief.md).

```sh
python3 -B ai-toolkit/tools/project.py doctor --profile foundation --json
./gradlew :desktop:dist
```

Use [the canonical project layout](docs/project-layout.md) and
[scene/inventory/character patterns](docs/scene-authoring-patterns.md).
QA is opt-in: run tests, runtime scenarios, playthroughs and media/device probes
only when the user explicitly requests that verification. Compilation and
static source/reference validation remain normal authoring work. See the
[QA policy](docs/qa-workflow.md); omitted QA is recorded as `not_run`.

For explicitly requested QA, select the relevant profile:

```sh
python3 -B ai-toolkit/tools/project.py qa --profile fast --out /new/fast-evidence --json
python3 -B ai-toolkit/tools/project.py qa --profile desktop --jar desktop/build/libs/desktop-0.1.0.jar --out /new/desktop-evidence --json
```

Use the actual generated game version in the JAR filename. First build may need
Maven network access; generation itself needs only the setup bundle/Java and
invokes no build, editor or authoring app. JDK 21 and Python 3.10+ are the normal
development prerequisites. Optional visual tests need the pinned Pillow version
in `ai-toolkit/tools/requirements-visual.txt`; do not confuse a skipped test with coverage.

For Ink compiler/graphics dependencies, explicitly prepare the matching tools:

```sh
./gradlew -I ai-toolkit/tools/automation.gradle :desktop:automationInkCompilerClasspath
./gradlew -I ai-toolkit/tools/automation.gradle :desktop:automationPackerClasspath
```

Configure authoring applications/art roots in ignored `ai-toolkit/tools/local.json`.
[Graphics](docs/graphics-workflow.md), [Spine](docs/spine-workflow.md),
[audio](docs/audio-workflow.md), [Ink](docs/ink-workflow.md) and
[model](docs/chapter-format.md) guides describe actual commands and limitations.
Optional media examples/profile are present only for selected capabilities.

The engine's HTTP controller ships in the game. See [runtime](docs/runtime-workflow.md)
and [QA](docs/qa-workflow.md) for isolated sessions and evidence. Ordinary desktop
play: `java -jar desktop/build/libs/desktop-0.1.0.jar -w` (use the built filename).
Mobile scaffolding does not certify device execution.

Canonical agent workflows live in `skills/`; `.agents/skills` is a relative
symlink to that directory for agent discovery.
Use [the agent workflow](docs/agent-workflow.md) and [work log](../docs/work-log.md)
to implement a brief in small slices with explicit QA status. `setup-manifest.json` records initial
provenance; do not regenerate an existing developed game over its directory.
Toolkit update planning/distribution work may require a newer setup release.

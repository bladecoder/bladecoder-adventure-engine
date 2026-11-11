# Engine authoring session

Canonical world/chapter edits go through the engine's explicit `edit` HTTP mode.
Do not hand-edit `assets/model/world`, `world.json` or `*.chapter.json`, and do not
fall back to file writes when the controller is unavailable. Initial project
scaffolding and isolated test fixtures are exceptions. Ink sources, compiled Ink,
localization bundles, source assets and recipes retain their discipline workflows.

Start a source session with an engine and generated DesktopLauncher that support
`-http-mode edit -edit-project ABSOLUTE_PROJECT_ROOT`. Older engines/launchers must
be updated first. Editing uses canonical assets, not packaged JARs or overrides.
If object references are broken, launch the generated DesktopLauncher with
`-http-mode edit -edit-project ABSOLUTE_PROJECT_ROOT -edit-allow-missing-references`.
Startup logs each missing reference as `EDIT WARNING`; `/edit/v1/session` also
lists them in `model.missing_references`. The preview may be unavailable until
the broken references are corrected, but queries, edits, and saves remain available.

```sh
python3 -B ai-toolkit/tools/project.py edit start --out /new/edit-session --chapter 00 --json
python3 -B ai-toolkit/tools/project.py edit status --session /new/edit-session --json
python3 -B ai-toolkit/tools/project.py edit query --session /new/edit-session --arguments '{}' --json
```

Queries without a target list document names. A target is a recursive entity
selector: `{"kind":"chapter","id":"00"}`, or for example
`{"kind":"actor","id":"door","parent":{"kind":"scene","id":"room","parent":{"kind":"chapter","id":"00"}}}`.
The world selector is `{"kind":"world"}`. Nested kinds are `scene`, `actor`,
`layer`, `verb`, `action`, `dialog`, `dialog_option`, `renderer`, `animation`,
`sound` and `geometry`. Lists use `index` instead of `id`; create appends at the
current list size. Geometry is the owning actor's bbox array. Animations address
MODEL `fanims`, not runtime playback state.

Use `edit command --operation entity.create|entity.update|entity.delete|entity.rename`
with `--arguments JSON` or `--spec FILE`. Create supplies `target` and `value`;
update supplies `target`, `fields` and optional `unset` field names; geometry
update supplies `value`. Delete supplies `target`; rename supplies `target` and
new `id`. Native MODEL types and identifiers apply. Never send STATE snapshots.

Use `--operation batch` with `{"operations":[{"operation":"entity.update","arguments":{...}},...]}`
for mutually dependent changes. Repair references explicitly in the same batch.
Errors preserve the model revision; inspect their cause rather than blindly
retrying. `--revision N` pins the revision read earlier. `--request-id ID` provides
idempotency within the session; retain it if a connection times out. HTTP callers
can query `/edit/v1/commands/ID` before retrying.

```sh
python3 -B ai-toolkit/tools/project.py edit select --session /new/edit-session --arguments '{"chapter":"00","scene":"room"}' --json
python3 -B ai-toolkit/tools/project.py edit capture --session /new/edit-session --arguments '{"debug_lines":true}' --out /new/scene.png --json
python3 -B ai-toolkit/tools/project.py edit save --session /new/edit-session --json
python3 -B ai-toolkit/tools/project.py edit stop --session /new/edit-session --json
```

The preview is static and runs no gameplay. Captures default to no debug lines.
Changes remain in memory until explicit save; closing loses unsaved changes.
External file modifications cause a save conflict. An unfinished multi-file save
leaves `assets/model/.edit-recovery` and blocks further sessions: restore each
manifest entry from its same-named backup, delete entries with no backup (new
files), then remove the recovery directory. Keep a copy of recovery evidence
before manual recovery. Do not bypass a recovery conflict by deleting evidence.

Use query to inspect results, then save and run `chapters` and relevant reference
checks. Authoring previews are production work; gameplay QA remains opt-in.

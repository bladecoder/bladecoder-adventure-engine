# Reviewable toolkit updates

The project's `ai-toolkit/tools/` and `ai-toolkit/skills/` sources are local and reviewable. Obtain an
explicit compatible setup bundle, verify its checksum, and keep that same JAR
through planning and application. No global skill installation is required.

```sh
java -jar /path/to/adventure-setup-VERSION-all.jar plan-update --project /path/to/game --out /new/update-plan.json --json
```

Review every row. `baseline_sha256` is the prior bundled version,
`local_sha256` is the current file, and `bundle_sha256` is the proposed version.
Unchanged upstream files retain local edits. Pristine older files can be replaced
or removed. A changed upstream file that also differs locally is a conflict;
new upstream names colliding with local files and local deletions are conflicts.

Only edit each row's `resolution`: `keep-project` preserves local content (or
its deliberate absence); `use-bundle` accepts incoming content (or removal).
Replace every `unresolved` value before applying. Review any source changes and
migrations; a conflict-free plan does not prove compatibility with authored game
behavior. Keep the plan and version-control diff as review evidence.

```sh
java -jar /path/to/adventure-setup-VERSION-all.jar apply-update --project /path/to/game --plan /path/to/reviewed-plan.json --json
```

The CLI rechecks the entire plan, current manifest, project engine declarations
and file hashes. Changed inputs or edited plan paths require a new plan. Managed
symlinks and incompatible engine versions are rejected. Writes use staged payloads
and backups; failures restore installed files. If restoration itself fails, the
error identifies retained backups for manual recovery. Do not edit managed files
concurrently with application. An existing `.adventure-setup-update.lock` must be
investigated before retrying, rather than removed while an updater is running.

Managed content is reusable Python/Java helpers, tests/licenses/model contracts,
canonical skills and their discovery symlink, technical workflow guides and project
instructions. Game assets, raw sources, scene/Ink files, build modules/launchers,
`adventure.json`, personal settings, the brief/work log and project recipes or
runtime/narrative scenarios are excluded. Updating this toolkit does not migrate
the engine or regenerate a developed game. Inspect new upstream project-template
features separately if you need to adopt them.

The manifest retains original generation provenance and records the latest
bundle's expected managed-file hashes plus kept local edits. A partial result is
explicit; it does not certify a fully compatible upgrade. Run `skills-check` and
keep custom skill content under `ai-toolkit/skills/`, where the discovery symlink exposes it
directly, and run `skills-check` after edits. Follow the [opt-in QA
policy](qa-workflow.md): run project QA profiles and authored playthroughs only
when explicitly requested by the user. Otherwise record runtime QA as not_run.
Generation, building, Git initialization, remote publication and launching an
agent remain separate operations.

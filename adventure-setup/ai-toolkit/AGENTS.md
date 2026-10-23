# Adventure agent guide

Read `adventure.json` for the project ID, package, primary language, capabilities,
Ink entrypoint and declared test catalogue. Read `docs/brief.md` for creative
scope and `docs/work-log.md` for current progress; neither a starter example nor
a previous plan overrides implemented behavior or the user's current request.

## Sources and ownership

- `assets/model/world` and chapter JSON define scenes, actors, verbs and resources.
- The Ink source/output paths are declared in `adventure.json`. Edit sources and
  regenerate; never fix only compiled story JSON. Engine inventory/location/actor
  state and Ink narrative knowledge have separate authoritative owners.
- Editable art defaults to `raw/art` or the explicitly selected external root;
  `ai-toolkit/tools/local.json` holds ignored machine paths. Use explicit SVG/Spine recipes.
- `raw/music` and `raw/sounds` hold selected audio sources/recipes. Final music is
  MP3; effects are PCM16 WAV with mono/stereo chosen per effect.
- `core/src/main/java/` is available for project code under the declared package.
  The engine is a dependency; ordinary development/debugging needs no editor.
- `ai-toolkit/skills/` is canonical. `.agents/skills` is a relative symlink to `../ai-toolkit/skills`,
  so agents discover the canonical files directly. Edit skills only under
  `ai-toolkit/skills/`; no discovery copy needs synchronization.

## Work and verification

Use the relevant local skill and `ai-toolkit/tools/project.py --help`. Follow
`ai-toolkit/docs/agent-workflow.md`. Write player-facing content and narrative design in the
brief's chosen language; write programming documentation in English. Do not
impose an example game's tone, puzzle policy, locale or visual style.

Start with `doctor --profile foundation`; resolve only dependencies relevant to
the selected task. Dependency preparation and build are explicit operations;
file generation does not install applications or launch an agent.
Use the Gradle wrapper and qualified desktop tasks; unqualified Android tasks may
require its SDK. Project Java remains Java 8 compatible; use the configured JDK
needed by the wrapper (JDK 21 is the documented development choice).

After model changes run `chapters`; after Ink changes compile/load, check links
and relevant narrative cases. Build a concrete desktop JAR before native QA.
Do not rebuild that JAR while a test uses it. Do not overlap owned native game
sessions in the same verification run. Confirm each process terminal before a
retry, retaining its first failure and diagnostics.

Run project-declared QA/scenarios for affected behavior and inspect captures.
Debug fixtures, semantic controls, actual input and player reachability prove
different things. Isolated narrative commands are simulated, not engine effects.
Use isolated saves; stop only owned sessions. Share root QA reports/artifacts,
not private discovery records, profiles or stores. Device, artistic and listening
reviews remain explicit when not executed.

Keep changes scoped, preserve source/output provenance and inspect the diff.
Gradle resource processing can update packaged version properties; account for
those changes deliberately. Record implemented versus intended behavior,
evidence, unavailable checks and next actions in `docs/work-log.md`.
Remote creation/publication and starting a production agent are separate selected
operations; this guide does not grant additional authorization.

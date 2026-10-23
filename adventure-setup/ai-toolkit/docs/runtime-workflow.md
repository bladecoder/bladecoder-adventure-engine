# Shared runtime controller

The controller is part of Blade Engine and works from a packaged game without the
editor. Player mode exposes player-visible state; debug mode explicitly enables
internal observation, fixtures and mutations. Logging `-d` does not elevate mode.
Private discovery identifies a session, loopback endpoint and bearer token.

```sh
python3 -B ai-toolkit/tools/project.py runtime start --jar /absolute/game.jar --out /new/session --json
python3 -B ai-toolkit/tools/project.py runtime state --session /new/session --path /scene --json
python3 -B ai-toolkit/tools/project.py runtime events --session /new/session --after 0 --json
python3 -B ai-toolkit/tools/project.py runtime capture --session /new/session --out /new/capture --json
python3 -B ai-toolkit/tools/project.py runtime stop --session /new/session --json
```

Managed starts isolate saves and retain process/build identity; stop requires
owned process records. Attach explicitly with a private discovery path when needed;
do not stop an unrelated process. Source launches prepare the development
classpath explicitly; `--jar` is the independent packaged route. `--scene` is a
literal debug scene ID, with `--chapter` (default `00`), not a split dotted name.
Startup fixture failures retain their structured cause and available evidence.

Commands use request identity, operation/arguments and a bounded timeout. The
client normally polls terminal completion; `command --no-wait`, `job` and `cancel`
expose asynchronous control. Acceptance is not completion. Cancellation/deadlines
do not roll back effects. Preserve first failures; reconnect explicitly after a
stale identity rather than blindly retrying mutations. Session capability metadata
is authoritative for available controls.

Useful debug operations include `fixture.scene`, typed `fixture.variable`,
`fixture.actor`, `animation.play`, `verb.run`, `dialogue.advance`/`choose`,
`gameplay` save/load, conditional `wait`, `clock.freeze`/`step`, `capture` and
`inspector.open`/`export`. State snapshots expose loaded scenes/actors, typed Ink,
text/choices, inventory, renderers, diagnostics and audio; they do not synthesize
unloaded resource state. Event cursors report loss explicitly. Keep command,
frame/source/action, dialogue, input and capture evidence together.

```sh
python3 -B ai-toolkit/tools/project.py runtime run --suite smoke --jar /absolute/game.jar --out /new/smoke --json
python3 -B ai-toolkit/tools/project.py runtime run --scenario automation/runtime-scenarios/smoke.json --engine-replay --jar /absolute/game.jar --out /new/replay --json
```

Suites come from `adventure.json`. Version-1 scenarios declare chapter/scene,
seed/locale/viewport and ordered assert/wait/click/key/frames/choose/command/capture
steps; inspect the generated smoke for a working example. The runtime validates
the schema. Input reaches the real game processor, not native OS event delivery.
Semantic controls and debug preconditions are distinct from player reachability.

Visual matrices, seeded exploration, trace comparison and bounded failure
reduction use the same client. Select their project specification paths explicitly;
there is no implicit scene, puzzle, baseline or GPU. Persistent overrides use a
separate explicitly enabled external store and preview/activate/restart boundary;
they do not replace canonical source edits. Preserve rollback/reset evidence.

## Interactive command completion

`verb.run` observes a verb until it finishes. If the verb waits for displayed text
or a dialogue choice, a scenario's synchronous command step cannot proceed to
advance that same interaction. Use actual input for an input acceptance test, or
use the `gameplay` dispatch operation for a semantic test and then explicitly
wait for/advance text or select the offered choice. For example, a scenario
command with `operation: "gameplay"` and
`arguments: {"type":"actorVerb","actorId":"key","verb":"use","target":"gate"}`
returns after dispatch; assert the resulting inventory/actor state separately.
Dispatch success alone proves neither completion nor reachability. An interactive
client may alternatively submit `verb.run` without waiting, handle the interaction
and finally observe the original job's terminal result.

Inspect actual state before choosing assertion paths. The debug displayed-text
object stores its string under `/text/text`; `/text` is not itself a string.
Auto-hidden UI controls must first be revealed before a named-widget click.
Inventory gestures may require pointer movement between press and release; keep
observed input and final state evidence rather than claiming a semantic command
was a delivered mouse gesture.

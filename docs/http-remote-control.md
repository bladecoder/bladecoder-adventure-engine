# HTTP remote control

Blade Engine can expose a localhost-only HTTP service so that a development
tool or agent can control a running game. It never accepts connections outside
`127.0.0.1` and it adds no runtime dependency.

## Start and stop

On desktop, start the generated launcher with `-http` or `-http <port>`; the
default port is `8080`.

```sh
./gradlew desktop:run -PappArgs="['-w', '-http']"
```

The Debug screen has an equivalent button. The service stops automatically when
the game UI is disposed. Android games need the `android.permission.INTERNET`
permission; use `adb forward tcp:8080 tcp:8080` to reach the loopback service
from the development machine.

## API

Player responses are JSON; debug artifact retrieval can return PNG bytes. Commands are accepted into a FIFO queue and the request
waits until the render thread accepts or rejects the command. A command returns
`200` when dispatched, `409` when its game-state preconditions are not met, and
`504` if it cannot be processed within two seconds.

```sh
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/state
curl http://127.0.0.1:8080/events
```

`GET /state` contains the current scene, player, currently interactable actors
with their available verbs, inventory items, dialogue options and automation
state. `GET /events` returns the player-visible text, scene, dialogue,
cut-mode, pause, inventory and finite-animation events since the last successful
gameplay command. A `gameEnded` event unambiguously marks completion. Reading
events does not clear them.

Send commands to `POST /command` with `Content-Type: application/json`:

```sh
curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"actorVerb","actorId":"door","verb":"action"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"sceneVerb","verb":"goto"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"dialogOption","option":0}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"goto","x":320,"y":180}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"saveGame","target":"agent-save"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"screenshot","target":"agent-screenshot.png"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"newGame"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"loadGame","target":"agent-save"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"continue"}'

curl -X POST http://127.0.0.1:8080/command \
  -H 'Content-Type: application/json' \
  -d '{"type":"pause"}'
```

`actorVerb` accepts an optional string `target`. Invalid request schemas return
`400`; unknown routes return `404`; unsupported methods return `405`.

`screenshot` writes a 1920-pixel-wide PNG to the game's user-data directory.
`newGame` starts the initial chapter, `loadGame` loads its saved-game target,
and `continue` resumes the latest saved game (or loads the initial chapter if
there is no save). `pause` toggles the game's pause state.

Except for `newGame`, `loadGame`, and `continue`, commands require an active
game. A paused game only accepts `pause`; use it again to resume before sending
another command. State endpoints remain available in either condition.


## Explicit debug sessions (protocol v1)

The default server and `start(port)` remain player-only. Full inspection requires
`start(port, RemoteControlMode.DEBUG)` or, before application creation:

```java
engine.setRemoteControlPort(0); // Available port, debug mode only.
engine.setRemoteControlMode("debug");
engine.setRemoteControlDiscoveryFile("/absolute/new/private/connection.json");
```

These methods are on `BladeEngine`; a game's custom launcher must forward its
arguments to them. Bonasera's launcher supplies `-http [port]`,
`-http-mode player|debug` and `-http-info NEW_FILE`. The engine's legacy generated
launcher still provides `-http`; it does not automatically gain a game's custom
argument parser. The logging flag `-d` grants no remote debug access. No HTTP
server starts without explicit activation, and no editor classes are required.

Discovery creates a new file (0600 on POSIX) containing protocol, session ID,
mode, loopback URL and, for debug only, a random bearer token. Never overwrite
an existing discovery file or print/commit its token. Debug port 0 selects an
available port; legacy player mode retains default 8080 and ports 1–65535.
A restart establishes a new identity. The server cannot change from player to
debug through an HTTP request. All debug requests require
`Authorization: Bearer <token>`; `/debug/v1/...` returns 404 in player mode.

Legacy routes retain their player projection even in a debug session. Hidden
actors and targets are unavailable to gameplay commands, internal scene verbs
are rejected (scene `goto` remains supported), save/screenshot names must be
plain basenames, and health failures do not expose internal exception text.
Inventory commands prefer an acquired item over its same-ID hidden scene origin.
The portable player-only workflow is `adventure-setup/ai-toolkit/skills/adventure-player`.

### Debug resources

| Method and path | Result |
| --- | --- |
| `GET /debug/v1/session` | Protocol, session/mode, capabilities/limits, loaded engine location, embedded version/source SHA-256 and active save directory. |
| `GET /debug/v1/state` | Game-thread snapshot with frame/event cursor, current and loaded-model scenes/actors, inventory, typed Ink variables/flows/runners, text/choices, animation/camera/geometry, music, diagnostics and serialized STATE. |
| `GET /debug/v1/state?path=/ink/variables` | Focused JSON pointer query; response contains `path` and `value`. |
| `POST /debug/v1/commands` | Accept an asynchronous command, normally HTTP 202. |
| `GET /debug/v1/commands/{id}` | Observe a command in this session. |
| `POST /debug/v1/commands/{id}/cancel` | Request cancellation of pending observation/remaining steps. Already performed gameplay is not rolled back. |
| `GET /debug/v1/events?after=N&wait_ms=T` | Nondestructive sequence stream, next/earliest cursor and explicit loss flag; maximum wait 20 seconds. A future cursor returns 409. |
| `GET /debug/v1/artifacts/{id}` | Session-owned PNG by opaque ID, never an arbitrary filesystem path. |

Snapshot reads and GL/world mutations run on the render thread. A stalled render
thread produces an unavailable result; it does not make up a successful state.
`scene_generation` can guard command arguments against a stale loaded scene.
Choice commands require the current `choice_set` revision, renewed when dialogue
options change. Unloaded resources and unavailable source-line metadata are not
synthesized. Atlas frame/canvas/offset information uses a public renderer API;
Spine renderer type/current animation/available animations share the same snapshot.

### Commands and completion

```json
{
  "request_id": "inspect-door-1",
  "operation": "verb.run",
  "arguments": {"actor": "door", "verb": "lookat"},
  "timeout_ms": 10000
}
```

Results include session/request/command identities, operation, status and result
or structured error. Status is `accepted`, `running`, `completed`, `failed`,
`timed_out`, `cancelling` or `cancelled`. Polling does not execute the command
again. An identical request ID and exact body retrieves the original command;
reuse with a different body returns 409. Clients should serialize retries
consistently and never retry mutations against a new/unverified session. History
is limited to 1,024 commands with refusal on overflow, not silent eviction.
Deadlines are real-time, 1–60,000 ms. Timeout/cancellation stops waiting and any
remaining controlled frame steps; it does not undo world actions or cancel an
already running verb. Inspect the resulting state. `gameplay` acknowledges
legacy dispatch, while `verb.run` follows the available underlying verb runner.

| Operation | Arguments |
| --- | --- |
| `fixture.scene` | `chapter`, `scene`; explicitly load entry state, then wait for asset readiness. |
| `fixture.variable` | `domain: engine|ink`, `name`, `value`; engine values are strings/null, Ink values preserve supported primitive types. |
| `fixture.actor` | `actor`, optional `scene`, `visible`, `x`, `y`, `scaleX`, `scaleY`; finite values required. |
| `animation.play` | `actor`, declared `animation`. |
| `verb.run` | `actor`, `verb`, optional `target`; use available normal gameplay verbs. |
| `dialogue.choose` | Current `choice_set`, zero-based `option`. |
| `dialogue.advance` | Advance currently displayed text. |
| `gameplay` | Legacy command body, including movement, pause, save/load. |
| `wait` | 1–32 `conditions`, each `{path: JSON_POINTER, equals: VALUE}`. |
| `clock.freeze` | Boolean `value`; wait/cancel a pending step before changing it. |
| `clock.step` | 1–600 `frames`, `delta` in (0, 0.1]; freeze first. |
| `capture` | `kind: scene|ui`; capture after a fresh render, return hash/length/dimensions, frame, cursor and associated snapshot. |

Fixtures establish debug preconditions and are distinct from player actions.
Stepping controls world/UI delta only: asynchronous IO, platform timers, audio
and all random sources are not made deterministic by this feature. A wait uses
explicit predicates; looping background animation does not prevent readiness.

### Timeline, limits and verification

The independent debug timeline persists across legacy player commands. Events
include sequence, frame, simulation time, type, data and command correlation
when synchronous; runner IDs link subsequent action events to their origin.
It records text/speakers, choice text/revisions, raw Ink lines/paths/tags, parsed
Ink commands, variable changes, action start/wait/completion, logs, animation
and audio requests. Source action types/IP/flow are available; compiler line
maps and complete external-function traces are not supplied. Audio request
logging does not prove audible output. Read-only observers coexist with UI
callbacks; logs and internal diagnostics never enter the player event stream.

The service bounds headers (64, line 8 KiB), request bodies (64 KiB), workers
(8, queue 32), concurrent long polls (4), events (2,048) and PNG artifacts (128).
Sockets time out while reading requests. Event loss is explicit; continuous
observers should persist their own transcript and resynchronize after a gap.
Artifact files remain in the game's user directory under `automation/<session>`;
retention/cleanup belongs to the managed profile or caller. Debug access grants
internal control to its local token holder; protect discovery files.

Run `./gradlew :blade-engine:remoteProtocolTest` for the standalone parser/mode/
event-buffer checks. They use a dedicated `remoteProtocol` source set rather
than JUnit discovery, and run automatically through `check` and `build`.
Java changes also require `./gradlew spotlessApply`.
Bonasera's shared HTTP client/scenarios verify real desktop rendering, P01,
player/debug isolation and assertion/hang/crash evidence from a standalone JAR.
Desktop verification does not establish Android/iOS, UI pointer hit-testing,
full deterministic replay, audio listening or persistent content patches.
The in-game inspector and validated external content overrides are later work.


## Renderer-specific pose diagnostics

`ActorRenderer.getDebugState()` is an optional read-only game-thread hook. The
full debug snapshot places its result under each actor's `renderer.details`;
legacy player DTOs do not expose it. The core engine does not import plugin
classes. The Spine plugin reports loaded skeleton version/source/atlas, skin,
original animation names, skeleton-space bone positions/rotations, slot
attachments in applied drawing order, track times/durations/loop/completion,
mirroring and the current alpha convention. Unloaded skeletons report
`loaded: false`. Actor/camera transforms remain separate from skeleton space.

The animation command now uses the renderer's supported directional lookup,
including its existing right/left mirroring. Pose diagnostics and animation
availability checks do not change the selected direction while observing.
Bonasera's phase 5 suite uses these shared hooks for a template-derived character,
real movement/Say actions, timed captures and save/load; no editor or reflection
is involved in runtime observation.

## Input and repeatability fixtures (phase 7)

These capabilities are debug-only and run on the game thread. `input.event`
accepts `mouseMoved`, `touchDown`, `touchDragged`, `touchUp` (integer `x`,`y`,
with `button` 0–2 for down/up), `keyDown`/`keyUp` (`keycode`), `keyTyped`
(one `character`), `scrolled` (`amountX`,`amountY`), or `reset`.
Coordinates are logical window pixels, top-left origin. One synthetic pointer
is supported. A down is required before dragging/up; invalid coordinates and
held-state transitions are rejected before mutation. The session input adapter
routes events through the active InputProcessor, including Stage and gesture
hit-testing, and matches Gdx.input polling state. It does not move the OS cursor.
Real input takes over, and stopping debug restores the current backend processor.
This tests application input routing, not native OS input delivery. Handled input
is recorded separately from assertions about game effects.

The snapshot's `ui` domain includes active screen, actual locale, logical window
size, widget names/paths/text/visibility/bounds/hit centers, current widget/scene
hit, projected actor bounds, inventory tile bounds and current dragged item.
Widget paths are diagnostic hierarchy indices; stock control names are preferable
selectors across layouts/locales. Geometry can change after scrolling/animation;
re-read and verify the intended effect. Focused `/state?path=/ui` queries avoid
full-world serialization. Widget snapshots are capped at 800 rows with explicit
truncation. Unsupported custom UI details are not synthesized.

`fixture.seed {seed: INT}` reseeds MathUtils and the loaded Ink story through its
public save-state API; a subsequent fixture.scene applies the declared seed to
the new story. `ink.random` reports the actual story seed/previous random value.
`fixture.locale {locale: IETF_TAG}` changes the game's bundle locale; set it before
entering a route and reopen menus to rebuild their translated widgets.
`fixture.viewport {width: INT,height: INT}` resizes a desktop window and completes
only after the requested logical dimensions are observed. These are explicit
setup operations. World stepping/seeds do not fix native audio, resource IO or
OS timers. Save/load retains saved Ink RNG state; it is not silently reseeded.

## Explicit content store and runtime inspector (phase 7 in progress)

A custom launcher can call `setContentOverrideDirectory(PATH)` before creation;
Bonasera supplies `-content-overrides PATH`. No store is scanned by default.
The resolver loads one validated external generation. Version-1 manifests declare
an id and files with packaged base/replacement SHA-256 hashes. Initial supported
resources are existing actor scalar model fields in chapter JSON and compatible
compiled Ink JSON. Structural/identifier/action-graph edits, translation-key
changes and arbitrary media/code are rejected. Preview returns explicit scalar
diffs; Ink compatibility uses the actual declared Story loader.

Debug commands `content.status`, `content.preview {directory: ABSOLUTE_PATH}`,
`content.activate {directory: ABSOLUTE_PATH}`, `content.export`, `content.rollback`
and `content.reset` manage this store. Activation copies/validates a complete
immutable generation and atomically selects it. Activation/reset/rollback are
restart boundaries; they do not mutate the current live resources or preserve
flows across reload. Status separates loaded and next-start generation. Invalid
startup selection is reported and uses the packaged base without partial load.
Generations/selection are separate from packaged content and savegames.

`inspector.open` opens the shipped RuntimeInspectorScreen; the debug menu exposes
an equivalent control. A running player controller must be stopped before a
user explicitly starts debug inspection. The in-game facade reads snapshots on
the game thread and submits commands through the same validation/queue used by
HTTP, without a blocking loopback call. Its views cover state, events, dialogue,
active actions and content; capture and command receipts remain asynchronous.
Menu navigation may have paused gameplay; the actual paused state is visible.

`inspector.export` or the UI Export button writes session-owned evidence and
returns a ZIP artifact id, SHA-256, length and media type. The existing artifact
endpoint now returns `application/zip` for these exports. The archive retains
state, events, command arguments/results, build/session identity and indexed
captures, without discovery tokens or personal saves. PNG retention is capped at
64 MiB with retained/skipped entries; total exported input is capped at 80 MiB.
The UI display has an explicit text limit, separate from exported evidence.
Broader source-trace/workflow coverage remains phase 7 work; shared scenario replay is described below.


## Shared scenario replay and visual context

`scenario.run {scenario: VERSION_1_OBJECT}` accepts the same declarative scenario
used by the project client and the inspector Replay filename control. Validate
all fields/step IDs before setup; one replay is active per session. Setup declares
chapter/scene/seed/locale/viewport. Steps support commands, typed assertions,
condition waits, captures, real input clicks/keys, controlled frames and offered
text-selected choices. JSON-pointer predicates distinguish missing from null;
`all`/`any` combine 1–32 predicates at up to four nesting levels.

The ordinary command queue owns parent and child jobs. Request deduplication
covers setup, partial results retain failed step/assertions and exports retain
the exact scenario. Parent cancellation/deadline cancels child observation and
remaining steps without rolling back gameplay. The existing 60-second parent
limit and 1,024-command session bound still apply; chunk long routes/checkpoints.
Interactive actor verbs should use dispatch followed by explicit waits/choices;
completion-waiting a conversation before selecting a choice can hit its deadline.

UI snapshots/capture states now include `render_environment`: backend/application,
GL version/vendor/renderer, backbuffer dimensions/scale, density/PPI and buffer
format. `fonts` lists registered skin font metrics/source where available; project
visual tooling also pins packaged source-font hashes. Scene capture dimensions
can differ from logical window dimensions. These fields support explicit
compatible-environment comparisons, not universal pixel equivalence.

LWJGL window resizing can restore the backend's original global `Gdx.input`.
The debug session restores its adapter before UI rendering, adopts direct backend
processor changes and reinstalls physical-event forwarding. Normal inactive/player
sessions do not install this adapter. Synthetic coordinates remain independent of
the OS cursor. The regression includes resize/reset routing and restoration.


Capture completion also persists an adjacent JSON receipt containing hash, byte
length, frame/cursor and full captured state in the session-owned artifact
folder. This allows a local supervisor to recover prior evidence if the render
thread exits before HTTP download. It is not a personal-save search or a promise
that a crashed process can provide a fresh screenshot. Project recovery must
validate the owned isolated profile, exact session UUID, bounded files and hash,
and preserve the original error when final observation is unavailable.
InkRunAction retains the caught runtime exception alongside its existing error
message, so bad Ink-path diagnostics include the actual exception stack.


The packaged runtime inspector includes a JSON-pointer field relative to the
selected state/events/dialogue/actions/content/scenarios view. Refresh applies
that read-only selection before display truncation; an empty pointer shows the
whole view. Missing paths, malformed escapes and invalid array indices report
errors; explicit null values remain valid. Export retains the complete backend
snapshot regardless of the displayed selection. Current Ink runner snapshots
include runtime action source references where available; legacy save-restored
actions without recorded source metadata remain unmapped.

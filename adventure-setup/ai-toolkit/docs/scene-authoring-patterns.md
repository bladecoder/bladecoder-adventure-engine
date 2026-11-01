# Reusable scene and inventory patterns

These conventions were checked against Bonasera 5's chapter and atlases and the
selected engine MODEL reader. Reuse the structure, not its characters, dialogue,
IDs, sound files or animation names. Examples here are templates whose resources
must exist in the target project.

Runtime checks described below follow the [opt-in QA policy](qa-workflow.md).
Without an explicit QA request, inspect sources/exports and validate references;
leave gameplay, movement, persistence and camera acceptance as not_run.

## Scene resources, player and origin

Pack a scene's background and local sprites in `raw/images/<atlas_group>/` and ship
`assets/atlases/1/<atlas_group>.atlas` plus its pages. Each character has its own
atlas containing all of that character's frame animations, reused across scenes.
A Spine character likewise has its own atlas with all cutout attachment images,
generated with its skeleton/rig export. Keep character regions out of scene
atlases and avoid a mixed-character atlas by default. Atlas grouping reduces texture switching;
do not turn every exported PNG into an independent `ImageRenderer` by default.
Normally the group matches the scene ID; normalize unsupported packer characters
and document the mapping (Bonasera's literal scene `00.bank-ext` uses `bank-ext`).
Use the mapped group in the shipped atlas filename too. One logical atlas may
have multiple texture pages; preserve indexed regions for tiled wide backgrounds.

Declare `backgroundAtlas: "<scene_id>"` and `backgroundRegionId: "background"`
on the scene. Do not duplicate that background as an actor. The scene background
sets scene dimensions/camera bounds and draws before layers. Preserve background
canvas dimensions and atlas original-size/offset metadata when packing.

Set `player: "Aroa"` on each playable scene and provide its `CharacterActor` in
that scene. The playable character selection serializes to this scene-level actor
reference; an actor-level `player: true` is not a supported MODEL field in the
reviewed engine. Verify `$PLAYER`, walking and getPlayer() in the runtime.

Use renderer `orgAlign: 4` (`Align.bottom`, bottom center) by default, especially
for characters and inventory icons. Actor positions then anchor their feet/base;
scaling and walking preserve that ground contact. Other origins require a stated
placement reason and adjusted coordinates/bounds. JSON stores an integer, not
the string `"bottom"`. Inspect transparent padding, original atlas size and
trim offsets as well as visible pixels. Do not fix an origin by shifting an
image until it happens to fit one screenshot.

Layers are front-to-back (`foreground`, `dynamic`, `background`); dynamic layers
sort actors for depth. Use a walk zone for reachable ground, an optional
depthVector for perspective scaling, and each target's relative `refPoint` for
the place the player's feet should reach. Ground coordinates differ from SVG
top-left coordinates. Anchor actors are useful for exits, staging and animation
positions. Use state-qualified verbs `id[.target][.state]` for state-dependent
interactions; retain the matching action/animation/resource identities.

## Character animation defaults and hit regions

Provide the engine's `stand`, `walk` and `talk` animation families (with `.right`
and mirrored `.left` where directional). Do not assume MODEL fields named
`standAnim`, `walkAnim` and `talkAnim` configure them: the reviewed CharacterActor
reader loads those overrides only in STATE mode. Custom names require a supported
runtime `SetActorAttr` action (`standAnimation`, `walkAnimation`, `talkAnimation`)
at the appropriate entry, with persistence considered. Map keys, descriptor IDs
and atlas region names must agree; do not invent aliases with mismatched IDs.
Verify walking completion as well as an explicit idle animation.

Generated sprite sheets may overrun a nominal grid even when the prompt asks for
regular cells. Review every extracted frame, preserve complete gestures and use
explicit per-row cell bounds when needed. Keep the shared scale/ground reference.
Retain the original sheet and extraction recipe; do not hide clipping with a
smaller interactive box. Interaction bounds are a separate concern: account for
transparent canvas padding and check both initial and puzzle-completed layouts.
A moved prop must not steal clicks from the next required target. When runtime
QA is requested, exercise the actual UI after each state change.

## Player-facing verbs and cut mode

Wrap each player-facing interaction in `Cutmode(value=true)` before its first
action and `Cutmode(value=false)` after its last completed action. This includes
pickup, examination, conversation, inventory use/combine and exits, including
world fallback responses. Use the project's real verb IDs: the engine commonly
uses `lookat` for examination (sometimes called lookup in design notes), and
`dialog` or `talkto` for conversation. Do not rename existing IDs merely to match
this vocabulary. Cover state/target-qualified variants as well.

```json
[
  {"class": "Cutmode", "value": true},
  {"class": "InkRun", "path": "crossing.rogelio", "wait": true},
  {"class": "Cutmode", "value": false}
]
```

Keep finite movement, interaction animations, text and Ink execution awaited
(`wait: true`) so the closing action does not release input while the work is
still running. Returning to an idle/stand pose may be nonblocking. Branches and
rejected/repeated attempts must converge on cleanup; do not add an early return
that bypasses it. Initialization/load/system verbs are separate entry points,
not automatically player-facing interactions.

Cut mode is a global boolean, not a nesting counter. One outer interaction owns
the pair. Helpers called with `RunVerb(wait=true)` from that interaction inherit
the lock and must not issue an unconditional `Cutmode(false)` while their caller
still has work. Use a protected public wrapper plus an unprotected internal helper
when both standalone and nested entry are needed; document that ownership.

`Leave` is terminal in the reviewed engine: it changes scenes and does not resume
the source verb's trailing actions. Keep the exit's explicit cut-mode boundaries,
but do not rely on the trailing false for cleanup. A destination `init`/selected
entry verb owns its own true/false pair and finishes the handoff; the engine also
resets cut mode during initialized scene startup. If destination entry contains
text/animation, keep its final release after that work. Audit every destination
and custom `initVerb`, including transitions to an already visited scene.

Ink temporarily leaves cut mode to present choices, remembers the previous value
and restores it when a choice is selected. Keep the outer `InkRun(wait=true)`
wrapper and let Ink manage this modal choice phase. Do not scatter false commands
through stitches or unlock to permit another actor verb. Text advancement and
choice selection are continuations of the active interaction. When runtime QA is
explicitly requested, verify input blocking, continuations and the final release;
without that request, inspect structure/waits and record runtime behavior as not_run.

## Scene exits, background hotspots and doors

Use a public `leave` verb for a scene exit, including state-qualified variants
such as `leave.locked`. The default UI recognizes this verb, chooses an exit
arrow and dispatches `leave` when the player clicks the hotspot. A `Leave` action
hidden only in `pickup`/`use` does not declare that affordance. Do not add a
separate arrow/sign sprite just to make a passage clickable. The engine can infer
arrow direction from the hotspot; optional `icon: "left"`, `"right"`, `"up"` or
`"down"` selects a direction explicitly. A custom drawable icon must actually
exist in the skin. A locked `leave` variant may still show an arrow while giving
a guarded refusal; disable interaction instead only when the design calls for it.

The editor's **background** actor type serializes as `class: "InteractiveActor"`
without a renderer, not `BackgroundActor`. It represents a clickable polygon over
something already painted into the scene background. Actor type and render layer
are distinct: choose the layer for hit priority/occlusion (often `background`),
and keep `visible: true`, `interaction: true`, an explicit `bbox` and `verbs`.
Position the polygon over the real doorway/path. Its relative `refPoint` gives a
reachable floor/threshold position for the player's feet, which may lie outside
the door's painted bounds. Avoid competing hotspots over the same passage.

A typical open passage is:

```json
{
  "class": "InteractiveActor",
  "id": "exit_to_plaza",
  "pos": {"x": 940, "y": 100},
  "bbox": [-20, -35, 20, -35, 20, 100, -20, 100],
  "refPoint": {"x": -30, "y": 0},
  "visible": true,
  "interaction": true,
  "layer": "background",
  "zIndex": 0,
  "desc": "<localized destination name>",
  "verbs": {
    "leave": {
      "id": "leave",
      "icon": "right",
      "actions": [
        {"class": "Cutmode", "value": true},
        {"class": "Goto", "actor": "$PLAYER", "target": "exit_to_plaza", "ignoreWalkZone": false, "wait": true},
        {"class": "Leave", "scene": "plaza", "init": true, "initVerb": "init_from_crossing"},
        {"class": "Cutmode", "value": false}
      ]
    }
  }
}
```

These are illustrative IDs/coordinates; adapt them to the actual layout and
walk zone. Keep cut mode around the complete awaited walk and interaction, even
though some legacy Bonasera examples acquire it only after walking. Use
`ignoreWalkZone: false` normally; a short off-screen traversal may deliberately
ignore it only after a reachable approach and with a stated geometry reason.

For a closed door, the usual order inside the outer cut-mode pair is:

1. Check access/state and walk to the threshold/handle (`Goto`, `wait: true`).
2. Play the player's existing handle/reach gesture when needed.
3. Play an existing latch/open sound when selected and registered; animate the
   actual door opening with a finite `Animation`, `wait: true`. Change its engine
   state before leaving when persistence requires it.
4. `Leave` to the destination, selecting its entry verb where appropriate.

A moving door needs a `SpriteActor`/renderer and real open/closed animations in
the scene atlas. It may own `leave` itself or be driven by the background exit
hotspot; never run a door animation on a renderer-less InteractiveActor. A door
already painted open needs no invented opening animation. Keep a blocked branch
inside the same protected interaction and release it after its feedback. Do not
reference nonexistent placeholder animations or sounds merely to match this list.

`Leave` is terminal: the source's trailing `Cutmode(false)` is an explicit
boundary, not executable destination cleanup. Use the ownership/handoff rule
above. The selected destination entry typically does:

1. `Cutmode(true)`.
2. Required scene/player setup, then `Position(actor=$PLAYER, target=<return_exit>)`
   to use that exit's threshold/refPoint (or an explicit arrival anchor).
3. An optional arrival gesture/door close using existing assets, and an awaited
   short `Goto` into clear walkable space.
4. `Cutmode(false)` after arrival work.

`Leave.initVerb` selects the entry instead of the default `init`; include any
needed common setup without nesting another helper's unconditional cut-mode
release. Use literal source/destination IDs and entry names consistently. An
`IfProperty` branch on `PREVIOUS_SCENE` in `init` is another established approach.
`Position` uses field `position`; `Goto` uses `pos` when an explicit coordinate is
needed. Do not place essential entry work only after the terminal source Leave.

After edits, validate destinations, entry verbs, polygons, reference points,
waits and real door resources. Update declared route/scenario callers when an
exit ID or route changes. In explicitly requested QA, check the native arrow,
click reachability, walk/door ordering, blocked variants, both directions,
revisited scenes and the final release of cut mode. Otherwise leave runtime
acceptance as `not_run` under the opt-in policy.

## Inventory and classic pickup

Create a non-playable support scene `inventory` with `verbs: {}` and all inventory
icon actors. Their `AtlasRenderer` uses the separate `inventory` atlas and a
consistent icon canvas/reference point. Each icon owns inventory look/use/combine
verbs. The world object is a different actor, normally with the same item ID in
its own scene; it owns world inspection and acquisition. Picking the inventory
actor removes it from the support scene. Hide the world representation explicitly.
Do not transfer a world-sized sprite with scene-only verbs into the inventory.

For an ordinary reachable object, the reusable acquisition sequence is:

1. `Cutmode(value=true)`.
2. `Goto(actor=$PLAYER, target=<world_item>, wait=true)`; target `refPoint` must
   put feet on reachable ground, and respect the walk zone.
3. `Animation(animation=$PLAYER#pickup, wait=true)` with an existing forward pose.
4. `SetActorAttr(actor=<world_item>, visible=false)` and set any engine-owned
   acquisition state needed to prevent repeats.
5. `PlaySound(sound=pickup)` only if that sound is selected and declared.
6. The same `Animation` with `repeat=REVERSE`, `wait=true`.
7. `Animation(animation=$PLAYER#stand, wait=false)`.
8. `PickUp(actor=inventory#<item>)`.
9. `Cutmode(value=false)`.

This complete sequence assumes the pickup owns the outer interaction. When it
is an internal acquisition helper called from an already protected Ink verb,
use steps 2–8 in the helper and keep steps 1/9 in the public caller. In particular,
do not unlock between acquiring the icon and the caller's remaining dialogue.

Use actual project animation IDs, including direction when needed. A container
may change its animation/state instead of disappearing. A dummy prototype may
use a small declared placeholder pickup animation and omit unselected audio;
record that limitation rather than referencing missing production resources.
Guard acquisition by authoritative inventory/visibility/state, including when
dispatched from Ink. For gated items, Ink can decide permission and run a named
MODEL acquisition verb, keeping physical movement/animation/visibility in MODEL.
Do not rerun the transfer when the inventory already contains the item.

When consuming/returning an item, choose the destination support scene explicitly
and restore the world representation only when the story calls for it. Verify
world visibility, icon size/alignment, inventory use verbs, repeat attempts,
scene changes and isolated save/load. Keep cut mode balanced even on a rejected
attempt; validate animation waits, reverse completion and return to stand.

## Reference observations

Bonasera 5 uses `backgroundAtlas`/`backgroundRegionId`, a scene-level `player`
reference, `orgAlign: 4`, per-character atlases reused across scenes, an `inventory` support scene,
state-qualified verbs, background exit polygons with `leave`, and named reusable scene verbs. Its backpack pickup changes
container state, walks the player, plays pickup/reverse/stand and obtains an
`inventory#...` icon. Some examples acquire the icon before the reverse pose;
the sequence above is this toolkit's default, not a claim that every reference
verb uses an identical order. Its dotted scene IDs are literal IDs, not chapter
separators. Do not copy legacy numeric IDs, localized text keys or save STATE.

Bonasera exit examples include `safe/exit` (renderer-less background-layer
InteractiveActor and `leave`), `bank-int/to-office` (Goto then Leave),
`bank-int/to-cleaning` (player reach/reverse before Leave), and
`office/exit` plus `bank-int/init-from-office` (specific destination arrival).
Some legacy examples use foreground hotspots, omit an outer lock or disable a
Goto. Reuse the intended passage/entry structure, not those incidental omissions.

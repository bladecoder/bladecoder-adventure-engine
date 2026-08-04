# Bladecoder action serialization

Use this reference only when adding or changing verb actions. Existing actions in the target game and source for its exact engine/plugin version are authoritative.

## Serialization rules

Each action is an object with a `class` tag and fields annotated with `@ActionProperty` in the Java class:

```json
{"class": "Goto", "actor": "$PLAYER", "target": "door", "wait": true}
```

- Built-in tags come from `ActionFactory`; the normal tag is the Java class name without the `Action` suffix.
- Explicit overrides include `ActorState`, `Comment`, `Cutmode`, `IfActorAttr`, `RotateAnim`, `ScaleAnim`, `ScaleAnimXY`, `SceneState`, and `SetWalkzone`.
- Unknown tags are loaded as fully qualified Java class names. Preserve plugin/custom names exactly.
- Null fields are normally omitted. Do not add defaults merely to make an object look complete.
- `SceneActorRef` and `ActorAnimationRef` are strings. `Vector2` action parameters are `"x,y"` strings, and colors are serialized strings.
- Fields not annotated with `@ActionProperty` do not belong in model JSON.
- Required values and defaults can change by engine version. Copy a matching action or inspect the exact Java class before relying on a default.

## Current built-in registry

The current engine registers these tags:

`AlphaAnim`, `Animation`, `Camera`, `CancelVerb`, `Choose`, `Comment`, `DisableAction`, `DropItem`, `End`, `EndGame`, `Goto`, `IfActorAttr`, `IfInkVariable`, `IfProperty`, `IfSceneAttr`, `InkNewStory`, `InkRun`, `InkVariable`, `Leave`, `LoadChapter`, `LookAt`, `MoveToScene`, `Music`, `MusicVolume`, `OpenURL`, `PickUp`, `PlaySound`, `Position`, `PositionAnim`, `Property`, `RandomPosition`, `RemoveInventoryItem`, `Repeat`, `RotateAnim`, `RunOnce`, `RunVerb`, `Say`, `SayDialog`, `ScaleAnim`, `ScaleAnimXY`, `ScreenPosition`, `SetAchievement`, `SetActorAttr`, `Cutmode`, `SetDialogOptionAttr`, `SetPlayer`, `SceneState`, `ActorState`, `SetDesc`, `SetWalkzone`, `ShowInventory`, `Sound`, `Talkto`, `Text`, `TintAnim`, `Transition`, and `Wait`.

Do not assume every game can load every current tag: compare the game's configured engine version and dependencies.

## Common action shapes

Fields shown as optional may still have behavior-changing defaults. Preserve the target game's convention.

| Tag | Principal fields |
| --- | --- |
| `Animation` | `animation` (actor-animation ref), `count`, `wait`, `repeat`, `keepDirection` |
| `Goto` | `actor`, optional `target` or `pos`, `ignoreWalkZone`, `wait` |
| `Position` | `actor`, optional `target`, optional `position` |
| `PositionAnim` | `actor`, optional `target`/`pos`, `speed`, `mode`, `count`, `wait`, `repeat`, optional `interpolation` |
| `ScreenPosition` | `actor`, `position`, optional `anchor` |
| `Camera` | optional `target`, `pos`, `zoom`, `duration`, `followActor`, `interpolation`; `wait` |
| `Say` | `actor`, optional `text`, `voiceId`, `animation`, `style`; `type`, `queue`, `wait` |
| `Text` | `text`, optional `voiceId`, `style`, `color`, `target`, `pos`; `type`, `queue`, `wait` |
| `LookAt` | optional `actor`, `text`, `voiceId`, `direction`; `wait` |
| `Talkto` | `actor`, `dialog` |
| `SayDialog` | `wait`; the current dialog and selected option supply both speakers and texts |
| `RunVerb` | optional `actor`, `verb`, `wait` |
| `CancelVerb` | optional `actor`, optional `verb` |
| `Leave` | `scene`, `init`, optional `initVerb` |
| `MoveToScene` | `actor`, optional `scene` |
| `LoadChapter` | `chapter`, optional `scene` |
| `PickUp` | `actor`, optional `animation`, `inventory` |
| `DropItem` | optional `actor`, `scene`, `pos`, `inventory` |
| `RemoveInventoryItem` | optional `id`; required destination `scene` |
| `ShowInventory` | `value` |
| `SetPlayer` | `actor`, optional `inventory` |
| `ActorState` | `actor`, optional `state` |
| `SceneState` | optional `scene`, optional `state` |
| `SetDesc` | `actor`, optional `desc` |
| `SetWalkzone` | optional `actor` |
| `SetActorAttr` | `actor` plus only the attributes to change, such as `visible`, `interaction`, `layer`, `scale`, `tint`, animation names, or `walkingSpeed` |
| `SetDialogOptionAttr` | `actor`, `dialog`, `option`, optional `visible` |
| `Property` | `name`, optional `value` |
| `IfProperty` | `name`, optional `value`, control callback field such as `caID` |
| `IfActorAttr` | `actor`, `attr`, optional `value`, control callback field such as `caID` |
| `IfSceneAttr` | optional `scene`, `attr`, optional `value`, control callback field such as `caID` |
| `IfInkVariable` | `name`, optional `value`, control callback field such as `caID` |
| `InkRun` | `path`, optional comma-separated `params`, optional `flow`, `wait` |
| `InkNewStory` | `storyName` |
| `InkVariable` | `name`, `value` |
| `Music` | optional `filename`; `loop`, `initialDelay`, `repeatDelay`, `stopWhenLeaving`, `volume` |
| `MusicVolume` | `volume`, `duration`, `wait` |
| `PlaySound` | `sound`, optional `stop` |
| `Sound` | `actor`, optional `play` sound ID |
| `Wait` | `time` |
| `Transition` | `time`, `color`, `type`, `wait` |
| `Cutmode` | `value` |
| `EndGame` | no parameters |

Animation modifiers (`AlphaAnim`, `RotateAnim`, `ScaleAnim`, `ScaleAnimXY`, and `TintAnim`) reference an actor and contain a target value plus duration/speed, count, wait, repeat, and sometimes interpolation. Confirm exact field names from a same-version example.

## Control and serialized actions

`Choose`, `Repeat`, `RunOnce`, and the `If*` actions control subsequent actions in the same ordered array. Their matching `End` actions and callback IDs are structural. Copy a complete, working block from the same engine version and change only the predicate/body.

`DisableAction` stores another action in `serializedAction` as an escaped relaxed-JSON string. Generate its inner value from an actual serializable action shape; do not substitute a normal nested object.

## Plugin and custom actions

Before adding a qualified action:

1. Confirm the game declares the plugin/module or custom class.
2. Find an existing chapter instance or inspect its Java fields annotated with `@ActionProperty`.
3. Preserve the fully qualified `class`.
4. Use only annotated fields and exact enum/value spelling.
5. Validate by loading the chapter with that game's runtime classpath.

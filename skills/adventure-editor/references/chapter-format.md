# Bladecoder chapter model format

This reference describes model serialization (`BladeJson.Mode.MODEL`) in the current engine. The target game and its exact engine/plugin version remain authoritative.

## Contents

- Top-level chapter
- Scenes and layers
- Actors and renderers
- Verbs, dialogs, and references
- Localization
- Structural checks

## Top-level chapter

`<chapter>.chapter.json` is a LibGDX JSON object:

```json
{
  "bladeEngineVersion": "4.4.0",
  "sounds": {},
  "scenes": {},
  "initScene": "scene-1",
  "inkManager": {}
}
```

- `bladeEngineVersion`: version recorded when the model was saved.
- `sounds`: map from sound ID to `SoundDesc`.
- `scenes`: map from scene ID to `Scene`; each map key must equal the nested `id`.
- `initScene`: ID of a scene in `scenes`.
- `inkManager`: optional chapter Ink configuration/state written when a story is configured.

LibGDX accepts relaxed JSON with unquoted names, but editor-saved models commonly use strict JSON. Preserve the input dialect.

## Scenes and layers

A scene commonly contains:

```json
"street": {
  "id": "street",
  "layers": [
    {"name": "foreground"},
    {"name": "dynamic", "dynamic": true},
    {"name": "background", "parallax": 1.0}
  ],
  "actors": {},
  "backgroundAtlas": "street",
  "backgroundRegionId": "background",
  "musicDesc": null,
  "depthVector": {"x": 0, "y": 1},
  "sceneSize": {"x": 1920, "y": 1080},
  "verbs": {},
  "state": null,
  "player": "hero",
  "walkZone": "walkzone"
}
```

- `layers` is an ordered array. Layer fields are `name`, `visible`, `dynamic`, and `parallax`; defaults may be omitted.
- Every interactive actor's `layer` must name an existing layer.
- `backgroundAtlas` and `backgroundRegionId` are written together.
- `musicDesc` may include `filename`, `loop`, `initialDelay`, `repeatDelay`, `stopWhenLeaving`, and `volume`.
- `verbs` contains scene-level verbs such as initialization sequences.
- `player` references a character actor. `walkZone` references a walk-zone actor.
- `state`, `depthVector`, and `sceneSize` are optional.

## Actors and renderers

`actors` is a map. Preserve both the map key and nested `id`, for example:

```json
"door": {
  "class": "InteractiveActor",
  "id": "door",
  "bbox": [0, 0, 0, 100, 50, 100, 50, 0],
  "pos": {"x": 100, "y": 50},
  "visible": true,
  "desc": "@street.door.desc",
  "refPoint": {"x": 25, "y": 0},
  "verbs": {},
  "interaction": true,
  "zIndex": 0,
  "layer": "dynamic"
}
```

Actor inheritance adds fields cumulatively:

| Actor class | Important model fields |
| --- | --- |
| `AnchorActor`, `ObstacleActor`, `WalkZoneActor` | `id`, `bbox`, `pos`, `visible` |
| `InteractiveActor` | base fields plus `desc`, `state`, `refPoint`, `verbs`, `interaction`, `zIndex`, `layer` |
| `SpriteActor` | interactive fields plus `renderer`, `tint`, `scaleX`, `scaleY`, `rot`, `fakeDepth`, `bboxFromRenderer` |
| `CharacterActor` | sprite fields plus `dialogs`, `textStyle`, `textColor`, `talkingTextPos`, `walkingSpeed` |

Do not infer required geometry. Copy a same-class actor from the target game and replace its IDs, references, positions, and assets deliberately.

Common renderer tags are `AtlasRenderer`, `ImageRenderer`, `ParticleRenderer`, `TextRenderer`, and plugin-qualified tags such as `com.bladecoder.engine.spine.SpineRenderer`.

Animation renderers commonly contain:

```json
"renderer": {
  "class": "AtlasRenderer",
  "fanims": {
    "stand.right": {
      "class": "AtlasAnimationDesc",
      "id": "stand.right",
      "source": "hero",
      "duration": 1,
      "animationType": "REPEAT",
      "count": -1,
      "preload": true
    }
  },
  "initAnimation": "stand.right",
  "orgAlign": 4
}
```

The `fanims` map key must match the animation `id`. Plugin renderers and animation descriptions may require fully qualified classes and extra fields such as `skin`.

## Verbs, dialogs, and references

Both scenes and interactive actors contain a `verbs` map:

```json
"lookat": {
  "id": "lookat",
  "target": null,
  "state": null,
  "icon": null,
  "actions": [
    {"class": "Say", "actor": "$PLAYER", "text": "@street.door.lookat.0.text", "wait": true}
  ]
}
```

- Map key and `id` must match.
- `target` and `state` specialize verb selection and may be omitted.
- `actions` is executed in array order.
- Control actions use matching boundaries and callback IDs (`caID`) as found in same-game examples. Do not reorder or synthesize these structures without checking the matching engine implementation.

Character `dialogs` is a map:

```json
"greeting": {
  "id": "greeting",
  "options": [
    {
      "text": "@street.npc.greeting.0.text",
      "responseText": "@street.npc.greeting.0.responseText",
      "verbId": "answer",
      "next": "greeting",
      "once": false,
      "soundId": null,
      "responseSoundId": null,
      "visible": true
    }
  ]
}
```

Reference forms include:

- `actor`: current-scene actor ID, a special ID such as `$PLAYER`, or `scene#actor`.
- `animation`: `actor#animation`; an empty animation suffix may have plugin-specific meaning.
- `scene`, `target`, `followActor`, `player`, and `walkZone`: IDs whose expected type depends on the field/action.
- vectors in actor model fields: objects such as `{"x": 1, "y": 2}`.
- vectors in action parameters: usually serialized strings such as `"1.0,2.0"`.
- colors in action parameters: names accepted by the editor or RGBA strings.
- enums: serialized uppercase using the engine enum spelling.

`DisableAction` may contain `serializedAction`, a string holding relaxed JSON:

```json
{
  "class": "DisableAction",
  "serializedAction": "{class:com.example.CustomAction,actor:door}"
}
```

Search and update inside this string when renaming a referenced object.

## Localization

The base bundle normally shares the chapter stem:

```properties
street.door.desc=door
street.door.lookat.0.text=It is locked.
```

The chapter stores `@street.door.desc` and `@street.door.lookat.0.text`. Typical generated key shapes are:

- Actor description: `<scene>.<actor>.desc`
- Actor action text: `<scene>.<actor>.<verb>.<position>.<field>`
- Scene action text: `<scene>.<verb>.<position>.<field>`
- Dialog option: `<scene>.<actor>.<dialog>.<position>.<field>`

Existing games may retain older or hand-authored keys. Preserve them unless the requested rename requires coordinated key changes.

An `@` prefix is field-sensitive. In visible-text fields such as `desc`, action `text`, dialog `text`, and `responseText`, it denotes an i18n properties key. In an animation description's `source`, it can denote a localized asset filename instead; do not look that value up in `.properties`.

## Structural checks

For every edited chapter, verify:

- top-level `initScene` exists;
- scene, actor, verb, dialog, and animation map keys match nested IDs;
- interactive actor layers exist;
- `player` and `walkZone` exist and have compatible actor classes;
- touched action `class` tags and fields exist for the game's engine/plugins;
- touched actor, scene, animation, dialog, sound, inventory, and property references resolve;
- every touched text-bearing `@key` resolves in a chapter or applicable world bundle;
- control actions retain valid ordering and callback boundaries.

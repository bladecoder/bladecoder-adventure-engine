---
name: adventure-editor
description: Edit Bladecoder Adventure Engine game models directly from a game project folder, including creating, changing, renaming, or deleting scenes, actors, verbs, dialogs, and ordered actions in assets/model/*.chapter.json while keeping chapter localization properties consistent. Use when asked to modify a Bladecoder game's chapter structure or visible chapter text without using the graphical Adventure Editor.
---

# Bladecoder Adventure Editor

Edit the game model as source data. Work from the game repository, not from this skill's repository.

## Ground the edit

1. Read every applicable `AGENTS.md` before changing files.
2. Inspect repository status and preserve unrelated changes.
3. Locate `assets/model/*.chapter.json`, the matching `<chapter>.properties` bundles, and the game configuration that declares its Blade Engine version.
4. Identify the requested chapter, scene, actor, verb, dialog, and action from the model. If the request is ambiguous, present the concrete matches instead of guessing.
5. Inspect at least one analogous element in the same game before creating a new structure.

Treat sources in this order:

1. The target game's existing model and conventions.
2. Source for the exact Blade Engine or plugin version used by the game, when available.
3. The bundled references, which describe the engine version current when this skill was authored.

Read [references/chapter-format.md](references/chapter-format.md) before structural edits. Read [references/actions.md](references/actions.md) before adding or changing actions.

## Edit the model

- Make the smallest coherent edit. Use patch-based edits; do not round-trip an entire chapter through `jq`, a generic JSON formatter, or a new serializer.
- Preserve the file's existing strict JSON or LibGDX relaxed-JSON style, indentation, ordering, numeric representation, and class-tag style.
- Keep every scene map key equal to its `id`, every actor map key equal to its `id`, and every verb/dialog map key equal to its `id`.
- Preserve action array order. It is execution order, not cosmetic ordering.
- Copy the complete shape of the closest same-type actor, renderer, animation, dialog, or action, then change only fields justified by the request.
- Never invent a `class` tag or action field. Confirm it from the same game, the matching engine/plugin source, or the action reference.
- Preserve fully qualified plugin/custom class names. A class unknown to the built-in registry is loaded by name.
- Preserve special references such as `$PLAYER`, `$SCENE`, `scene#actor`, `actor#animation`, colors, vectors, enum spelling, and null-versus-omitted conventions.
- Treat `DisableAction.serializedAction` as an embedded LibGDX JSON value. Update both the wrapper and embedded references when the requested change affects it.

### Create, rename, or delete

- Before renaming or deleting an ID, search the complete game model for the old value, including chapter files, `world`, properties, Ink sources/output, tests, and embedded `serializedAction` strings.
- Update all in-scope references atomically. Check scene transitions, actor targets, animations, dialogs, inventory references, `player`, `walkZone`, and `initScene`.
- Do not leave a dangling reference. If a required update is outside the requested chapter-and-text scope, report the exact references and ask before expanding the edit.
- When adding an interactive actor, use an existing layer and include the fields required by its actor class. When adding a scene, define its layers before assigning actors to them.
- When deleting a localized object or action, remove only localization keys that are exclusively owned by it. Preserve shared or uncertain keys.

## Keep visible text synchronized

Use `@key` references for visible chapter text when the game localizes comparable text.

- Store the base value in the properties file whose stem matches the chapter.
- Follow the editor's key convention: scene, optional actor, verb/dialog, action or option position, and property such as `desc`, `text`, or `responseText`.
- Reuse an existing key when changing only its value. Generate a non-conflicting key when adding new text.
- Rename the key in every existing locale bundle when an ID/key rename requires it.
- Do not invent translations. Add or change locale-specific values only when the user supplies them or the request explicitly asks for translation; otherwise rely on the base-bundle fallback.
- Preserve UTF-8, existing property ordering, escaping, and `\n` conventions.
- Resolve `@` references in text-bearing fields against the chapter bundle and any applicable world bundle before calling them missing. Do not treat an animation `source` prefixed with `@` as a properties key; it marks a localized asset.

## Validate

1. Re-read the edited subtree and its surrounding commas/braces.
2. If the original file is strict JSON, require `jq empty <file>` to pass. If it uses LibGDX relaxed JSON, do not normalize it just to satisfy `jq`; use a project-native engine/editor load instead.
3. Check map-key/`id` equality, unique IDs, valid `initScene`, actor layers, `player`, `walkZone`, verb/dialog IDs, action classes, and every reference touched by the edit.
4. Verify every added or renamed text `@key` resolves in the base or applicable world properties and that locale bundles have not lost existing translations.
5. Run the checks required by the game's `AGENTS.md`. When a runnable validation path exists, load the edited chapter with the game or matching engine. If only static validation is possible, state that limitation.
6. Inspect the final diff for unrelated reformatting and unintended localization churn.

Report the changed gameplay behavior, files changed, validations run, and any references that could not be verified.

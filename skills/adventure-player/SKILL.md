---
name: adventure-player
description: Play a Bladecoder Adventure Engine game through its localhost HTTP controller and finish the adventure using only player-visible textual state and events. Use when asked to autonomously play, solve, explore, test-complete, or verify completion of a Bladecoder adventure exposed with -http.
---

# Bladecoder HTTP Adventure Player

Play as a player, not as an editor or a model inspector. Use `/state` to choose legal actions and `/events` to read what the player has seen since the last successful gameplay action. Do not inspect story/model files, custom properties, saves, or screenshots to discover puzzle answers.

## Start

Start the desktop game with the localhost controller enabled by passing these game arguments:

`-http [port]` enables the controller; omit the port to use `8080`. Add `-w` to start the game windowed when that is useful.

Use these arguments whether the game is launched through Gradle, Java, or a generated executable.

1. Confirm the controller is live with `GET /health`.
2. Read `GET /state` and `GET /events` before acting.
3. If there is no active scene, send `{"type":"continue"}`. If that does not start an active game, send `{"type":"newGame"}`.
4. Record the initial scene, interactable actors, inventory, dialogue options, and events as the first observation.

Use `curl -sS` or an equivalent HTTP client. Send JSON with `Content-Type: application/json`.

## Play loop

Repeat until the game gives an unambiguous completion outcome.

1. Read `/events`, then `/state`. Treat event text, dialogue and scene transitions as the narrative evidence for the next decision.
2. If `cutMode` is true, do not send a gameplay command. Poll `/events` and `/state` until it becomes false.
3. If `dialogOptions` is non-empty, choose one option with `dialogOption`. Prefer options that advance a stated goal or reveal new information; record rejected or unproductive choices to avoid loops.
4. Otherwise inspect the available actors and verbs. Prefer this order when applicable: `lookat`, `talkto`, `pickup`, `action`, `leave`. Read the resulting events before choosing another action.
5. Use inventory items only after their text or the current puzzle provides a reason. Send `actorVerb` with the inventory item as `actorId`, `verb:"use"`, and an interactable scene actor as `target`.
6. Save a named checkpoint before a risky branch or a puzzle experiment: `{"type":"saveGame","target":"agent-checkpoint"}`. It does not clear `/events`.
7. Maintain a compact exploration ledger keyed by scene, inventory IDs, actor IDs, their available verbs, and dialogue choices. Do not repeat an action from an unchanged ledger entry unless new text or inventory explains why.

## Commands and responses

Use these bodies with `POST /command`:

```json
{"type":"actorVerb","actorId":"door","verb":"action"}
{"type":"actorVerb","actorId":"key","verb":"use","target":"door"}
{"type":"dialogOption","option":0}
{"type":"newGame"}
{"type":"loadGame","target":"agent-checkpoint"}
{"type":"continue"}
{"type":"pause"}
```

`POST /command` waits for the render thread:

- `200`: The command was dispatched. It may still produce later text or animation events; read `/events` and `/state`.
- `409`: Refresh `/state`; satisfy the reported precondition instead of retrying unchanged. If paused, send `pause` once to resume.
- `504`: The game is busy or not rendering. Do not retry blindly; poll `/state` and `/events` until it is ready.
- `400`: Correct the command schema before retrying.

Only `newGame`, `loadGame`, and `continue` are valid without an active game. Do not issue normal actions while paused or during cut mode.

## Completion and recovery

- Treat a `gameEnded` event from `/events` as unambiguous completion. Otherwise require player-visible evidence: explicit ending/credits text, a final scene transition with no further playable actions, or the task's stated success text.
- If exploration stalls, load the most recent checkpoint and test a different action justified by text or inventory. Do not brute-force every item-target pair unless the user explicitly asks for exhaustive testing.
- If the game is disposed after an active run, read `/events` and `/state` once more. Treat it as completion only if the visible narrative supports that conclusion; otherwise report the ambiguity.
- Report the completion evidence, final scene, inventory, key choices and any unresolved ambiguity.

---
name: develop-adventure
description: Develop or extend a Blade Engine adventure from its project brief, using local authoring tools and a resumable verification workflow.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read `adventure.json`, `AGENTS.md`, [the brief](../../../docs/brief.md) and [the work log](../../../docs/work-log.md). The brief owns creative choices, scope and acceptance criteria; project metadata owns paths, language and selected capabilities. Existing implementation and evidence take precedence over an old plan's status text.

Use [the canonical project layout](../../docs/project-layout.md) for design, Ink,
editable art, atlas inputs and QA evidence. Before authoring scenes, apply the
[scene/inventory patterns](../../docs/scene-authoring-patterns.md): scene atlases
and background properties, an explicit scene player, bottom origins and a
separate inventory support scene. These defaults apply to dummy slices too.

Author exits as `leave` hotspots over actual background passages, with awaited
approach/optional door opening and a destination arrival. Reuse the shared
[exit patterns](../../docs/scene-authoring-patterns.md#scene-exits-background-hotspots-and-doors)
so the UI supplies its native exit arrow.

Protect player-facing verb sequences with an outer Cutmode true/false pair,
awaiting text/movement/animations. Preserve that owner through nested helpers and
transfer cleanup explicitly on scene exits, as described in the shared patterns.

Use [the agent workflow](../../docs/agent-workflow.md) to discover available tools, design a small playable slice, implement it and validate its sources before expanding. Record unresolved decisions and the next concrete action in the work log. Do useful independent work when an optional tool is unavailable; keep dependent checks pending.

Route puzzle design to [puzzle-designer](../puzzle-designer/SKILL.md), implementation to [implement-puzzle](../implement-puzzle/SKILL.md), scene changes to [edit-scenes](../edit-scenes/SKILL.md), and dialogue to [write-game-ink](../write-game-ink/SKILL.md). Use the art, Spine, music, SFX and localization skills only for disciplines the brief needs. Do not impose the starter's art, example dialogue, language or puzzle policy on the new game.

A build proves compilation. A narrative simulation proves its declared bindings. Runtime scenarios prove their actual assertions and control level. Keep those outcomes separate, together with visual/listening/device review gaps. Report the requested implementation with its current authoring evidence and explicitly unrun QA. Do not claim gameplay acceptance from a build. Creating remote repositories, publishing or starting a production agent is a separate user-selected operation; these instructions do not authorize it.

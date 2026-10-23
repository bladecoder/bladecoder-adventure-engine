---
name: debug-game
description: Observe, diagnose and test a Blade Engine adventure through its shared HTTP controller, state, events and captures.
---

Read [the runtime workflow](../../docs/runtime-workflow.md) and [QA workflow](../../docs/qa-workflow.md). Use `tools/project.py runtime`; the server ships in the engine, without the editor. Inspect actual mode, build identity and capabilities before relying on an endpoint. Keep discovery tokens private.

Start owned isolated sessions or explicitly attach to a private discovery file. State/focused JSON pointers, event transcripts and scene/UI captures provide different evidence. Commands acknowledge acceptance separately from completion; poll jobs and use bounded deadlines. Preserve the first failure and collect evidence instead of blindly repeating a mutation. Cancellation does not roll back effects; a stale identity requires an explicit reconnect. Stop only owned sessions.

Use project-declared suites/profiles and declarative scenarios rather than assuming scene/puzzle IDs. Input steps reach the actual game processor; semantic verb/fixture operations do not establish UI reachability or native OS input delivery. Distinguish debug setup from a player-perspective playthrough. Use player mode for player-visible evaluation and keep hidden debug knowledge out of it.

Persistent content overrides require explicit activation and review of a prepared bundle; they are different from temporary fixtures and canonical source edits. Use the common preview/activate/restart boundary and preserve rollback evidence. For reproduction, retain state, events, input/scenario steps, captures, errors, seeds, build identity and any reduction result. Report unavailable comparisons/coverage honestly; a default smoke is not full-game certification.

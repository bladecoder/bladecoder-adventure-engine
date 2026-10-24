---
name: puzzle-designer
description: Design adventure puzzles with clear state ownership, player feedback and testable outcomes before implementation.
---

Follow the [opt-in QA policy](../../docs/qa-workflow.md): execute test suites,
narrative tests, runtime scenarios/playthroughs and media/device probes only
when explicitly requested by the user. Authoring previews, compilation, static
reference validation and package generation remain normal production work.
Report omitted QA as not_run; an implementation request alone does not select QA.

Read the project brief, relevant scene/story design, existing inventory and implemented interactions. Use the project's puzzle policy when one is declared; otherwise propose decisions that fit the brief rather than importing another game's philosophy or identifier scheme.

Describe the obstacle, player motivation, solution dependencies, clues, early/wrong-attempt feedback, repeat/interruption behavior and consequence. Make intentional failure states explicit. Keep independently solvable branches independent. Record entry conditions and observables for success, failure and persistence.

Assign each state value an owner: engine for inventory, location, actor state/visibility and current player; Ink for narrative knowledge and dialogue progress. Prefer querying the owner to adding mirrored flags. Document identifiers and resources the implementation will need, distinguishing existing assets from proposed ones.

Keep the design under `docs/design/` using the project's naming convention. Link it from the work log and continue with [implement-puzzle](../implement-puzzle/SKILL.md) when implementation is requested. Design documents describe intent; do not label unexecuted verification as passed.

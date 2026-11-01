# Ink language reference

Use this bundled reference for Ink syntax. Read only the topics relevant to the current edit or failure. For speakers, commands, tags, external bindings, state ownership and save/load behavior, use [the project Ink contract](../../../../docs/ink-workflow.md). Generic runtime/C# examples do not define Blade Engine behavior; compile and test with the compiler and Java runtime selected by the project tools.

## Topics

- **Content and basic flow:** [overview](1-the-basics.md), [content, comments and tags](1-1-content.md), [choices and displayed text](1-2-choices.md), [knots](1-3-knots.md), [diverts and glue](1-4-diverts.md).
- **Branching and organization:** [branching and loops](1-5-branching-the-flow.md), [INCLUDE and stitches](1-6-includes-and-stitches.md), [once-only, sticky and conditional choices](1-7-varying-choices.md).
- **Text and built-in queries:** [sequences and conditional text](1-8-variable-text.md), [visits, turns, choice counts and random seeds](1-9-game-queries-and-functions.md). These language queries are distinct from the engine's external functions.
- **Weave:** [overview](2-weave.md), [gathers](2-1-gathers.md), [nested flow](2-2-nested-flow.md), [labels, scope and visit tracking](2-3-tracking-a-weave.md).
- **Variables and logic:** [overview](3-variables-and-logic.md), [global variables](3-1-global-variables.md), [expressions and numeric/string operations](3-2-logic.md), [conditional and multiline blocks](3-3-conditional-blocks.md), [temporary variables](3-4-temporary-variables.md), [functions](3-5-functions.md), [constants](3-6-constants.md), [external functions and variable observers](3-7-advanced-game-side-logic.md).
- **Advanced flow:** [overview](4-advanced-flow-control.md), [tunnels and return flow](4-1-tunnels.md), [threads](4-2-threads.md).
- **Lists and state:** [overview](5-advanced-state-tracking.md), [declarations](5-1-basic-lists.md), [reusing lists](5-2-reusing-lists.md), [list values and ordering](5-3-list-values.md), [membership, sets and knowledge tracking](5-4-multivalued-lists.md), [advanced operations](5-5-advanced-list-operations.md), [multiple list types](5-6-multi-list-lists.md), [crime-scene example](5-7-long-example-crime-scene.md), [flags, state machines and properties](5-8-summary.md).
- **Identifiers:** [international characters](6-international-character-support-in-identifiers.md).
- **Background:** [introduction](introduction.md).

For project narrative flags/lists, start with the list declarations/membership references and the contract's state-ownership guidance. For repeated dialogue after save/load, consult varying choices and weave tracking, then test the integrated route. For `EXTERNAL` calls, consult the contract's actual bindings; upstream C# hosting examples are background only.

## Provenance

Copied on 2026-10-22 from `skills/write-ink-fiction/references/ink/` in [narrative-skills at commit 05835920c789c037b88e16cf2369defe5864f191](https://github.com/bladecoder/narrative-skills/tree/05835920c789c037b88e16cf2369defe5864f191/skills/write-ink-fiction/references/ink). The 36 topic files are excerpts of inkle's [Writing with Ink](https://github.com/inkle/ink/blob/master/Documentation/WritingWithInk.md). This is a checked-in reference snapshot, not a dependency on another checkout or a claim to track the latest language/runtime version.

Local adaptations replace the index, correct cross-section links for the split layout, repair external documentation/GitHub links, update extraction comments, and normalize leading indentation/trailing whitespace. Topic examples and explanations are otherwise preserved. When refreshing the reference, compare the supplied topics with this snapshot, retain these link fixes and check new syntax against the project's declared `blade-ink` compiler/runtime before adopting it.

Copyright and permission notices are retained in [inkle's MIT license](LICENSE-inkle.txt) and [narrative-skills' MIT license](LICENSE-narrative-skills.txt).

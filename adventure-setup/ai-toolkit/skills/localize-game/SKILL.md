---
name: localize-game
description: Localize adventure dialogue, model/UI text and localized artwork while preserving identifiers, placeholders and verified layout.
---

Read the brief/metadata language and inspect the actual owning text bundles and narrative sources. Preserve model `@` keys, format tokens, escaping and markup. Do not translate actor/scene IDs, Ink variables, knots, commands or conditions as displayed prose.

World text belongs to `assets/model/world*.properties`; chapter text belongs to its chapter's `.properties` bundles. Inspect fallback behavior before creating or renaming localized bundles. Ink prose belongs to canonical `.ink` sources and must compile/load through [write-game-ink](../write-game-ink/SKILL.md). The starter has no general Ink translation extraction pipeline; do not invent an extraction command or patch compiled JSON alone. Document a needed extraction/key-ownership workflow before implementing it.

Localized artwork uses explicit locale metadata in [art recipes](../create-game-art/SKILL.md). Run model/reference checks and declared locale/viewport scenarios. Inspect real UI/dialogue for glyph coverage, wrapping, clipping, hit areas and fallback. Extend the matrix for long strings and changed screens. Accept visual baselines only after actual review; renderer/font incompatibility is unavailable comparison, not a pass. Record linguistic and device review separately.

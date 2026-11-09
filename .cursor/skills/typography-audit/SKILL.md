---
name: typography-audit
description: Checks a fixed Android canvas for type size, hierarchy, spacing, pairing, and readable contrast. Adapted from Matthew Blode's typography-audit skill. Use when changing TextStyle, font files, or the cover clock's type scale.
license: MIT. Copyright (c) 2026 Matthew Blode. Full terms in LICENSE.md.
source: https://github.com/mblode/agent-skills/blob/main/skills/typography-audit/SKILL.md
---

# Typography audit for the cover clock

Derivative of the upstream skill, rewritten for a single Jetpack Compose canvas (1080×1272 px, 360 dpi) instead of CSS. The upstream rule files (`font-`, `size-`, `spacing-`, `hierarchy-`, and the rest) stay in that repo; this copy keeps the checks that apply here and drops browser, `@font-face`, and HTML-punctuation machinery.

Two faces are already chosen. Do not propose a third.

- Display: Bitcount Grid Double, digits and seconds only.
- UI: JetBrains Mono Bold, every other string.

## Scale

At 360 dpi, 1 sp is 2.25 px. Read the roles, not a web base of 16 px:

| Role | Face | About | Color |
| --- | --- | --- | --- |
| Primary | Bitcount | the clock, clearly the largest | bright foreground, colon in accent |
| Secondary | JetBrains Mono Bold | date; temperature and condition | light / bright foreground |
| Tertiary | JetBrains Mono Bold | status strip, high/low, hinge, stand | dark foreground, with state colors only where the state matters |
| Seconds | Bitcount | smaller than the clock, larger than the status strip | accent |

If two neighboring roles are within a step of each other, the hierarchy has collapsed. Fix size or color, not both at once.

## Spacing and measure

- Keep the date close to the clock. It is the clock's label, not a second header.
- Give the tertiary status strip air underneath before the date starts.
- One line for the strip. Even gaps. Do not let it wrap.
- Do not set a fixed line height so small that a larger style inherits it and collides. Each style sets its own line height to its own size.
- The Motorola icon row (about y 1120–1190, x 0–585) is not a margin you can put type in. Lines above it end above it. Lines below it start below it. Nothing is clipped by that band.

## Pairing and weight

Bitcount and JetBrains Mono are different jobs. Digits do not switch to the mono, and sentences do not switch to Bitcount. The mono is the bold file; do not also faux-bold it with a stroke.

## What to report

When a change breaks the scale, name the composable, the style, and the fix (size, color, or position). Do not restyle the whole face because one line is tight.

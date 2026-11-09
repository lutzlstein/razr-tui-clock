---
name: frontend-design
description: Guidance for distinctive, intentional visual design when building or reshaping UI. Adapted for a fixed 1080×1272 Android cover canvas. Helps with aesthetic direction, typography, and choices that don't read as templated defaults.
license: Apache-2.0. Full terms in LICENSE.txt. Derivative of Anthropic's frontend-design skill, adapted for this cover clock.
source: https://github.com/anthropics/skills/blob/main/skills/frontend-design/SKILL.md
---

# Frontend Design

Adapted for the Razr cover: one fixed portrait canvas, 1080×1272 px at 360 dpi (480×565 dp). There is no browser, no responsive breakpoint, and no page scroll on the clock face. "Above the fold" is the whole panel. Web-only mechanisms (CSS selectors, hover, viewport units, document flow) do not apply; the same decisions are made with Compose layout, `TextStyle`, and the Maine Autumn Sunset palette.

Approach this as the design lead at a studio known for a distinct identity. The clock already has a brief: a terminal desk clock in that palette, Bitcount Grid Double for the digits only, JetBrains Mono Bold for everything else. Follow the brief. Do not invent a second aesthetic.

## Ground the design in the subject

This is a clock you read from about half a meter, on a small OLED, with two camera holes in the bottom-right and one system icon row on the lower left. The subject's vernacular is a terminal, not a phone home screen and not a marketing site. The one memorable element is the time.

## Design principles

The hero is the clock digits. Open with them. A big number with a row of equal stats and a gradient is the generic treatment; here the number is the time, and the stats stay quiet.

Typography carries the personality. Two families, clearly distinct: Bitcount for the digits and seconds, JetBrains Mono Bold for every other string. Do not add a third face.

Set a type scale and keep it. On this canvas, size is hierarchy:

- Primary: the clock, large enough to be the only thing you see first.
- Secondary: the date (as the clock's label) and the current temperature plus condition.
- Tertiary: the top status strip, seconds, high/low, hinge and stand.

Weight is already Bold for the mono. Use color from the palette to demote tertiary lines (`dark_foreground`) and to mark state (`bright_green` charging, `bright_magenta` stand, `accent` on the colon and seconds). Do not invent colors.

Line length on this panel is short by nature. Do not wrap the status strip; if a line does not fit, drop a word, do not shrink the clock.

Avoid generated-UI tells that fight this brief:

- A label in all caps sitting above every block.
- Accenting a single word inside the time.
- Cards, shadows, and rounded containers around each readout.
- Middle-dot separated meta strings used as decoration.

Structure encodes information. The lens rings are the camera holes, not ornament. The gap in the lower left is the system icon row, not a random margin. Do not add borders that retrace the cutout.

Motion is the ring step (one lens each second, the other each 15 seconds) and the seconds digits. Do not add entrance fades.

## Process

Before moving type around, name the hierarchy in one sentence and sketch where each level sits. Then check it against the brief: the clock is still the largest thing, the status strip is one line, the icon row and the lens glass stay clear. Then change the composable.

## Copy

Words are for reading the clock, not for describing the layout. "53°" and "clear" beat "Current conditions". "stand" beats "device is in stand mode". Sentence case. One job per line.

## Restraint

Spend the boldness on the digits. Remove anything that competes with them. Critique from a 1080×1272 render, not from a desktop window.

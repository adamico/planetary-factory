---
status: accepted
---

# Bodies are registered under Factorio's names and displayed under Latin ones

Every celestial body carries two names. The identifier it is registered under is Factorio's —
`vulcanus`, `fulgora`, `gleba`, `aquilo`, `shattered_planet` — and the name a player ever sees is
Latin or Greek, supplied by lang files alone: Terra, Ignus, Electro, Sapros, Gelida, Atlantis.
Nothing in code, data or dimension keys uses the display name; nothing in the UI, quest text or
JEI uses the identifier.

The identifiers are Factorio's because the design is a port of Factorio's, and every design
conversation, comparison and upstream reference will name Vulcanus when it means Ignus. Keeping the
source names in the layer developers read makes that mapping free. The display names exist because
the pack should not simply be Factorio with Minecraft blocks — it wants its own surface identity,
and a lang file is the cheapest possible place to put one.

## Considered Options

- **Factorio names everywhere.** Simplest, and rejected only on identity grounds: the pack would
  read as a reskin rather than a design in its own right.
- **Latin names everywhere.** Consistent, but severs the mapping to the source design in exactly
  the layer where it is most useful, and makes every upstream comparison a translation exercise.
- **Latin names, Factorio names as comments.** Comments drift; lang files are checked by the game.

## Consequences

A developer reading `data/gcyr/planet/gleba.json` must know it is Sapros. That mapping lives in
`CONTEXT.md`, where each body's entry states its internal ID, and the glossary's `_Avoid_` lines
name the Factorio term so it does not leak into player-facing text.

Ignus is a deliberate corruption of Latin *ignis*, matching how Factorio derives *Vulcanus*. It is
not a typo and should not be corrected.

`aquilo` is Factorio's ice planet. Ad Astra and GCyR call their own ice world Glacio; that name is
avoided entirely to keep the convention — identifiers are Factorio's names, not a third mod's.

Gelida and Atlantis have no GCyR body behind them and must be defined as new entries in the
`SolarSystem` registry.

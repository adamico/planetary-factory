---
status: accepted
supersedes: [55]
---

# Factorio science is Terra's progression spine, and Researchd is the lab

Terra runs three tech mods in series plus a grid mod, and every one of them ships its own
progression. GregTech's is a voltage ladder, Create's is a build-complexity curve, Mekanism's is a
processing-factor ladder. Left as they are, the pack has three ladders and no spine, and a
Factorio-literate player — the audience this pack is for — recognises none of them.

This ADR records the spine itself. **Which mod owns each rung is ADR-0017**, consulted on every
recipe; this one is read once.

## The spine

**Factorio's science packs are the progression, and each rung grants a capability the next rung's
production physically requires.** One ladder, not two: the science tier is the gate, the capability
is the reward, and the reward is what makes the next tier producible.

Four packs plus an unscienced rung 0 (`#26`):

| Rung | Pack | What the rung is about |
| --- | --- | --- |
| 0 | *(none)* | Steam and Create kinetics. LP Solid Boiler, LP Steam Miner, the Furnace, Create's belts and the Steam Engine. **ADR-0032 removed Crushing Wheels from this row** — ore smelts 1:1, so the chain is Miner → Furnace → plate, one hop. |
| 1 | `automation` | **Electricity, and the first assembler** — the Alternator, the FE grid and Assembling Machine I (`#34`), **plus steel** (ADR-0039). *This row read "First machines and Mekanism enrichment"; ADR-0032 cut enrichment, and `#34` had already hung the grid on the Assembler so the rung's reward did not depend on it.* |
| 2 | `logistic` | Movement at scale — Create 6 package logistics — **plus the Oil Refinery, the Chemical Plant, basic oil processing, solid fuel, sulfur, sulfuric acid and plastic** (ADR-0025), **and the red circuit, which plastic makes** (`#125`). |
| 3 | `chemical` | **Advanced oil processing, heavy and light cracking, lubricant** (ADR-0025), the blue circuit and the Electric Furnace (`#91`), and **the nuclear chapter** — `uranium-mining`, `uranium-processing` and `nuclear-power` are all chemical science in Factorio (ADR-0033). *The 5x dissolution tier is gone — ADR-0032.* |
| 4 | `production` | **Rocket fuel, rocket control units, rocket parts and the silo**, plus `nuclear-fuel-reprocessing` (ADR-0033). Kovarex costs **space** science and is post-launch, so Terra's reactors run at raw 0.7% U-235 — Factorio's own inefficiency, not a pack nerf. |

**Steel is a rung 1 grant, amended in by ADR-0039.** This table named steel at no rung at all, and
`docs/spec/terra-progression.md` placed only the Steel Furnace, at rung 2 — so the metal itself had
no home. Factorio's own placement settles it: `steel-processing` costs 50 automation packs, which is
this rung. The Steel Furnace stays at rung 2, so the metal arrives a rung before the block made of
it, and `steel-axe` — which doubles mining speed and hangs off `steel-processing` — lands at rung 1
too, where the Steam Miner is carrying the player and hand-mining feels worst.

**Rungs 2–4 were re-cut by ADR-0025.** Rung 4 was "the oil chapter entire"; the chapter now starts
at rung 2. The cause is sulfur: making it petroleum-derived, as Factorio does, puts it and sulfuric
acid behind oil — and sulfur gates chemical science, so oil must precede it. Factorio's own tech
costs say the same thing, since `Oil processing`, `Sulfur processing` and `Plastics` are all
red+green while `Advanced oil processing` costs blue.

The spine rule survives the move intact: rung 2's sulfur buys rung 3's science pack, rung 3's
advanced processing buys rung 4's launch fuel, and rung 4's fuel buys the silo.

**The red circuit moved with the oil chapter** (`#125`). `#55` granted it at rung 1, reasoning from
Factorio's own tech tree, where red circuits come from plastics and plastics are affordable then.
ADR-0025 had already moved plastic to rung 2, so rung 1 would have granted a recipe the player
cannot build. Red is a rung 2 grant; blue stays at rung 3, where sulfuric acid gives it a rung of
headroom, and `#42`'s red circuit in the `chemical` pack is bought a rung after red is standing.

**Rung 4's gate is re-argued.** `#39` justified the rung on the polymer, since every GCyR fuel tank
and rocket motor is an Assembler recipe taking `plate KaptonK ×6`. The polymer is now rung 2, so
that argument is spent, and the replacement is Factorio's own: the **Rocket silo** is the technology
that costs production science, and it requires rocket fuel, rocket control units and concrete.

Packs keep **Factorio's names, ingredient count and ingredient roles**; the items filling those
slots come from the mod that owns the rung. Military is dropped — its ingredients feed nothing
downstream. Utility and Space are reserved for after the first launch, which is why `production`
takes the fourth slot, with its vanilla recipe discarded.

**Terra's science packs are inert items.** Sapros's science pack decays; that is where the
buffer-as-liability puzzle belongs, and it is specified with Sapros.

## GregTech is instrumental, not the ladder

GregTech is in the pack because GCyR requires it, because its miners are good, and because it is a
cheap chassis for custom machines. It is **not** the progression. Create and Mekanism sit *in
series* on the same ladder, never as a parallel escape from it — ADR-0017 enforces that block by
block.

Two consequences bind every recipe author:

- **Assembling Machine I/II/III** (GT's LV/MV/HV Assemblers, renamed) are **granted by a rung and
  differ by speed**, with **one exception, amended in by `#125`: Assembling Machine I has no fluid
  tanks**, so it cannot run the corpus's 26 `crafting-with-fluid` rows. ADR-0026 dropped that
  restriction on this line's authority and said recovering it was an amendment here; this is that
  amendment. The rider is narrowed rather than broken — the ban was on *accidental* gates,
  invisible ones a recipe author creates by writing an `EUt` or picking a tier. A machine with no
  tank gates nothing by fiat: it is the machine's visible shape, it is Factorio's own ramp, and oil
  arrives at rung 2 alongside the tier that can drink it. `advanced-crafting` stays ungated — all
  three machines run it, and Factorio excludes only the character.
- **No GT recipe carries an `EUt` at all** — ADR-0029 puts energy on the machine, so a recipe cannot
  encode a tier gate even by accident. This supersedes the original rider, "every GT recipe is
  authored at LV `EUt`", which asked a recipe author to remember what is now structural.

## Researchd is the lab, and the quest book teaches

The gate is **Researchd's Research Lab multiblock** (`#45`): research packs are items, the Lab
accepts them by pipe and consumes them unattended, and unlocking fires `unlock_recipe` effects.
FTB Quests keeps the book and the reward surface and **stops being the gate** — which supersedes the
GTCEu `RecipeCondition` approach of `#36` entirely.

Two rules ride on this:

- **The automation rule** — *hand-crafted implies small; large implies automated*. No fixed
  quantity: it depends on the item and the tier, and is applied per quest at authoring time. The
  Lab's piped, unattended intake is what makes the rule enforceable rather than aspirational.
- **No science tier is unlocked by an item the player can hand-craft.** A rung that a player can
  reach by grinding a crafting grid is not a rung.

Researchd's lock is a property of the pack's **crafting-surface inventory**, not of the mod: any mod
that subclasses or replaces `RecipeManager` is a silent hole. That is why FastSuite, FastBench and
ClientCrafting are cut. Neither JEI nor EMI reflects locked state, so a blocked recipe is **shown
and refused**.

## The pacing figure

**20–25 hours** from spawn to first launch, for a Factorio-literate, GregTech-naive player following
the book. It is a figure to **check the beat sheet against**, never a knob to tune costs with. A
beat sheet that lands far outside it has the wrong number of beats, not the wrong prices.

## Considered Options

- **GregTech's voltage ladder as the spine.** Rejected: it inverts the pack's design, and the
  audience does not read voltage tiers as progression. It also makes Create and Mekanism decoration.
- **Two ladders — science for unlocks, mod tiers for capability.** Rejected: the player then has two
  progressions to track and the cheaper one wins. The rung *is* the capability.
- **FTB Quests Task Screens as the lab** (the premise this map was chartered on). Superseded by
  `#45`: Researchd gives real research packs, a research graph and data-driven recipe locking, which
  Task Screens plus `RecipeCondition` only approximated — and that approximation reached GT machines
  only.
- **Three science rungs instead of four.** Rejected in `#26`: in-pack sulfur is a coal product, not
  a refinery by-product, so `chemical` no longer implies oil and the oil chapter earns its own rung.
  **The premise inverted in ADR-0025** — sulfur *is* a refinery by-product now, by choice, so
  `chemical` does imply oil. The rejection nonetheless stands, on a different fact: oil does not fill
  a rung by itself once it is split across two of them, and rung 4 is held by the silo rather than by
  the chapter. Four rungs, re-cut, not three.

## Consequences

- **The beat sheet is written against this table**, rung by rung, and the pack ingredient lists
  (`#42`) are a production test landing on ADR-0017's ownership boundaries.
- **A hand-craftable unlock is a bug**, whatever it costs to author around.
- **A new crafting surface is a spine risk**, not a convenience mod: it must be screened against
  Researchd's lock before it enters the pack.
- **Emission's metric is left open.** ADR-0005 defines it as GT machines' EU/t draw, but ADR-0017
  reduced GT to extraction and assembly, so a GT-only metric now measures a much smaller share of
  the factory than when it was written. Decided with the Emission work.
- **Co-op is a standing assumption, not a decision.** The spine is specified single-player.
- **The delivery sequence moves.** Terra's flow lands before Sapros — see `docs/gdd.md`.

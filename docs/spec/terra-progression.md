# Terra: spawn to first launch

The beat-by-beat arc for Terra, against ADR-0018's **20–25 hours** for a Factorio-literate,
GregTech-naive player following the book. The spine is ADR-0018, the ownership table is ADR-0017,
the recipes are ADR-0031's corpus. This document is the *route* through them.

**Beats name items and surfaces, never quantities.** Prices live in the corpus and in `#42`'s slot
lists and move constantly; a beat sheet carrying numbers is stale by the next recipe edit.

## Hour budget

| Chapter | Budget |
| --- | --- |
| Opening | 1h |
| Rung 0 — steam | 3–4h |
| Rung 1 — `automation` | 4–5h |
| Rung 2 — `logistic` | 5–6h |
| Rung 3 — `chemical` | 4–5h |
| Rung 4 — `production` | 3–4h |
| **Total** | **20–25h** |

Front-loaded, which is Factorio's own shape: the first hours are slow because everything is
hand-made, and the last rung is quick because by then the factory does the work. `logistic` is the
fattest because ADR-0025 hung the opening of the oil chapter on it, and that is the chapter where a
Factorio player stops recognising things.

A chapter that lands far outside its budget has the wrong number of beats, not the wrong prices.

## Who teaches what

**The research graph shows cost. The book explains the verb.** Researchd's UI lists what a node
unlocks and what the Lab will eat; the quest book never repeats a price. A chapter opens on a rung
and its quests are *here is what this block does and why you want it*.

They are kept apart on purpose. Prices move every time a recipe is touched; verbs do not. Overlap
them and the player reads neither.

---

## Opening — the first twenty minutes

The wreck is `#100` and `#134`: indestructible, habitable, one cargo hold, and you spawn inside it.

| # | Beat | Surface |
| --- | --- | --- |
| 1 | Wake up inside the wreck. The book is in your inventory; its tooltip points at the panel. | — |
| 2 | Open the panel. The Personal Assembler is already there. Craft one thing, badly, slowly. | Personal Assembler |
| 3 | Leave. Three ore fields are visible from the door. | — |
| 4 | Place the Furnace and the Steam Miner from your pocket. First plates. | hand |
| 5 | Chart the outfield with the prospector; read the map. | prospector |
| 6 | Miner → belt → Furnace → chest. Something runs while you watch. | machine-fed |

Beat 6 is the twenty-minute mark and the first machine-fed beat in the pack.

**What you start with.** Factorio's own split — tools in your pockets, materials from the ship
(`#100`), and Factorio is famously stingy about both.

- **Pocket**: the prospector (ADR-0019), one Furnace, one LP Steam Miner.
- **Hold**: iron plate, copper plate, coal. Single digits, matching freeplay's eight-plate debris
  chest.
- **No weapon.** Factorio hands you a pistol; here the wreck is the answer to night one (`#134`),
  and a door is a better answer than a pistol.

Nothing in the hold is otherwise unobtainable. It removes the pre-tool grind; it does not seed a
tier. The moment the hold contains a green circuit, rung 0 stops being taught.

**Beat 2 is the one to playtest first.** The entire pack rests on a player finding a panel that
nobody handed them. `#95` chose that deliberately — there is nothing to grant and nothing to lose —
but the cost is that discovery is the opening's only job.

---

## Rung 0 — steam

*No science pack. Burner and kinetic, and not one watt anywhere.*

**Granted**: LP Solid Boiler, LP Steam Miner, the vanilla Furnace as Stone Furnace (`#91`), Create's
Steam Engine as prime mover, mechanical belts.

**What rung 1 needs it for**: `automation` packs are Personal-Assembler-only forever (`#42`), so
rung 0's job is to make the plates that feed them faster than your hands can.

| Beat | Fed by |
| --- | --- |
| Boiler and Steam Miner as a pair — the miner is your ore supply from here on. | hand |
| Belt the miner's output to the furnace bank. | machine |
| Green circuits by hand. They are Assembling Machine I's own key, which is why they are not a rung (`#55`). | Personal Assembler |
| Steam Engine: steam becomes rotation. It powers machines, not a grid. | machine |
| Hand-feed the Lab its first `automation` packs. | hand |

**The grid does not exist yet**, and this is the chapter that earns it. Everything here burns coal
or spins. The player should finish rung 0 slightly sick of walking packs to the Lab — that is the
argument for rung 1, made by the game rather than by the book.

---

## Rung 1 — `automation`

**Granted**: the Alternator and the FE grid, Assembling Machine I.

**Why the grid arrives here**: the Alternator turns Create's rotation into watts (`#92`), and
Assembling Machine I is FE-native (`#37`) — the grid arrives with its first customer and not one
rung earlier. This holds whatever `#69` decided about ore multiplication — and ADR-0032 cut it entirely.

**What rung 2 needs it for**: the belt-and-package build-out is an assembly problem, and everything
past here is assembled.

| Beat | Fed by |
| --- | --- |
| Alternator on the Steam Engine. Rotation becomes watts; the first cable run. | machine |
| Assembling Machine I. The Personal Assembler stops being how you *produce* — it never stops being how you *craft*. | machine |
| Feed the Assembler from the belt, not from your hands. | machine |
| Pipe the Lab. `logistic` packs arrive without you. | machine |

**Assembling Machine I has no fluid tanks** (ADR-0018, amended by `#125`). It cannot run the
corpus's `crafting-with-fluid` rows, and it is not meant to — oil arrives at rung 2 alongside the
tier that can drink it. This is the machine's visible shape, not a hidden gate.

---

## Rung 2 — `logistic`

The long chapter. Two things happen at once: movement at scale, and oil.

**Granted**: Create 6 package logistics (`#28`), rail and trains, the Oil Refinery, the Chemical
Plant, solid fuel, sulfur, sulfuric acid, plastic, the red circuit (ADR-0025, `#125`), the Steel
Furnace (`#91`).

**What rung 3 needs it for**: sulfur buys the `chemical` pack. That is the spine rule, stated
plainly.

| Beat | Fed by |
| --- | --- |
| Package logistics. Auto-requesting is Factorio's "logistic robots after belts". | machine |
| Rail and trains. Distance stops being a wall. | machine |
| The Fluid Drilling Rig taps a bedrock fluid deposit. *Fractions per `#86`.* | machine |
| Oil Refinery and Chemical Plant. Two new machine idioms in one beat — this is where GT literacy is actually spent. | machine |
| Solid fuel, and the Steel Furnace that burns it. Fuel throughput becomes a constraint you can feel. | machine |
| Sulfur → sulfuric acid. | machine |
| Plastic, and the red circuit it makes. | machine |

**Pantographs are not in Terra's first iteration.** Rail is Factorio's `Railway`, which is red +
green and lands exactly here; electrified rail is not a Factorio mechanic, so there is no citation
to honour and no reason to spend a beat on it before Terra ships.

**The chapter's thesis**: *oil exists, and it makes three things you already wanted.* Fuel, sulfur,
plastic. A Factorio player recognises every one of them, which is what carries them through two
unfamiliar machines.

---

## Rung 3 — `chemical`

**Granted**: advanced oil processing, heavy and light cracking, lubricant (ADR-0025), the blue
circuit (`#55`), the Electric Furnace as Mekanism's renamed Energized Smelter (`#91`), uranium past
its acid gate (`#58`, and whether the nuclear chapter ships at all is `#89`).

**What rung 4 needs it for**: cracking is what produces launch-fuel feedstock in quantity, and blue
circuits are 5 per silo cycle, 50 cycles per launch.

| Beat | Fed by |
| --- | --- |
| Advanced oil processing. The barrel splits three ways instead of two. | machine |
| Cracking. Heavy → light → gas, and suddenly the ratios are yours to choose. | machine |
| Lubricant. | machine |
| Blue circuits, on acid. | machine |
| Electric Furnace. Fuel stops being a constraint, one rung after it started being one. | machine |

**The chapter's thesis**: *the same barrel, split finer.* Cracking is the first beat in the pack
that is about a ratio rather than an unlock — nothing new is revealed, you simply decide what your
oil becomes. For a Factorio-literate player this is the moment of recognition the whole arc has been
building to, so it gets the chapter's weight even though it unlocks the least.

---

## Rung 4 — `production`

**Granted**: rocket fuel, rocket control units, rocket parts, the Rocket Silo.

Short by design. By now the factory builds things while you watch, and the rung's difficulty is
throughput rather than novelty.

| Beat | Fed by |
| --- | --- |
| Rocket fuel, petroleum-derived, `gtceu:rocket_fuel` and nothing else (`#41`). | machine |
| Build the Rocket Silo. The largest multiblock in the pack. | hand + machine |
| Feed it 50 cycles: HDPE, blue circuits, rocket fuel (`#41`, `#53`). | machine |
| Build the rocket by hand. Once. Ceremony. | hand |
| Launch. | — |

**The 50 cycles are the real final exam.** Nothing is unlocked by them and nothing is taught; the
beat asks one question — *is your factory finished?* — and the answer is a number of hours, not a
recipe. That is the whole reason the count is a persisted integer on the silo (`#53`) rather than a
crafting cost: the player watches it climb.

**The hand-built rocket is deliberate friction at the end of an automated chapter** (`#41`, GDD §4).
It happens once, for the first departure from Terra's orbit, and never again.

**The Orbital Starter Kit is named here, not specified.** It is what the first launch delivers; its
contents belong to the orbital platform, and this arc stops at the pad.

## What this document does not decide

- ~~**Ore processing at rung 0 and enrichment at rung 1**~~ — **settled by ADR-0032**: ore smelts
  1:1, every multiplier is cut pack-wide, and the chain written above (Miner → Furnace → plate, one
  hop) is now what the ADRs say too. ADR-0017's Ore processing row is deleted.
- **Which fractions come out of Terra's bedrock** — `#86`.
- **Whether the nuclear chapter ships** — `#89`.
- **Emission's pre-launch readout** — it keys on a metric ADR-0018 leaves open, so placing a beat
  for it now would be placing a beat to delete later. Decided with the Emission work.
- **Every quantity on this page**, because there are none.

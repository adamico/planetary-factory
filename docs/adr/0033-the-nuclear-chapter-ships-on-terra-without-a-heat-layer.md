---
status: accepted
supersedes: [58]
---

# The nuclear chapter ships on Terra, and superheated steam replaces the heat layer

`#58` kept uranium as one of Terra's four ores on Factorio fidelity, and gave it Factorio's
sulfuric-acid gate. Nothing owned anything after that step. `#88` could not assign
`intermediate-products/uranium-processing` or four rows of `production/energy` from ADR-0017's table,
because fission is a generation system and no row describes one.

## The chapter ships, and Factorio's own costs place it

The tech tree is committed data (ADR-0022), so the placement is read rather than argued:

| Factorio technology | Cost | Rung |
| --- | --- | --- |
| `uranium-mining` | automation + logistic + chemical | 3 |
| `uranium-processing` | **trigger** — no packs at all | 3 |
| `nuclear-power` | automation + logistic + chemical | 3 |
| `nuclear-fuel-reprocessing` | + production | 4 |
| `kovarex-enrichment-process` | + **space** | **post-launch** |

**Nuclear power is chemical science.** It is pre-launch on Nauvis and it is pre-launch here. The
chapter's inefficiency is Factorio's own and needs no pack nerf: **Kovarex costs space science**,
which `#26` reserved for after the first launch, so Terra's reactors run on raw 0.7% U-235 exactly as
Nauvis's do before you get there.

**`uranium-processing` is a trigger technology** — `cost_kind: trigger`, no packs — so it ships as
packless research at rung 3 on Researchd's `CheckItemPresenceResearchMethod`, which `#42` already
sanctioned at any rung. It gives rung 3 a beat that teaches *research is not only packs*, right where
the player has just built the acid gate that let them mine the stuff.

## The reactor is the pack's, and MekanismGenerators is not adopted

**Mekanism's fission reactor is not in this pack.** The manifest ships `Mekanism-1.21.1-10.7.19.85`
and nothing else; the Fission Reactor, the Industrial Turbine and the fuel assemblies live in
**MekanismGenerators**, which was never installed — zero matching classes in the jar we ship. Base
Mekanism gives the fuel chain (Isotopic Centrifuge, uranium hexafluoride, uranium oxide) and no
reactor to burn it in. `docs/factorio-mechanics.md` said *"fission is Mekanism's reactor, so it is
what the pack has today"*; that was true of Mekanism and false of the pack.

Adopting the jar was considered and refused. It does not arrive alone — Heat Generator, Gas-Burning
Generator, Wind Turbine, Solar Generator, Advanced Solar, Bio-Generator and the Fusion Reactor come
with it, landing on the Power generation row ADR-0017 gave Electro outright and `#92` spent a whole
ticket settling. That is seven recipe removals to buy one reactor, a second power ladder in the
manifest forever, and a permanent `adapted` on a chapter the pack can have exactly.

So the chapter is **pack-authored on a GT chassis**, the idiom ADR-0025, ADR-0026 and `#53` have now
established four times. Three machines:

- **Centrifuge** — runs `centrifuging`: `uranium-processing`, `nuclear-fuel-reprocessing`, and Kovarex
  when the post-launch map reaches it. `setMaxIOSize` is **(1, 2, 0, 0)**, read off the corpus.
- **Nuclear Reactor** — consumes fuel cells, emits superheated steam.
- **Steam Turbine** — the only machine that accepts superheated steam.

GregTech keeps the chassis and nothing else. **`#37`'s removal of GT's power layer stays total**: GT
ships LV/MV/HV Steam Turbines and a Large Steam Turbine multiblock, and un-removing one was the
cheaper route and was refused. The value of that rider is that it is absolute — four facts of GT
literacy with no exceptions to remember — and the *first* exception is the expensive one, not the
tenth. GT's Large Steam Turbine is nonetheless the structure to read from if the pack later wants a
multiblock tier.

## Superheated steam is a fluid, not a heat system

Factorio's chain is reactor → heat pipe → heat exchanger → steam turbine, and heat there is a real
transport layer: heat pipes run 500–1000 °C at 1 MJ/°C, flow needs a temperature *differential*
(`1 + P/15 °C` drop per pipe, and reactors conduct worse than pipes at `1 + P/387`), coverage on
Aquilo is **adjacency** rather than plumbing — one tile, orthogonal or diagonal, above 30 °C — with
per-entity draw (a belt 10 kW, a beacon 400 kW) and an immunity list.

**None of that is built here.** `heat-pipe` and `heat-exchanger` are `not_emitted` on Terra, and the
reactor emits superheated steam directly.

What replaces the temperature axis is **two fluids**. Superheated steam is its own GT material in the
pack's material JSON; the Reactor and — later — Ignus's acid neutralisation emit it, and **only the
Steam Turbine accepts it**. Ordinary steam keeps the rung-0 chain, GT boiler → Create Steam Engine →
Electro Alternator, which the Turbine will not take.

This is Factorio's own model with the continuum flattened to the two values the game actually uses:
the wiki is explicit that *"actual power production is based on the temperature of the steam, not the
building itself"* — a turbine on 165 °C boiler steam is worth two steam engines, on 500 °C it is
5.82 MW. Factorio already puts the value in the fluid. We keep two points on that line instead of the
line.

**It is also what stops the Turbine being a parallel escape.** A turbine taking any steam straight to
FE would retire the rung-0 Alternator chain the moment rung 3 landed — precisely the bypass ADR-0017
exists to prevent. Two fluids means nothing is retired: the player's first Alternator turns out to
have been the right investment, and nuclear's reward is *a great deal more steam*, which is what it is
in Factorio too.

## The real heat network is Gelida's

Terra was the wrong place to build it. Gelida's puzzle **is** heat — GDD §2: *"Fluids freeze without
active heating; every process needs a thermal budget"* — and Aquilo's mechanic is spatial in a way a
fluid cannot express: buildings freeze by proximity, not by plumbing.

The seam is visible in Factorio's own numbers: **the heat exchanger needs ≥500 °C, keeping a building
warm needs ≥30 °C** — one network, two thresholds. The conduction layer (temperature, capacity,
adjacency, differential flow) and the freezing layer (per-entity draw, immunity classes) are genuinely
two things. Building the first for a three-block closed loop on Terra, before the body whose design
needs it has been written, would have committed Gelida to a system designed for someone else's
problem.

So the ledger's *"Heat pipes and heat exchangers as a separate transport network"* stays `blocked` and
moves to Gelida. Terra's cost is real and recorded: **the reactor-to-exchanger ratio puzzle and the
pipe-layout puzzle do not exist here**, and with the neighbour bonus already `excluded`
`by-consequence`, Terra's reactor is a fuel-burning steam source rather than Factorio's layout game.

## The acid gate moves, and Mekanism's ore chain reaches zero

`#58` and ADR-0021 put uranium's sulfuric-acid gate on Mekanism's **Chemical Dissolution Chamber**,
arguing **no custom machine** and that the gate *"lands out of parts the pack had already committed
to"*. The pack now registers five machines, so that argument is spent rather than wrong.

The gate moves onto the pack's **Chemical Plant**, and the **Chemical Dissolution Chamber is cut** —
closing the lever ADR-0032 deliberately left open, and taking Mekanism's ore chain to zero blocks.
*Amended by ADR-0035: this was the last Mekanism block any decision named, and reaching zero here is
what left the mod with nothing. The cut is now the jar's removal — there is no recipe to remove.*

Recorded as a knowing divergence, because the next reader will notice: **Factorio puts the acid in the
mining drill, not in a machine hop.** `#58` chose the hop and ADR-0021 recorded it; this ADR changes
only which machine performs it.

## ADR-0017 gains a row

**`Power generation (superheated steam) | the pack`**, distinct from the steam-and-solar row Electro
owns — the same shape as `#91`'s argument for giving Smelting its own row rather than folding
reduction into Ore processing.

It is named for the fluid rather than for fission on purpose. Terra's reactor and Ignus's acid
neutralisation are the same row, one converter, and Gelida's heating towers land on it later without
an argument. The losing-blocks column records that **MekanismGenerators was considered and refused**,
because the reason the row exists is that refusal.

## Considered Options

- **Cut the chapter.** The session's first recommendation, and wrong: it read a reactor as late-game
  power past this map's destination, when Factorio's own tech costs put `nuclear-power` at chemical
  science, pre-launch, and gate only Kovarex behind space science.
- **Adopt MekanismGenerators.** Argued above. It also silently answers Gelida's fusion row, which
  rests on the same absent jar and must argue it separately.
- **Build the real heat network now.** Argued above: the freezing layer is Gelida's design question
  and is not written yet.
- **Un-remove GregTech's Steam Turbine.** Cheapest, and refused to keep `#37` total.
- **No turbine at all** — reactor steam into the existing Steam Engine → Alternator chain. Held until
  Ignus's acid neutralisation surfaced as a second consumer (`docs/planets.md`: *water ← steam
  condensation ← acid neutralisation*), which the corpus routes and Terra cannot leave `not_emitted`.

## Consequences

- **Three machines and a fluid**, at rungs 3 and 4. `#34`'s beat sheet budgets 4–5 hours for rung 3
  against a chapter that already carries advanced oil processing, cracking, lubricant, blue circuits
  and the Electric Furnace; **the beat sheet needs a revisit** and this ADR is the reason.
- **`heat-pipe` and `heat-exchanger` are `not_emitted`** and Terra loses Factorio's nuclear layout
  puzzle entire.
- **Gelida inherits two things**: the thermal system as its own build, and the fact that its ledger
  row's `via: mekanism` means a jar the pack does not install.
- **The Chemical Dissolution Chamber is cut**, resolving ADR-0032's open lever — *and, per ADR-0035,
  removed with the mod rather than recipe-removed.*
- `data/pack/category-map.json` routes `centrifuging` to the Centrifuge;
  `data/pack/subgroup-owner.json` resolves all seven deferred rows; both static checks extended and
  passing.

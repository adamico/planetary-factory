---
status: accepted
---

# Terra's resources are Nauvis's resources

Terra is the Nauvis analogue. Its ore has never been Nauvis's ore: twenty-three GregTech veins
running from iron down to olivine, Mekanism's six worldgen toggles, Create's two stripe features and
— because Terra had no noise settings of its own until ADR-0019 — the entire vanilla ore set on top.
That is GregTech's spread, Mekanism's spread, Create's spread and Minecraft's spread stacked in one
world, and a Factorio-literate player reads none of it as meaningful.

**Terra's resources are Nauvis's resources: iron, copper, coal, uranium, oil, and stone as the
ground itself.** Nothing else is mined on Terra, and the cut materials do not reappear elsewhere as
imports — every other body already has its own Factorio identity to keep.

## The axis is fidelity, and the cost was known when it was taken

This decision is not a cleanup and it does not pay for itself. It **invalidates a large amount of
existing recipe work** — Mekanism's control-circuit spine gates on osmium and redstone, both cut;
two of ADR-0018's four science packs were specified in brass, i.e. copper plus zinc, also cut. The
choice was made anyway, because the axis is **Factorio fidelity over Minecraft fidelity, modded or
vanilla**. Terra either reads as Nauvis or it does not, and a four-ore Nauvis with a zinc asterisk
reads as a subset with an excuse.

Recording the axis is the point of this ADR. The ore list is the easy half; the next time a recipe
tree asks for one more material because it would be cheap, the answer is here.

## The precedent was already the pack's doctrine

Three of four bodies already do planet-fidelity resources. Ignus carries Vulcanus's set —
`ignus_tungsten_deposit`, `ignus_calcite_deposit`, `ignus_coal_deposit`,
`ignus_sulfuric_acid_geyser`. Electro carries Fulgora's — `electro_scrap_deposit`,
`electro_petroleum_deposit`. ADR-0016 gives Sapros no veins at all and routes its metal through
decay. **Terra was the exception**, carrying `terra_ferrous` (iron *and nickel*),
`terra_cupriferous` (copper *and tin*) and `terra_polymetallic` (lead, silver) on top of the vein
sprawl. This ADR generalises a doctrine the pack already had rather than inventing one.

## All four of Terra's ore systems are cut, not just GregTech's

The framing this started from was "the 23 GregTech veins". Those are a minority of Terra's ore, and
they are the only ore anything in this design can see:

- **Prospecting reads GregTech's vein cache.** A Mekanism, Create or vanilla ore body is not on the
  map.
- **Depletion is GregTech's `depleted` flag, flipped by a GregTech Miner.** Nothing else can be
  marked worked-out.
- **The miner ladder is GregTech end to end**, so GregTech ore is the only ore whose extraction has
  a progression at all.

Which means any surviving parallel system is not a second flavour of ore — it is a **straight
bypass** of ADR-0020, pickaxe-reachable forever, uncharted and undepletable, in exactly the place
where "manual extraction stops being the verb" assumes no such thing exists.

So the cut is applied to all four:

- **GregTech veins** — the survivors are iron, copper, coal and an authored uranium vein; every
  other override is deleted.
- ~~**Mekanism `config/Mekanism/world.toml`** — `shouldGenerate = false` for tin, osmium, uranium,
  fluorite, lead and salt. Mekanism's uranium is off too: Terra's uranium is a GregTech vein, so it
  is charted and depletable like the rest.~~ *Amended by ADR-0035: Mekanism left the pack, so its six
  toggles have nothing to switch off. The file, and the block in `scripts/build-terra-ore.py` that
  rewrote it, are both gone; the cut is applied to three ore systems, not four.*
- **Create worldgen** — zinc and copper stripe features off.
- **Vanilla ore features** — suppressed in Terra's `noise_settings`, which ADR-0019 introduced and
  which now carries this as a requirement it did not previously have.

## Decided

- **The set is iron, copper, coal, uranium.** Cut: zinc, tin, lead, nickel, silver, osmium, gold,
  redstone, fluorite, salt, lapis, diamond, emerald, and GregTech's decorative veins — apatite,
  salts, mica, olivine, garnet, sapphire, manganese, mineral sand, lubricant, oilsands, galena,
  magnetite, cassiterite, copper_tin, garnet_tin.
- **The cuts are deleted, not relocated.** No imports from other bodies, and no ADR-0016 decay
  route: routing a cut metal through Sapros's organics would make decay a mining alternative and
  dissolve the puzzle ADR-0016 exists to pose. Each body keeps its own Factorio identity, which is
  the same rule read from the other end.
- **Oil is a bedrock fluid deposit on Terra**, ~~alongside the iron, copper, coal and uranium bedrock
  deposits~~. `terra_polymetallic` is retired — it is definitionally the grab-bag fidelity rejects —
  and `terra_ferrous` and `terra_cupriferous` are stripped to their headline material. **Amended by
  #86: the coal and uranium bedrock deposits are not built.** Factorio's depleting-but-never-exhausted
  resource is oil, and only oil; an ore patch runs dry. A bedrock coal or uranium deposit would be an
  infinite ore patch, which is the shape this ADR exists to refuse. The bedrock set is therefore one
  authored crude deposit — `planetaryfactory:terra_crude_oil_deposit`, on `gtceu:raw_oil` — and
  nothing else. `terra_ferrous_deposit` and `terra_cupriferous_deposit` are deleted too: the same
  argument reaches them, and ADR-0020's tail section is amended to say so. Terra's bedrock carries
  oil and no ore. GregTech's own six overworld fluid deposits are narrowed to nowhere:
  four of them are refined fractions, and shipping those would void the Oil Refinery of ADR-0025.
- **Stone is ambient terrain, never a patch.** This is the one place fidelity deliberately loses. A
  stone patch in a world made of stone reads as a joke; Factorio's stone patch exists because
  Nauvis's surface is *not* stone. The function — a bulk feedstock you must build production for —
  is already served by ADR-0019's flat ground and by a cobble generator being something the player
  builds. Take the function, drop the form.
- **Uranium keeps Factorio's sulfuric-acid gate.** The ore is minable from hour one and yields
  nothing until sulfuric acid exists. It is built as an authored `mekanism:dissolution` recipe on
  the existing Chemical Dissolution Chamber, with uranium's non-acid processing paths removed —
  **no custom machine**. Mekanism already ships `processing/uranium/slurry/`, and science rung 3
  already sits on Mekanism's dissolution tier, so the gate lands out of parts the pack had already
  committed to. **Amended by ADR-0032, then settled by ADR-0033**: the ore chain is cut, so the
  Dissolution Chamber's only remaining job was this gate — and the pack now registers five machines,
  so *"no custom machine"* is spent rather than wrong. The gate moves onto the pack's **Chemical
  Plant** and the **Chemical Dissolution Chamber is cut**. *Then ADR-0035: `mekanism:dissolution` is
  not a recipe type this pack has — the mod is out of the manifest, and the gate's only surface is
  the Chemical Plant.* Uranium itself stays, and the nuclear
  chapter it feeds ships at rungs 3 and 4.
- **Uranium is not a starting patch**, on the same fidelity grounds: Factorio never places it in the
  starting area.
- **No zinc exception.** It was considered explicitly — zinc surviving as a *processing yield* from
  copper rather than as a patch, which would have saved every Create brass recipe at a stroke — and
  refused. **The line holds at patches and items, not at extraction alone.** An extraction-only line
  is the tempting version and it is precisely where the four-ore set becomes decoration: a player
  who can still hold zinc has not been told anything about where they are.
- **Weights are ordering, not numbers: iron, then copper, then coal, with uranium rare.** The
  numbers are tuning and are out of scope for this decision, on the same footing as the circuit
  ladder's interiors.

## Amendments this ADR makes

- **ADR-0020's starting patches become iron, copper and coal** — were iron, copper, zinc, tin, coal.
  Uranium is excluded deliberately. ADR-0020 invited this by calling its list "a starting
  configuration, not an invariant"; what changes here is not playtest tuning but the pool the set is
  drawn from. The bootstrap targets it named — brass for Create, bronze for steam — are what
  breaks, and re-filling them is the fallout below, not a reason to keep the materials.
- **ADR-0019 gains a requirement**: Terra's noise settings must also suppress vanilla ore features,
  not only replace terrain and carvers.
- **ADR-0017's ownership table** is touched by the re-basing this forces, not by this ADR.

## Known fallout, accepted with eyes open

- **Mekanism's spine breaks.** `basic_control_circuit` is metallurgic infusing of osmium plus
  redstone, both cut, and every Mekanism tier gates on control circuits.
- **Two science-pack recipes break.** The logistic and chemical packs were specified around Smart
  Chute, Brass Funnel and Deployer — all brass, i.e. copper plus zinc.
- **The circuit ladder and the capability-ownership table** both assumed the cut materials existed.

None of this is a surprise discovered afterwards; it is the price the fidelity axis was chosen
knowing. Re-filling the broken slots and re-basing the ownership table are separate decisions.

## Consequences

- **`scripts/build-terra-vein-weights.py` is rewritten, not retired.** Its registry constraint still
  holds — `GTCEuServerEvents.oreVeins` throws `Missing registry: gtceu:ore_vein` during world load,
  so overrides must ship as datapack files at the same id. What dies is its stated premise, *"cutting
  GregTech's set to those four would starve its own recipe tree… nothing is removed."* This ADR is
  that sentence reversed, and the docstring must say so.
- **A uranium vein is authored, not trimmed.** There is no `uranium.json` among the existing
  overrides.
- **The cuts are verifiable**: absent vein files plus `forbidden_ore_veins` fixture rows in the
  worldgen check, and fixture rows for the four survivors. Whether the surviving set *reads* as a
  coherent Nauvis rather than as a subset is a human judgement, recorded as such.
- **Recipe work across the pack answers to the four-ore set**, including on bodies this decision
  does not otherwise touch, wherever a Terra-made intermediate is assumed.
- **Save invalidation is not a cost.** The pack is pre-release.

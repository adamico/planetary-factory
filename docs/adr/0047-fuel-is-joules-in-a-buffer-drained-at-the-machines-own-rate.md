---
status: provisional
supersedes: [155]
---

# Fuel is joules in a buffer, drained at the machine's own energy_usage

`#155` built the three furnace tiers as a speed-and-energy ladder. Every number in it came out of
the corpus except one: the two burner tiers take their burn time from
`ItemStack.getBurnTime(RecipeType.SMELTING)` — Forge's vanilla fuel table — in
`FurnaceBlockEntity.light()` and `isFuel()`. `#185` found it reviewing `#184`.

That leaves the ladder asserting a ratio it never computed. `FurnaceTier`'s javadoc says the Steel
tier's doubled speed "yields twice the items from one coal -- Factorio's ratio, with no per-tier
fuel rule to keep in step", and reasons that Factorio's absolute rate cannot be taken because "the
corpus carries no `fuel_value`". Both halves are wrong in an instructive way, and the second is why
the first was never checkable.

## What Factorio actually does

There is no burn-time field in Factorio. A burner energy source has `energy_usage` in watts and an
`effectivity`; a fuel item has `fuel_value` in joules and a `fuel_category`. Burning one item puts
`fuel_value * effectivity` **joules into the entity's buffer**, and the entity drains
`energy_usage` per tick *while it is working*. Burn time is an emergent quotient, and an idle
burner's part-spent coal keeps its remaining joules indefinitely.

`data/factorio/machine.json` has carried the denominator all along:

| | `crafting_speed` | `energy_usage` | `energy_type` |
| --- | --- | --- | --- |
| `stone-furnace` | 1 | 90 kW | burner |
| `steel-furnace` | 2 | 90 kW | burner |
| `electric-furnace` | 2 | 180 kW | electric |

Both burners draw **the same 90 kW**, and both have `effectivity: 1`. That — not "one burn tick per
operation tick" — is why the Steel furnace gets twice the items from one coal. The ladder's
efficiency story was true by coincidence of Minecraft's tick accounting, on a machine table nothing
consulted.

The second problem is the alphabet. ADR-0034 sweeps stock recipes by default-deny, so the burnables
reachable here are Factorio's fuels. The vanilla table is being asked about planks, blaze rods and
lava buckets the pack does not have, and gives a vanilla number to the handful that it does.

## The decision

**A furnace holds a fuel buffer measured in joules.** Lighting an item adds its `fuel_value` to the
buffer and consumes it whole; a tick of work subtracts `energy_usage / 20` — 4,500 J on both
burners. Nothing is stored in ticks and there is no MJ-to-ticks constant, so there is no rounding
rule to defend: coal's 4 MJ is 888.89 ticks of Stone-tier work and the buffer simply runs out
mid-tick. The Steel tier's doubled yield is now arithmetic — same 4,500 J/tick, twice the craft.

**Fuel is default-deny and category-filtered.** An item burns only if the table has a row for it
and that row's `fuel_category` is `chemical`, which is the only category a Factorio furnace accepts.
The category is carried as data rather than dropped even though every reachable fuel is currently
`chemical`: `#135`'s `uranium-fuel-cell` has a `fuel_value` and is `nuclear`, and without the filter
it would become furnace fuel by arriving.

**`nuclear-fuel` is an ordinary chemical fuel** — 1.21 GJ, in the table, furnace-legal, exactly as
in Factorio. It has no `item-map.json` row, so it is a recorded skip until something makes it, the
same way the recipe converter treats one. The `uranium-fuel-cell` is the `nuclear` item and stays
`#135`'s.

**`wood` resolves through its tag.** `item-map.json` maps it to `minecraft:logs`, so any log burns
at wood's value. The lookup is item-then-tag, not a flat map.

**The table is generated datapack JSON**, joined by a script from a new `data/factorio/fuel.json`
(a narrow extraction — `name`, `fuel_value`, `fuel_category`, nothing else) and `item-map.json`, and
loaded by the mod as a reload listener. Nothing is decided in the script: an `undecided` or absent
item-map row is a recorded skip in the join, not a silent absence at runtime. The static check reads
what the game reads.

## What this costs

`bb12726` is superseded, not extended. The burner flame — drawn as a fraction of the lit item — was
built on the tick model, and the burners now hold the same kind of thing as the Electric tier: a
joule buffer. They get the same horizontal gauge, and the flame goes. Two widgets for one quantity
would say the burner and the Electric furnace hold interchangeable stuff, which is a worse lie than
the one this ADR fixes; one widget with two refill economies is the truth.

`light()`'s `getCraftingRemainingItem()` branch goes with it. That is vanilla's
lava-bucket-leaves-a-bucket rule, no bucket is fuel under a default-deny table, and keeping it would
say a fuel-with-remainder is a case someone considered.

## Why not a committed conversion constant

`#185` asked for "a committed MJ to burn-ticks conversion, in a data file with its reasoning, not in
a script", by the rule that holds `item-map.json` and `grid-substitutions.json`. The rule is right
and the artifact is not: the reasoning turned out to be a division the corpus can perform on itself.
A committed constant is a number someone has to defend and that drifts silently when Factorio
changes a machine; a derived one fails a check. This is `test_resource_extract.py`'s pattern —
re-derive from the game's own formula rather than trust the number — applied to energy.

## Scope

This is the burner half of ADR-0029, which fixed how a Factorio machine's electrical energy becomes
a Minecraft number. `#37`'s boiler and `#135`'s reactor are the two things that will next need a
fuel value, and both are pointed here.

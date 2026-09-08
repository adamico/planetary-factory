# The fuel table check

`tests/factorio/test_fuel_convert.py` asserts that what a burner furnace burns is Factorio's fuel
list joined onto the pack's item map, and that the mod reads the file the join writes. It is a
static data check in `docs/testing/what-to-check.md`'s terms and launches no game.

ADR-0047 is the decision. `#187` is the ticket; `#185` extracted the corpus half.

## What it holds together

Four artifacts, in a line, none of which can be checked from either end alone:

```
data/factorio/fuel.json     name, fuel_value, fuel_category         (#185)
data/pack/item-map.json     Factorio name -> pack item or tag       (ADR-0026)
  -> scripts/factorio-fuel-convert.py
kubejs/data/planetaryfactory/fuel/*.json    what the game loads
  -> PFFuel / FuelTable                     what a furnace asks
```

## The failures it catches

**A fuel that no longer burns.** The join is a name match. Rename an `item-map.json` row's target,
or re-extract a corpus that spells `solid-fuel` differently, and the fuel's row goes — with no
error anywhere, because under ADR-0034's default-deny table an item that is not fuel is the
ordinary case. It reaches the player as a furnace that refuses coal.

**A fuel that burns and should not.** The mirror, and the one the category filter exists for.
`uranium-fuel-cell` has a `fuel_value` and is `nuclear`; the day `#135` gives it an item-map row,
the only thing standing between it and a Stone Furnace is `FuelTable`'s category gate. The check
asserts both gates independently, so `#135` removing the first one is not silently the whole
defence.

**A folder rename.** The listener's folder string and the converter's output path are one name in
two files. A rename in either produces a table that loads nothing, in a game that reports no error.

**A hand-edited number.** Coal's burn duration is re-derived here from `stone-furnace`'s own
`energy_usage / 20`, the way `test_resource_extract.py` re-derives the ore totals from Factorio's
formula. 888 whole ticks, and the Steel tier — same 90 kW, half the craft — at exactly twice the
crafts. A stored figure would be a number someone has to defend; this one fails a check.

## What it asserts

- the converter's own `--check`: the emitted table on disk is not stale, and is never hand-edited
- every fuel with a **decided, non-fluid** item-map row has a table row, and nothing else does
- no emitted row is anything but `chemical`, and `uranium-fuel-cell` has no row at all
- coal's row buys 888 whole ticks at 4,500 J/t, and the Steel tier doubles the crafts
- `wood` reaches the table as the **tag** `minecraft:logs`, not as one species
- `PFFuel` reads the folder the converter writes, and only the pack's own namespace
- ADR-0047's two removals are gone from the smelting package: no `getBurnTime`, no flame sprite
- the burner's hover keys are in the lang file

## Recorded skips, not silences

A corpus fuel with no item-map row, an `undecided` one, or one blocked on an unregistered item gets
no table row and is **printed with its reason** by the converter, the way
`factorio-recipe-convert.py` records one. `nuclear-fuel` is the standing example: an ordinary
chemical fuel in Factorio, waiting on `#135`, and a skip line is what says so rather than its
absence.

Note the deliberate difference from the recipe converter, where a **missing** item-map row is a hard
failure. There the corpus is the pack's recipe list and an unexamined name is a hole in it; here the
corpus is every fuel in Factorio — Gleba's jellynut, Aquilo's fusion cell — and most of them are
nothing on Terra. Sixteen of the twenty rows are skips today.

## Its other halves

`tests/factorio/test_fuel_extract.py` holds the **corpus**: that each `fuel_value` re-derives from
its own raw string, that the category pair still means what the filter assumes, and that both
burner furnaces accept `chemical` at `effectivity: 1`. This check makes the same division on the
artifact the game actually reads.

`mod/src/test/java/com/planetaryfactory/core/smelting/FuelBufferTest.java` and `FuelTableTest.java`
are the **arithmetic and the rule**, under `./gradlew :planetaryfactory_core:test`: that a tick is
paid in full or not at all, that the remainder survives the next lighting, that an item with no row
does not burn, and that a tag row burns every member.

## What it cannot prove

That a furnace in a running game accepts a log in its fuel slot, burns it for the right length and
comes back from a logout with its buffer intact. That is a world load, and it rides with `#156`'s
GameTests.

# Ore amount checks

ADR-0041 makes an ore block carry an **amount** and a break gesture draw one unit from it. That
claim is four separate things that can each be wrong on their own, so it is four checks rather than
one, and none of them launches the game.

## The numbers are Factorio's

`tests/factorio/test_resource_extract.py` — the corpus half.

`scripts/factorio-resource-extract.py` reads the dump's own `resource_autoplace_all_patches` noise
function, because Factorio's starting amounts are closed-form in the prototypes rather than a
property of a generated map: `starting_amount = 20000 * base_density * (frequency_multiplier + 1) *
size_multiplier`, with a default map's controls at 1. The formula is carried across as a **string**
and the check re-derives every total from it, so a hand-edited number in `resource.json` fails
rather than being believed. It also asserts the totals ADR-0041 quotes (iron 400 000; copper and
coal 320 000; stone 160 000), that uranium has no starting patch, and that there is exactly one
distance law and it is flat within 1600 tiles — the reason leaving the starting area early buys
nothing.

`stage_ratios` is asserted per resource to a stated `RATIO_TOLERANCE`, not exactly. Uranium's
`stage_counts` is the shared list scaled by about 2/3 and *then rounded* — its last rung is 50 where
the scaling gives 53.3 — so an exact-fraction assertion would encode a claim the game does not make.

The same check reads `PickTier.java` and asserts the pack's `MINING_TIME` is half Factorio's
`mining_time` and that the two tier speeds are the extracted ones. That is what caught the pack
reading `character-mining-speed` as an addend: Factorio applies the modifier as `base * (1 +
modifier)`, so steel is 1.0 rather than 1.5. The value was already right; its prose was not.

## The mechanism pays out what the block holds

`mod/src/test/java/com/planetaryfactory/core/ore/` under `./gradlew :planetaryfactory_core:test`.

`OreDeltaTest` is the one that matters: a block pays out **exactly** its amount over exactly that
many gestures, the last unit is paid rather than swallowed by the break that removes the block, and
an exhausted position **retires** its entry. Retirement is not tidiness — a delta left behind is
inherited by whatever is placed at that position next, which reaches a player as a fresh patch that
pays out half, with nothing in any log. Note the consequence the tests encode: because retiring is
required, a draw against a retired position is indistinguishable from a draw against a fresh block,
so "an exhausted block pays nothing more" is not a statement this class can make. `OreMining` never
asks it, because by then the block is stone.

`OreFieldTest` is the derivation the design turns on — patch total ÷ blocks actually placed — and
`OreStageTest` the eight sprite rungs against a remaining fraction. `OreCodecsTest` is the
attachment's round trip, which ADR-0038 asks for by name: a codec that drops the map does not crash,
it hands back a chunk whose every ore block silently refilled. `OreCorpusTest` asserts the classpath
slice the mod loads at class-init is the one the extractor writes — it has to be a resource rather
than a datapack file, because the stage count sizes a blockstate property before any world exists.

`MiningSpeedTest` carries the arithmetic across the two halves: a field's cost is its **amount**
times the tier's seconds, not its block count. That is the whole of what the amendment changed, and
it is the one number that was silently a function of how many blocks the generator laid down.

## The blocks exist and drop the right thing

`tests/pack/test_ore_assets.py`, which also runs `scripts/build-ore-textures.py --check` and
`scripts/build-ore-assets.py --check`.

Five ore blocks × eight stages is 54 generated files, and every one of them is a hop a player falls
through: a blockstate naming a model that is not there is a purple cube, a loot table naming an item
the alphabet does not have is a block that drops nothing. The check walks blockstate → model →
texture for all forty, asserts the lang keys, and cross-reads the drop table out of
`OreResource.java` so the Java and the JSON cannot disagree.

It also asserts every ore block is in **`c:ores`**, which is not cosmetic: GregTech's `MinerLogic`
scans that tag to decide what a drill may take, so a pack ore block outside it is invisible to
`gtceu:lv_miner` — the failure ADR-0041 gates its whole worldgen half on.

## The fields fit

`tests/worldgen/test_start_geometry.py`, already registered — stone joins iron, copper and coal as
the fourth field, so the check now deals four patches against every hub variant.

## One consequence worth knowing about

The ore blocks' loot tables are **empty on purpose** — a table that paid out would let a player
blow up a thousand-unit block for one free item, which is ADR-0041's rejected "vandalise a patch
for one ore" wearing TNT. `OreMining.drop` is the only payout, and it is metered.

GregTech's `MinerLogic` produces its output *through the loot table*, so until #105 builds the rigs
a `gtceu:lv_miner` pointed at a pack ore block grinds it to stone and pays nothing. That is not a
regression the empty table introduced so much as one it made honest: the same miner previously
destroyed the whole block for a single item, because it calls `destroyBlock` rather than drawing a
unit. Both readings are wrong, and only #105 — which owns the drills and is what ADR-0041 unblocks
— can make a drill draw one unit at a time. `c:ores` membership is kept precisely so that rig has a
block to see.

## What still needs a world

Whether the stages render, whether the Jade line agrees with what the block actually pays, whether
`gtceu:lv_miner` mines a pack ore block, and whether rung-0 pacing feels right. All four are
one launch, and none of them is a static check.

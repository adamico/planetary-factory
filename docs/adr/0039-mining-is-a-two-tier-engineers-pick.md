---
status: accepted
supersedes: [165]
---

# Mining is a two-tier Engineer's Pick, and the corpus cannot author it

ADR-0034's default-deny sweep removes every stock recipe the pack does not name. Nobody named a
pickaxe, because nobody noticed one was needed: `docs/factorio-mechanics.md`'s **Manual mining** row
read `adapted` / `native_mechanic` with owner `unargued`, and its notice asserted that "mining is a
Minecraft block break, so it is per-block and **tool-tiered**" — a mechanic the pack had already
deleted the means to deliver.

The result is a pack in which **nothing can be mined at all**. `requires_correct_tool_for_drops` is a
block property fixed at registration, not a tag, so hand-breaking iron ore, stone or a GregTech vein
drops nothing and no datapack can change it. `#100`'s starting kit is prospector, Furnace, LP Steam
Miner and a few plates — no tool. And **the corpus can never supply one**: ADR-0031 says the corpus
authors every recipe it contains, the corpus is Factorio's, and Factorio has no mining-tool prototype
to extract. There is nothing to convert.

This ADR is the mining chapter, and it names its own exception to ADR-0031 rather than widening it.

## The rule

**Mining is one tool in two tiers — the Engineer's Pick — and it is a real item.**

| Tier | Recipe | Mining speed | Seconds per ore |
| --- | --- | --- | --- |
| Engineer's Iron Pick | `wood ×1` + `iron plate ×1` | 0.5 | 1.0 |
| Engineer's Steel Pick | Engineer's Iron Pick + steel plate | 1.0 | 0.5 |

*(Amended — this table first read 2.0 and 1.0. See **The mining time is ours** below.)*

Both are **indestructible** — no durability bar on either. The steel recipe **consumes** the iron
pick, so the player holds one or the other and never both.

**One tool, all block classes.** There is no axe, shovel, shears or hoe. The Pick mines wood, stone,
ore, dirt and leaves, and it is the tool that dismantles a GregTech machine. That is Factorio's
single mining gesture wearing a pickaxe model, which is the whole trade this ADR makes: it reads as a
pickaxe so a Minecraft player is at home, and it behaves as one gesture so a Factorio player is too.

**Both recipes land on the `assembling` surface**, which already has a survivor entry in
`recipe_survivors.js` and which the Personal Assembler's predicate reaches — so both are
hand-craftable at rung 0 with no machine, and no new survivor is needed.

**The Iron Pick is in the starting kit** (`#100`, amended here) and remains craftable afterwards. It
is not undroppable: losing it is recoverable by punching a tree, which is the most Minecraft opening
available to a pack with no crafting grid.

## The speeds are Factorio's, and they are transcribed

`mining_time / mining_speed = seconds per item`. Terra's four resources — iron ore, copper ore, coal
and stone — all carry `mining_time: 1`; the character's base mining speed is `0.5`. Hence 2.0 seconds
by hand and 1.0 after Steel axe **in Factorio**, which is what this ADR first shipped.

**These are transcribed from the wiki, not extracted**, and that is a real weakness worth labelling.
`data/factorio/` holds `machine.json`, `recipe.json`, `science_packs.json` and `technology.json` —
there is no resource or entity dump, so unlike `steel-axe` below these two numbers cannot be checked
against the repo. ADR-0022's standard is "extracted rather than transcribed"; this row falls short of
it knowingly. Extending the extractor to emit `data/factorio/resource.json` is the follow-on, and
until it lands a reader who finds these numbers wrong should suspect the transcription first.

## The mining time is ours, because two seconds failed on delivery

**Amendment.** The `mining_time` this ADR uses is **0.5, not Factorio's 1** — one second an ore by
hand, half a second after `steel-axe`. Factorio's two speeds are unchanged and so is the ratio
between the tiers; only the constant they divide moves.

The first version shipped Factorio's 2.0s and failed the human check this ADR itself named
("*2.0s actually feels like Factorio's 2.0s* — **Human on delivery**"). The reason is that the
number is Factorio's *inside Factorio's economy*: there the engineer hand-mines perhaps thirty ore
before a burner drill takes the job, whereas ADR-0019's starting area holds around 1150 ore blocks
and ADR-0020 prices a patch at about an hour by hand. Next to that, and next to a game where every
other block breaks in well under a second, ore alone ran three to five times heavier than the world
around it — a vanilla iron pickaxe takes ~0.75s on iron ore, so the Pick was making its own
signature material the slowest thing in the pack.

What the halving keeps is everything the decision was actually about: the time is still **flat**
across the four resources rather than following vanilla's hardness table, still **halved by
research**, and still **one stated number** a reader can check rather than an emergent property of
block properties. What it gives up is the claim that the seconds are Factorio's. They are the
pack's, and this section is why — a fidelity argument that loses to a playability one is worth
recording as such rather than quietly retuning.

The halved number was then played and confirmed in-game, which is the same check that rejected the
first one — this row's evidence is a person mining, and it is recorded here because there is no
other place it could be checked.

Note that this is a different kind of divergence from the `steel-axe` effect below. That one changes
the *mechanism* and keeps the outcome; this one changes the *number* and keeps the shape.

**Flat time for the four resources and stone; vanilla hardness for everything else.** Factorio gives
all four resources the same `mining_time`, and reproducing that spread-free number is the point.
ADR-0029 already puts Factorio's time on the machine; putting a stated time on the block is the
same move one layer down, and it makes the pack's mining numbers one line to read rather than a
hardness table to derive. Applying the flat time to *every* block was rejected — dirt taking a full
second is not fidelity, it is tedium Factorio never asked for.

## Steel axe keeps its trigger, and changes its effect

The extracted tree carries it exactly:

```
name: steel-axe          source: base
cost_kind: "trigger"     unit: null
research_trigger: { craft-item, steel-plate, ×50 }
prerequisites: [steel-processing]
effects: [{ type: character-mining-speed, modifier: 1 }]
```

**It costs no science packs.** It is one of 33 trigger technologies in the dump and one of the seven
Terra reaches (`#138`).

**Researchd cannot express a craft trigger.** Its four research methods are `ConsumeItem`,
`ConsumePack`, `CheckItemPresence` and the And/Or combinators; nothing fires on a crafting event. The
pack therefore declares `steel-axe` as **`CheckItemPresence` on 50 steel plates** — the item is
checked, not eaten. That is closer to Factorio than `ConsumeItem` would be, since Factorio's trigger
charges nothing and leaves the plates in your inventory. A true craft-trigger is mechanism, belongs
in `planetaryfactory_core` under ADR-0015, and would serve all seven of `#138`'s technologies rather
than this one; it is not a prerequisite for this decision.

**The effect changes from `character-mining-speed` to `unlock-recipe`, and this is the first time the
pack overrides an extracted effect rather than supplying one.** Factorio raises a character
attribute; here the research unlocks the Engineer's Steel Pick recipe and the speed rides on the
item. The outcome is identical — mining doubles — and the mechanism is the one Minecraft players
already read. It is recorded here because `factorio_tech_dsl.js` reads the same data file a future
reader will diff `researchd.js` against, and a mismatch with no explanation looks like a bug.
`#137` and `#138` will produce more of these.

Note the consequence for testing: `tests/factorio/test_research_unlocks.py` asserts every recipe a
research grants is a recipe the pack emits, so the Steel Pick recipe **must** be emitted or that
check fails.

## Steel arrives at rung 1

`steel-processing` costs 50 automation packs in Factorio — rung 1 — and `steel-axe` hangs off it.
**ADR-0018's rung table names no steel at any rung**, and `docs/spec/terra-progression.md` places
only the Steel Furnace, at rung 2. That gap is closed here: **steel is a rung 1 grant; the Steel
Furnace stays at rung 2**, so the metal arrives a rung before the block made of it. ADR-0018 is
amended accordingly.

Rung 1 is also where the boost belongs on its own merits: it is the chapter where the Steam Miner is
carrying the player and hand-mining feels worst.

## The wrench's four verbs

GTCEu 7.0.2's own `en_us.json` shows the wrench doing four distinct things, not one:

| Verb | String | Disposition |
| --- | --- | --- |
| Dismantle | `"Hold left click to dismantle Machines"` | **The Engineer's Pick absorbs it.** Delivered by the tags below and confirmed in-game: a machine breaks and drops its item. |
| Rotate / set facing | `"Rotates Blocks on Rightclick"` | **The Engineer's Pick absorbs it** — *#168, which settled what this row deferred.* The Pick declares the `wrench_rotate` ability; the tags below were never enough. |
| Pipe connections | `"Use Wrench to set Connections, sneak to block Connections"` | **Declined outright** — *#168.* ADR-0017 gives fluid and item logistics to Create and GT's pipes left with its power layer, so the four `wrench_configure*` abilities are deliberately not declared. |
| Multiblock maintenance | `"Pipe is loose. (Wrench)"` | **Already dead** — `config/gtceu.yaml:218` sets `enableMaintenance: false`. |

Rotation is **not** deletable: Factorio has a rotate verb (`R`), so a pack with no way to turn a
machine is missing a mechanic rather than simplifying one. This ADR deferred it rather than deciding
it, because it is a separate interaction from mining and does not block the Terra Slice. *#168 has
since decided both rows: rotation is taken and pipe connections are declined. The two paragraphs
above and the consequence below are left as written and amended in place, rather than rewritten, so
that what this ADR actually decided stays legible.*

## Wood is a log, not a plank

`item-map.json`'s `wood` row named the tag `minecraft:planks`. That mapping made the Iron Pick
unbuildable: log→planks is a vanilla shaped recipe, ADR-0034's sweep removes it, `#140` removes the
2x2 grid that would make it, `#97` forbids the pack emitting a vanilla shaped recipe in its place,
and Factorio has no plank for the corpus to author. **Planks are unobtainable.**

The row moves to a log tag. Factorio's tree drops `wood` directly with no intermediate step, so a log
*is* Factorio's wood and planks were always the less faithful reading. The blast radius is three
corpus recipes — `small-electric-pole`, `wooden-chest` and `shotgun` (the last `undecided` under
`#118`) — and a chest made of logs is arguably the more faithful of the two.

## Considered and rejected

- **No mining tool at all** — delete the tool requirement, mine everything barehanded at full speed.
  This was live for a full round and is the most Factorio-faithful option on paper: Factorio's
  engineer has no tool item, and a stone→iron→diamond ladder would be a *fourth* progression ladder
  in a pack whose ADR-0018 exists because three tech mods shipped three of them. **Rejected on
  identity**: without a pickaxe it is not Minecraft. The pack is a Factorio pack built in Minecraft,
  and the opening gesture is the one place the host game should still be recognisable. The two-tier
  Pick keeps the ladder to two rungs, both of which Factorio itself has, so the fourth-ladder
  objection is answered by the tier count rather than by deleting the item.
- **An undroppable pick with no recipe.** Rejected: recovering a lost tool is a problem Factorio does
  not have, but neither does Factorio have a tool to lose. A one-log recipe is cheaper than an
  inventory restriction and more legible than either.
- **A `character-mining-speed` player attribute, with one pick item that never changes.** This is
  Factorio's literal mechanism and it was the standing recommendation. Rejected in favour of two
  items because Minecraft's rules govern here: a player who researches something and receives a
  *recipe* understands what happened; a player whose numbers silently change does not.
- **Consuming 50 steel plates in the Lab** (`ConsumeItem`). Rejected: Factorio's trigger charges
  nothing, and a 50-plate toll at rung 1 is a real cost the corpus never priced.
- **A general escape hatch in ADR-0031.** Rejected. A narrow exception with a stated reason is safe;
  a general hatch is how "the corpus authors every recipe it contains" quietly stops meaning
  anything. The reason here is unusually clean and does not generalise: the corpus *cannot* contain
  these recipes, because the mechanic they serve does not exist as an item in Factorio at all.

## Consequences

- `docs/factorio-mechanics.md`'s **Manual mining** row takes owner `ADR-0039`, and its notice is
  rewritten — the current text asserts tool-tiering, which this reverses. The row carries the
  amended seconds, and the verdict stays `adapted` rather than becoming `shipped`: the time is now
  the pack's own.
- `CONTEXT.md` gains **Engineer's Pick**.
- ADR-0018's rung table gains steel at rung 1.
- `item-map.json`'s `wood` row moves to a log tag; `small-electric-pole` and `wooden-chest` move with
  it.
- Two pack-authored recipes exist that no corpus regeneration will ever produce. A converter run must
  not remove them.
- Rotation and pipe connections are `#168`'s. The wrench item tags this ADR grants do **not** hand
  them over, which is worth stating because it looks as though they would: GregTech gates rotate and
  the configure verbs on NeoForge `ItemAbility` declarations (`GTItemAbilities.WRENCH_ROTATE`,
  `WRENCH_CONFIGURE` and friends) that a plain `Item` answers false to, while dismantle rides the
  ordinary break path this ADR already covers. Until #168 lands there is one visible seam: GTCEu's
  rotation overlay draws on `toolTypes.contains(WRENCH) || canPerformAction(WRENCH_ROTATE)`, and the
  Pick satisfies the first half, so the overlay appears on a machine while right-click does nothing.
  *#168 has landed and closed that seam from the other end: the Pick declares `wrench_rotate`, so the
  overlay the tags already drew is now honest. The configure verbs stay undeclared, which is a
  decision rather than a remaining gap — see `docs/factorio-mechanics.md`'s Manual mining sub-rules.*

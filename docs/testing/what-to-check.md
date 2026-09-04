# What to check

Before a feature is built, one question has to be answered: **does this need a check, and of what
kind?** This document answers it. It exists so that the answer is looked up rather than re-argued,
and so that "no check" is a decision somebody recorded instead of a step somebody skipped.

The document is keyed on the **claim** a feature makes, not on the harnesses the pack happens to
own. Harnesses come and go; the claims do not. Find the claim, read off the check.

## The six claims

| The feature claims | The check |
|---|---|
| This data parses, and this thing is registered | **Nothing.** Load-time facts are free. |
| Cross-file references resolve | **Static data check** — `tests/`, no game launch. |
| This is emitted into a world | **Fixture row** in `tests/worldgen/expected.json`. |
| This pack logic computes something | **Unit test** in `planetaryfactory_core` or `respoiled`. |
| This block or entity behaves in-world | **Nothing** if vanilla by construction, else **GameTest**. |
| This looks or feels right | **Human on delivery.** |

### Load-time facts are free

Malformed datapack JSON fails the load, loudly, with the offending file named. A biome, a
feature or a noise setting that does not parse never reaches a world, and no check the pack could
write would tell you anything the crash log does not.

**The trap: "a codec catches it" is true for malformed and false for absent.** A codec validates
the files it is handed. It has nothing to say about a file that was never written, a biome that
parses perfectly and is never placed, or a vein registered against a dimension nobody visits.
Absence is the failure mode this row does not cover, and the two rows below are where it is caught.

### Cross-file references resolve

The pack's content is spread across the mod's registries, KubeJS, datapack JSON, loot tables,
blockstates, model and texture files and lang. Each of those parses in isolation. Nothing in
Minecraft's loading checks that they agree with each other: a lang key naming a block that was
never registered, a blockstate pointing at a texture that does not exist, a loot table for a block
that dropped out of the mod — all of these load without complaint and fail in front of a player.

That is what a static data check catches. `tests/flora/test_flora_data.py` is the pattern:
it reads the registered ids out of the mod source, reads the ids out of KubeJS and the datapack,
and asserts that every reference on either side resolves — plus the pack-specific facts that no
file could contradict on its own, such as which marshland carries which tree. It launches no game
and needs no JVM.

**These checks grow by assertion, not by fixture.** Adding a feature to a static data check means
writing the assertion. This is the opposite of the fixture row below, and the difference is real —
do not read the fixture check's "a new body adds data, not code" rule as covering both.

### This is emitted into a world

A registry that loaded is not a world that contains anything. Ore veins, bedrock deposits,
worldgen layers and biomes all have a state in which they parse, register, and are then never
placed. `scripts/worldgen-check.py` launches the pack into a fresh world and asserts against what
the game actually loaded and, for biomes, actually located. See
[the worldgen registry check](./worldgen-registry-check.md).

**A fixture row is unconditional.** Every body-level worldgen fact gets one — there is no
judgement call about whether a given vein is important enough. The launch happens regardless and
rows are free after the first, so the cost of the rule is nil and the cost of re-arguing it per
ticket is not. A new body adds a fixture entry; it does not add harness code.

**A recipe that is REMOVED is the same claim read backwards, and it is the same kind of check.**
A removal is a claim about the loaded recipe manager, which exists only in a running JVM: nothing
static can see that a stock recipe survived a sweep, and nothing static can see a research holding
a lock on an id that no longer resolves. ADR-0034's sweep therefore splits in two —
`tests/factorio/test_recipe_sweep.py` asserts the files agree with each other (the row above), and
0 failed recipes plus no JEI route to a removed idiom is a world load, asserted by a human on
delivery. `#97`'s widened form, which has to see a *surviving stock* grid recipe rather than a
pack-emitted one, is a world load for the same reason.

**That check kind has a name: a world-load recipe-manager assertion.** It is the fixture check's
harness pointed at a different registry — `scripts/worldgen-check.py` already launches a world and
recovers a dump written by a KubeJS script, so asserting the loaded `RecipeManager` is an extra
section in that dump rather than a second harness. It is named here so that a ticket needing it
finds a kind rather than inventing one.

**No such assertion is built, and what each removal gets is decided per claim.** A recipe-manager
dump sees recipes; it does not see *surfaces*. `#140` removed the 2x2 inventory grid, and it did so
in `planetaryfactory_core` — the slots are inactive and refuse both directions, and `slotsChanged`
never resolves a recipe — so every `crafting_shaped` recipe the dump would list is still there and
still unreachable. Nothing the dump could assert would have caught that grid working, and nothing it
could assert would catch it coming back. **That removal's check is therefore a human on delivery**,
with the steps in the ticket, and this paragraph is the recorded reason rather than an omission.
`#97`'s widened form — *no surviving stock recipe is reachable on a surface the pack keeps* — is the
claim the dump does fit, and it is still unbuilt; it is that ticket's to build, at the moment the
pack keeps a surface where reachability is in doubt.

### This pack logic computes something

Where the pack computes rather than declares — Decay's sampler, a Freshness transition, anything
with arithmetic or state in it — the check is an ordinary unit test, run without Minecraft. These
live in `planetaryfactory_core` or in the `respoiled` fork.

The line is between pack logic and game features. A subclass that returns a feature holder is
testing Minecraft, not the pack, and gets nothing.

**Fork-side tests carry their repo.** `respoiled` is a separate clone beside the pack, so its
tests do not run from this one. Write them as `unit (respoiled): <test>` so a reader can see at a
glance that the check is not runnable here.

### This block or entity behaves in-world

Most of what the mod ships is vanilla by construction: a real `SaplingBlock` grows the way a
sapling grows, and a `TreeGrower` subclass hands back a feature holder. Checking those tests
Minecraft. They get nothing.

**A GameTest is warranted when all three hold.** The behaviour

1. runs on world state — ticking, neighbour updates, block interaction; **and**
2. is not inherited unchanged from a vanilla superclass; **and**
3. cannot be reached from a plain JVM unit test without a server.

The supply-area pole (ADR-0036, `#147`) is the first thing in the pack to trip all three, and
**the harness is `#156`** — which is what this section has always said happens at this moment.

The shape of that decision generalises, so it is worth stating once. Most of what looked like
in-world behaviour was arithmetic that had no business needing a server, and it was written to be
reachable without one: the pole's geometry, its FE-to-EU conversion and its rationing are
Minecraft-free classes with unit tests. What was genuinely left over — that a capability lookup
finds a machine, and that inserting energy makes it run — is the part a GameTest exists for.
**Split first, then check what remains**; the residue is usually much smaller than the feature.

Nothing else in the pack trips all three. The rest of the mod content satisfies (1) and fails (2),
which is why declining to test it was the right call.

### This looks or feels right

Sky colour, ground stone, whether a vein yields the intended ore variant under its intended name,
whether a loop is any fun. Some of this is genuinely unautomatable and some of it merely has not
been automated; the document does not pretend to know which in advance. Either way the check is a
human, on delivery, running the steps the ticket lists.

**Human steps live in the ticket, and nowhere else.** They are written in its `## Checks` section
and asserted when it closes. There is no standing manual-check document: one that nobody runs goes
stale the day it is written, and a per-ticket list is verifiable at the moment it matters.

## What a ticket must do

**A ticket that changes pack content carries a `## Checks` section**, one line per claim it makes:

```markdown
## Checks

- emitted → fixture rows for the five Sapros biomes in `tests/worldgen/expected.json`
- references → assertions in `tests/flora/test_flora_data.py` for both trees' loot and lang
- in-world behaviour → none; `SaprosSapling` is a real `SaplingBlock`, vanilla by construction
- looks right → human: land on Sapros, confirm sky and that both marshlands carry their tree
```

Naming the claim and then writing `none` is the point. It makes an unchecked feature a decision
with a reason attached, which is reviewable, instead of an omission, which is invisible.

**Docs and process tickets omit the section.** A hollow `Checks: none` on every meta-ticket trains
readers to skip the section on the tickets where it matters.

**When no seam exists, the ticket does not block.** Fall back to a human check on delivery and say
so. If the fact is not humanly observable either, record `unchecked — <reason>` and ship. A policy
that stops work gets routed around, and the missing seam is a ticket of its own.

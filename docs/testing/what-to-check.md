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

Nothing in the pack trips all three today, and there are no GameTests. The current mod content
satisfies (1) and fails (2), which is why declining to test it was the right call. When something
does trip the trigger, standing up the GameTest harness is its own ticket, filed at that moment.

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

---
status: provisional
---

# Terra's engineer keeps Minecraft's walk, and concrete is what makes you faster

`#170`'s playtest reported that the player "needs to move a lot between patches" and suggested a
walking speed increase. `#207` refused to act on the suggestion before doing the subtraction, on the
grounds that both halves of the starting area's traversal budget were unextracted: nothing had
compared Factorio's character speed to Minecraft's, and `DISTANCES` in
`scripts/build-terra-start.py` was justified by feel while the corpus already held Factorio's own
starting radius.

The subtraction has been done and it says: change nothing. This ADR is here because that outcome is
a decision with a rejected alternative, and `#207` is closed — a closed ticket is a route, not the
state.

## What the corpus says

`character.running_speed` is a sibling key of the `mining_speed` ADR-0039's tiers already read, in
the same prototype: **0.15 tiles/tick**, which at 60 ticks a second is **9.0 tiles/s**. Minecraft's
player walks at **4.317 blocks/s**. A tile and a block are both one metre, so the tick rate is the
only conversion. `starting_resource_placement_radius` is **150 tiles**; `DISTANCES` is
`[34, 48, 62]`, and Terra's four fields sit on the four cardinal faces — iron east, copper north,
coal west, stone south — so a patch-to-patch traversal is a chord and not the radius.

| leg | Factorio | Terra |
| --- | --- | --- |
| hub to furthest field | 16.7 s | 14.4 s |
| two fields, perpendicular | 23.6 s | 20.3 s |
| two fields, opposite | 33.3 s | 28.7 s |

**Both halves drifted, in opposite directions, and they cancel.** Terra's player walks at 48% of the
engineer's speed and its fields sit at 41% of Factorio's starting radius, so every leg comes out at
**0.86× Factorio's time**. The ratio is invariant across legs, which is what keeps this from resting
on a chosen one — the first draft of the comparison measured the hub-to-field radius, which is not
the leg `#170` was complaining about, and the verdict did not move when it was corrected.

## The decision

**No base movement speed is set on Terra.** The player walks at Minecraft's speed, and Terra's
opening costs less walking than Factorio's does. There is no fidelity gap to close.

**`DISTANCES` does not move.** `#207` permitted the geometry to change and did not assume it;
matching Factorio's opening exactly at Minecraft's walk would put the furthest field *further* out
than it currently sits, at about 72 blocks.

**The rejected alternative is the reason this is an ADR.** Setting a flat base speed in
`planetaryfactory_core` was the obvious fix and is refused: it would spend Block Runner's bonus.
§*Terrain modification* is `adapted` specifically so that a **built surface** is the thing that makes
you faster, which is what Factorio's concrete is for. A global buff would deliver the same seconds
while deleting the mechanic that is supposed to earn them, which is a worse outcome than the
slowness it fixes.

**Sprinting is not part of the budget.** Every figure above is walked. Sprinting burns hunger and
`#183` has not decided whether Minecraft's hunger mechanic stays in the pack; a budget that assumed
sprinting would be load-bearing on an undecided mechanic. A measured leg on a fresh world came in at
about 14 s sprint-jumping — roughly 100 blocks, some 23 s walked — which is consistent with the
chord table and is not evidence against it.

**The complaint is real and its cause is legibility.** Factorio lets you zoom out and hold all three
patches in view at once; Minecraft does not, and the `#170` comment names this itself. `#116` (radar
and surface indicators) and `#158` (pole supply-area overlay) are the surfaces that answer it. This
ADR does not answer it and does not claim to.

## Scope

Base movement on foot only. Cars, tanks and spidertrons are §*Personal transport*, `blocked` and
owned by `#121`, which is open and unargued; the two must not be conflated, because a vehicle
decision would be about a capability no rung grants and this is about the walk every player has from
the first tick.

## When to reopen

`starting_resource_placement_radius` is the bound a starting patch may be placed *within*, not where
patches typically land. It is the softest number here and the only one taken as a stand-in rather
than measured. If Factorio's own starting patches cluster well inside 150, the 0.86 flatters Terra,
and this decision should be re-argued against measured patch positions. That is the stated condition;
nothing else here is provisional in a way a further playtest would settle.

## The check

`tests/factorio/test_resource_extract.py` fails if a regenerated corpus drops `running_speed`, if the
tiles/tick-to-tiles/s arithmetic drifts, or if `DISTANCES` moves the opening past Factorio's time —
so the arithmetic this ADR rests on is re-derived rather than trusted, which is ADR-0022's rule and
the reason the extractors exist.

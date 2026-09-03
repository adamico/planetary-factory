---
status: accepted
supersedes: [95, 96, 98]
---

# The Personal Assembler is a planner, not a queue

`#95` decided what the Personal Assembler is — a permanent panel on the inventory screen — and `#96`
established that its mechanism has to be Java. Both described its *behaviour* as Factorio's crafting
queue: pick a recipe, it goes in a list, it completes. `#160` was filed to build that, and grilling it
found the queue model wrong in one place and under-specified in four others.

Factorio's hand-crafting is not a flat queue. Its defining property, and the wiki's own statement of
what separates the hand from an assembling machine, is **chain-crafting**: requesting a recipe whose
ingredients you lack queues the sub-crafts automatically. No document in this repo recorded that, and
a flat queue silently drops it.

## The rule

**The Personal Assembler resolves a Crafting Plan before it crafts anything.** The interaction is
Applied Energistics 2's autocrafting shape, simplified: an amount dialog, then a plan showing every
intermediate and every shortfall, then a single commitment. Refined Storage (MIT) is the readable
implementation of the resolver; AE2 is a UX model only, being LGPLv3 with CC BY-NC-SA art.

The full flow, and the only route in:

1. The inventory is open, so the Assembler panel is open — EMI offers **Fill Recipe** only for the
   screen currently open, so there is no craft-from-anywhere path.
2. EMI: search the item, `R` for its recipes, choose one, press **`+` Fill Recipe**.
3. **Select Amount** — `x1`, `x5`, `all`, and a typed field. `all` is the largest count whose complete
   plan the inventory covers, computed by the resolver, so `all` can never produce a plan that step 5
   then refuses.
4. **Crafting Plan** — the whole tree, flattened, in three categories: `To Craft` for intermediates the
   plan will make, `Missing` for leaves the player must mine or smelt, `Locked` for a recipe the team
   has not researched.
5. **Start**, which is **refused unless the plan is complete**.

## What Start does

Start takes the plan's entire raw cost from the inventory at once, flattens the tree into an ordered
list of crafts, and appends it to the Assembler queue. **The plan is never re-resolved.** Nothing can
change underneath it, because it is already paid for.

## Consequences

- **No recipe type of its own**, which reverses `#96`'s recommendation and `#98`'s wording. `#88`
  already made the hand-craftable set a *predicate* — first category `crafting`, which is Assembling
  Machine 1's set, minus the eleven Factorio withholds — and `tests/factorio/test_subgroup_owner.py`
  already refuses to let any row name the Assembler as a value. One emitted recipe serves both
  surfaces; ADR-0029's speeds do the rest, `× 2` at the machine's 0.5 and `× 1` at the Assembler's 1.
  A second recipe type would emit every hand recipe twice under two ids, which `#97`'s research-unlock
  check would then see as two unlock targets for one craft.
- **ADR-0029's routing sentence is superseded.** `category-map.json` cannot route `hand-crafting` to
  the Assembler, because no recipe in the corpus carries that category — it is declared by
  `character/character` and `god-controller/default` only. The row becomes `!`-routed, and the
  `personal_assembler` machine entry goes, since the Assembler is not a routing destination. What
  ADR-0029 actually decided — speed 1, durations unmultiplied, slowness from serial execution — is
  untouched.
- **The queue is serial and stops dead.** Plans run one at a time. When a completed craft cannot fit
  in the inventory the head pauses and the whole queue waits, Factorio's own 0.15 behaviour. Nothing
  is ever dropped on the ground. Letting a blocked head step aside would give the player parallel
  hand-crafting, and serial execution is the *only* thing making hand-crafting slow once ADR-0029
  removed the multiplier.
- **The plan is the unit of cancellation**, refunding its remaining reservation plus intermediates
  already produced.
- **The resolver plans only through unlocked recipes.** 104 of the 113 hand recipes carry an
  `unlocked_by`, and Researchd locks are per-team, so a locked intermediate is a real state and gets
  its own `Locked` category rather than being folded into `Missing` — the two demand different
  actions from the player.

## Two deliberate departures from Factorio

Recorded because ADR-0021's fidelity axis makes an unmarked departure a defect:

- **Cancellation is by plan, not by item.** Factorio cancels one, five, or all of an item type,
  because its queue is flat and has no plan structure to break. Cancelling an intermediate out of the
  middle of a resolved plan would orphan everything downstream and leave a reservation matching
  nothing.
- **A partial plan cannot be started.** Factorio never offers a chain it cannot complete, so the case
  does not arise there; AE2 *does* start partial plans and stalls until the network supplies the rest.
  We show AE2's information and take Factorio's rule: a blocked head entry in a serial queue is a
  stall with no in-game signal, and the player's fix — go mine it — is what the plan dialog exists to
  tell them before they commit.

## Alternatives considered

- **The flat Factorio queue**, which every document in the repo described until this ADR. Rejected: it
  drops chain-crafting, the mechanic Factorio itself names as the hand's advantage over a machine, and
  it makes the bootstrap tier a long manual sequencing exercise on the pack's only crafting surface.
- **Progressive extraction, AE2's model**, taking ingredients per sub-craft instead of at Start.
  Rejected: our source is the *player's* inventory, not a network's, so a running plan could be
  starved by the player spending its inputs elsewhere, ending in a stall or a half-refund.
- **A recipe browser of our own**, instead of EMI. Rejected on the same grounds as `#95`: it is a
  large surface to write and maintain, and EMI already does it. EMI therefore remains a hard
  requirement of the pack.

## Checks

- **The resolver against the corpus** — static, `tests/factorio/`, no Java and no game: all 113 hand
  recipes resolve to plans that terminate in the 21 known leaf ingredients, with no cycles and no
  unresolvable intermediate. It can land before the mod does, and it fails the day a corpus
  regeneration adds a recipe nothing hand-makes.
- **The queue's behaviour** — the mod's own JUnit: resolve, reserve, tick to completion, cancel and
  refund, pause on a full inventory, and round-trip the data attachment.
- **This thing is registered** — one world load: the panel is present from the first tick, EMI's Fill
  Recipe reaches it, and a plan completes.

## The EMI contract, verified

The entry point rested on one assumption, so it was checked against the installed
`emi-1.1.24+1.21.1+neoforge.jar` before either half was built. EMI is MIT, so its source is legitimate
reference material.

`RecipeFillButtonWidget` computes `canFill = supportsRecipe(recipe) && canCraft(recipe, ctx)`, and
that single field drives both the greyed texture and the click gate. `canCraft` is the implementer's,
so **step 2 holds**: implement `EmiRecipeHandler` directly — *not* `StandardRecipeHandler`, whose
default `canCraft` checks the inventory against the recipe, which is precisely the behaviour we must
not have — and return true unconditionally. The button stays lit with the ingredients missing, which
is what lets the plan do its job of naming them.

One ordering fact follows from the same read: `EmiRecipeFiller.performFill` calls
`craft(recipe, ctx)` and then, on true, `Minecraft.setScreen(handledScreen)`. **A client-side `Screen`
opened synchronously inside `craft()` therefore loses the race** — EMI replaces it a moment later. It
is a last-writer-wins ordering problem at one instant, not a rule confining the Assembler to the
inventory screen, and it does not limit how large a dialog may be.

## The dialogs are server-opened menus

Select Amount and the Crafting Plan are **their own `MenuType`s, opened by the server**, not client
screens and not overlays on the panel.

The reason is not EMI's ordering, which several tricks would sidestep. It is that a plan is server
truth: resolving one reads the player's inventory *and* the team's Researchd state, and Start takes
the reservation. There is a round-trip either way, so the plan may as well live where it is computed.
A client-side dialog would hold a plan the server must re-validate at Start — reintroducing exactly
the re-validation this ADR removed by paying for the plan up front.

So `craft()` sends "plan this recipe, amount N", returns true, and lets EMI restore the inventory
screen; the server resolves and opens the dialog menu. Nothing races EMI, because the server is doing
the opening. `N` comes from `EmiCraftContext.getAmount()`, which is `1` on a click and
`Integer.MAX_VALUE` on shift-click — Factorio's one-and-all, arriving for free.

Two smaller facts worth keeping: the button passes `Integer.MAX_VALUE` on shift-click and `1`
otherwise, arriving as `EmiCraftContext.getAmount()` — Factorio's click-for-one and shift-for-all,
free, and the right initial value for Select Amount. And `EmiRecipeFiller.handlers` is keyed by
`MenuType`, which is what makes the panel being open a precondition of the only route in.

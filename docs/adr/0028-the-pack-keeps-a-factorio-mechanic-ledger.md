---
status: accepted
---

# The pack keeps a Factorio mechanic ledger

The pack's stated ambition is to reproduce Factorio, base game and Space Age. Nothing in the repo
said which mechanics that covers, so the exclusions were made by silence. A `grep` over `docs/adr/`
and the GDD before this ADR found only local exclusions — uranium from Terra's starting area, ore
multiplication, packwiz index rows. Six major Factorio systems had left the pack without anyone
deciding they should.

They landed in `data/pack/subgroup-owner.json`, a recipe-routing table, as side effects: seven
`combat/*` shelves `not_emitted` because #26 dropped Military science and their ingredients fed
nothing downstream — turrets, armour, ammo, guns, capsules and equipment; `logistics/circuit-network`
parked `undecided` with a note conceding "that is a decision this table cannot take from ADR-0017";
`production/module` (beacon), `logistics/transport` (car), and landfill on `logistics/terrain`, all
`undecided`.

This ADR records that the pack keeps `docs/factorio-mechanics.md`: one row per Factorio mechanic,
each with a verdict, so that "we reproduce all of Factorio" stops being an intention and becomes a
list somebody has to disagree with in writing.

## The ledger and the routing table are two axes, and neither derives the other

`subgroup-owner.json` answers **is a Factorio recipe emitted, and on whose machine**. The ledger
answers **does the mechanic exist in the pack, by any means**. A mechanic can exist with no emitted
Factorio recipe behind it at all.

**`not_emitted` is therefore never evidence for `excluded`.** The proof case was already in the repo
when the ledger was written: `combat/defensive-structure` is cut, and #57 still ships a Radar as a
pack machine on a GT chassis with its own research node. A ledger generated from the JSON would have
recorded "radar: excluded" and been wrong about shipped content.

The same split holds against #25. **The ledger decides whether the pack has a mechanic at all; #25
decides where on Terra's ladder it lands.** A row of `planned` does not oblige #25 to place it
pre-launch, or on Terra.

## Five verdicts, and `blocked` is the one the exercise exists for

- **`planned`** — in, not built.
- **`shipped`** — in, built. Earned by being registered *and* by the check its claim warrants under
  `docs/testing/what-to-check.md` passing. Deliberately not "a human has played it": that cannot be
  automated, and requiring it would leave every row permanently `planned`.
- **`adapted`** — in, but Minecraft's shape differs. Carries a mandatory `notice` sentence naming
  what a Factorio player would notice is missing or different. ADR-0010's coarser freshness stages
  and ADR-0005's Emission are the pre-existing examples. **A row whose author cannot write the
  sentence has not decided anything**, and the mandatory field is what exposes that.
- **`blocked`** — wanted, no known implementation. **The bucket this exercise exists to fill.**
- **`excluded`** — deliberately not reproduced, with a written reason. No bare rows.

`blocked` and `excluded` stay apart on purpose. Collapsing them is precisely how an exclusion gets
made without being argued: a mechanic nobody can currently build reads as a mechanic nobody wants.

There is no `undecided`. An undecided row is the failure mode the ledger exists to correct, so every
row ships with a proposed verdict even where the proposal is weak.

## `via` must exist; `candidates` commits to nothing

`via` reuses `subgroup-owner.json`'s owner tokens verbatim — `gregtech`, `create`,
`electro`, `pack`, `kubejs`, `native_mechanic` — so the two files speak one language and one `grep`
crosses both. *`mekanism` was one of them until ADR-0035 took the mod out of the manifest; the
must-exist rule below is what retired the token.* **A `via` value must exist in `index.toml`.** That constraint is the whole thing
keeping the ledger from drifting into fiction about jars the pack does not ship. The routing-only
tokens (`split`, `deferred`, `not_emitted`) are not imported; the five verdicts already cover what
they say.

`candidates` is free text and explicitly commits to no jar. **`pack` is admissible as a candidate
only with a named mechanism**, per ADR-0015's ownership table. Without that rule "we could write it
ourselves" is true of every row, every `blocked` row launders itself into `planned`, and the one
useful bucket empties out.

## `owner`, and why `by-consequence` is owned here

Three states in one field: an ADR or issue link where the decision was already made and is merely
transcribed; **`unargued`** where the ledger is the first place it has been written down — the query
you will actually run; and **`by-consequence`** where the mechanic was dropped as a downstream effect
of a decision about something else and never argued on its own merits.

**`by-consequence` rows are owned by the ledger, not handed back to the ticket that caused them.**
#26 decided how many science tiers stand between spawn and the first launch. It did not decide that
the pack has no combat, and re-opening it to answer for seven shelves it never considered would be
inventing a decision it never took.

## Factorio's names are the row keys, as a declared exception

Row keys are `Gleba` and `Vulcanus`, not Sapros and Ignus — names `CONTEXT.md`'s _Avoid_ lists
otherwise forbid. The exception stands on the same footing as `data/factorio/*.json`: Factorio's
vocabulary is legitimate when quoting Factorio, and the ledger's value is being diffable against
Factorio by someone holding the wiki open. Every body row carries the pack's own name for it in `where` — `Gleba` is `Sapros`, `Aquilo` is
`Gelida` — so no row is untranslatable. The `factorio-` filename prefix mirrors `data/factorio/` and carries the
same signal. The exception is recorded in `CONTEXT.md` itself so that a later agent tidying names
does not "fix" it.

## Prose, and no check

The ledger is Markdown, not JSON. Nothing tests against it, so a machine-readable form buys nothing
and costs the argument — the `notice` sentences and the written exclusion reasons are the content,
and they do not survive a schema.

It stays honest by convention plus discoverability: a body or puzzle ticket updates its own rows, and
a `CLAUDE.md` skill entry is what makes an agent find the ledger at all. Per
`docs/testing/what-to-check.md` this is a design ledger making no runtime claim; a check here would
be testing prose.

## Consequences

- Load-bearing `by-consequence` rows get their own `Grilling:` issue rather than being settled inside
  a row. Settling them in a row would repeat the failure the ledger exists to correct.
- #94 depends on the ledger — `via` naming five mods flat is the Factorio-terms framing it asks for.
- The ledger runs before #25 and hands it a checklist.

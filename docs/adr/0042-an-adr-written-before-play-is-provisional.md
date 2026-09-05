---
status: accepted
supersedes: []
---

# An ADR written before the rung is playable is `provisional`, not `accepted`

Every ADR in this repo carries `status: accepted`. Most of them decide the behaviour of machines
nobody has ever placed, on a Terra nobody has ever played. `accepted` is not what those documents
have earned, and a reader who finds the word on a document about an unbuilt block is being told
something false about how much evidence stands behind it.

**So `status:` takes a fourth value, `provisional`, and a decision argued only from documents takes
it.** An ADR is promoted to `accepted` when the rung it belongs to has been played end to end and
the decision survived. Promotion is an edit to the frontmatter with the play-test named in the
prose; nothing else changes.

## The evidence this is real, not hygiene

ADR-0040 decided Terra's rung-0 drill. In one grilling session against a Factorio player's
expectations, three separate defects surfaced in it, and all three are the same defect:

- It recorded **"drills output onto a belt directly — `excluded`"** and argued the row entirely
  about belts. Factorio has no belt rule — a drill outputs onto **the tile its arrow points at** —
  so the row silently deleted the drill-into-furnace pair, which is the first machine pair a
  Factorio player builds and the one they will look for in the first ten minutes.
- It justified the burner drill as standing on existing ground: "the mod already registers blocks,
  block entities and menus — the supply-area pole, **the furnace ladder**, the trees". There is no
  furnace. Nothing in `mod/src/main/java` or `kubejs/startup_scripts` registers one.
- It assigned rung 1 to `gtceu:lv_miner` and reasoned at length about `nativeEUToFE` wrapping. That
  block deletes an ore block whole into cobblestone off its loot table, which contradicts ADR-0041 —
  accepted *after* ADR-0040 — and would have left stale `OreDelta` entries behind every mined
  position.

None of the three is a lapse of care; ADR-0040 is a carefully argued document. They are what
happens when a decision is checked against other documents rather than against a running game. That
is a property of the *method*, and the method is not going to change — this pack is designed
substantially ahead of being playable, on purpose, because Factorio's shape is known in advance.
What can change is the claim each document makes about itself.

## What `provisional` does and does not mean

- **It is not `proposed`.** A provisional ADR is decided, is being implemented, and binds the code
  written against it. It is the pack's answer today.
- **It is not an excuse to under-argue.** A provisional ADR is argued to the same standard; the
  status records what it was argued *against*, not how hard.
- **Promotion has a named trigger** — the rung played end to end — rather than a maintainer's sense
  that enough time has passed. Without a trigger the value degrades into decoration within a month.

## This ADR, and the drill ADR, are provisional too

ADR-0043 is written in the same condition it describes and takes `provisional` on its own terms.
So does this one: the claim that play-testing is what promotes a decision is itself untested until
a rung has been played and the promotion pass has actually been run. Marking it `accepted` would
reproduce the exact error it exists to name.

## Consequences

- `scripts/adr-backlink-check.sh` reads only the `supersedes:` key and never inspects `status:`, so
  the new value breaks no existing check. Nothing else in `scripts/` or `tests/` parses ADR
  frontmatter.
- **The back-catalogue is not swept by this ADR.** Auditing the existing documents is #181's job,
  not a side effect of accepting this one. What this ADR marks is ADR-0040 and ADR-0043.
- A future ADR states its own status deliberately. "Has this been played?" becomes a question the
  author answers rather than a field they copy from the document above.

## Considered alternatives

- **A tracked ticket alone** (#181), listing the ADRs to re-validate. Rejected as the *only*
  record: it is invisible at the point of failure. The misreading happens to someone who has
  opened one ADR and found `accepted` at the top of it, and a ticket in another system does not
  reach that reader.
- **A paragraph in `docs/agents/domain.md`.** Same objection, one layer weaker — it is guidance
  for sessions that read the map, and the sessions that get misled are the ones that arrive at a
  single document from a search.
- **Nothing formal; note it in ADR-0043's prose.** Rejected because it makes the observation about
  one document when it is true of the corpus.

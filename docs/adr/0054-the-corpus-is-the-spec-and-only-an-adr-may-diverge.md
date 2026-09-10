---
status: provisional
supersedes: []
---

# The corpus is the spec, and only an ADR may diverge from it

ADR-0042 grades ADRs by the evidence behind them: a decision argued from documents rather than
from play is `provisional`. It says nothing about the documents themselves. The repo holds four
kinds of source — the extracted Factorio corpus, the ADRs, the design prose (`docs/gdd.md`,
`docs/planets.md`, the planet and spec documents), and the defaults inherited from upstream mods
and their config files — and **nothing states which of them outranks which.** So a session that
opens `docs/gdd.md` and finds a mechanic described in confident present tense has no way to know
it is reading a sketch rather than a decision.

**The order is: an ADR, then `data/factorio/*.json`, then everything else, which has no standing
at all.**

More precisely, because the ordering is not a simple ladder:

- **The corpus is the default answer.** Where Factorio's own prototypes state a number, a
  topology or a lifecycle, that is what the pack reproduces, and no argument is required to
  follow it.
- **Only an ADR may diverge from the corpus**, and it must say so in its own prose. ADR-0032 cut
  ore multiplication, ADR-0028 declared Factorio names as row keys, ADR-0049 kept Minecraft's
  walk. Each is a deliberate, argued exception. That is the mechanism, and it is the only one.
- **Design prose has no standing to create a divergence.** `docs/gdd.md` and its neighbours are
  informative — a sketch of intent written largely before the corpus pipeline existed. Where
  prose and corpus disagree, the corpus wins by default and the prose is stale until an ADR says
  otherwise.
- **An upstream default is not a decision.** A value the pack inherited by installing a mod —
  a config toggle, a registered block, a balance number — binds nothing until an ADR adopts it.
  Inheriting is not choosing.
- **A player-changeable setting binds nothing.** Anything a player or server admin can alter in
  a menu or a properties file is not a fact about the pack, and no mechanic may be designed
  against a particular value of one.
- **Every divergence appears in `docs/factorio-mechanics.md`.** The ledger already records
  verdicts; this ADR gives it the standing to be the index of exceptions, so a divergence has one
  place to be found rather than being discoverable only by reading the ADR that created it.

## The evidence this is real, not hygiene

One grilling session on #118 — whether the pack has combat — demoted five sources in a row, each
time because the session had treated a document as binding that its author had not meant that way:

- `docs/gdd.md` §6 described Illager raids on an Overseer at a Command Center, with a Cryo-Pod
  manufacturing chain behind it. Read as the design, it made the enemy question look settled. It
  was a pre-fidelity sketch, and Factorio's actual loop — nests absorb pollution and emit the
  attacks — deletes the Overseer, the Command Center, the Cryo-Pod and the kill-switch outright.
- `docs/gdd.md` §1's arrival pressure, and GCyR's space suit under it, produced a four-slot
  conflict with Factorio's six-rung armour ladder that consumed a full round. Factorio has **no**
  space suit: `grep -i "space-suit|spacesuit|oxygen|pressure|life-support"` over
  `data/factorio/*.json` returns nothing, across 163 recipes and 162 technologies. The conflict
  was imported from a substrate the pack builds from source and can change.
- `config/gcyr.yaml`'s `enableOxygen: true` was treated as a constraint. It is an upstream
  default the pack has never decided on, and one line.
- **ADR-0005 rejected per-entity emission** — "score dirty recipe outputs instead of power draw"
  — on the ground that it would cost "tagging every recipe in a GregTech pack, with a permanent
  maintenance burden". `emissions_per_minute` is in the dump. The extraction pipeline that makes
  that cost zero was built *after* ADR-0005, and the ADR still carries `status: accepted` for a
  subsystem nobody has played. ADR-0042 would call it `provisional`; #181 is the audit that has
  not yet reached it.
- `options.txt`'s `simulationDistance:12` was cited as a pack property in support of a delivery
  radius. It is untracked — a local client file. What the pack ships is `default-options`, whose
  values a player overrides at will, and the design floor is 3–5 chunks, *inside* the 112-block
  constant the argument depended on.

None of these is a lapse of care, and the same three are true of them as ADR-0042 found in
ADR-0040: each document is carefully written, each was checked against other documents, and the
method that produced them is not going to change. What was missing is a statement of which
document to believe when two of them disagree.

## What this does not mean

- **It is not "Factorio fidelity always wins".** It is that a divergence costs an ADR. The pack
  diverges deliberately and often, and every one of those divergences stays exactly as valid as
  the day it was argued.
- **It does not demote the design prose to fiction.** `docs/gdd.md` is where intent is worked out
  and it remains the best statement of what the pack is *for*. It is not where mechanics are
  settled.
- **It does not sweep the back catalogue.** Auditing prose against the corpus is not a side
  effect of accepting this. What this ADR marks is the ordering; #181 owns the ADR audit, and the
  prose audit is not yet filed.

## Consequences

- A session that finds a mechanic in `docs/gdd.md` and nothing in `data/factorio/` or an ADR is
  reading a sketch, and should grill it rather than implement it.
- A mechanic the pack inherited from an installed mod and never argued is, by this ADR, undecided
  — which is a larger set than it sounds and will surface as work.
- `docs/factorio-mechanics.md` gains a stated reason to outrank the GDD, which today it does not
  have: it records verdicts but never says it wins.
- This ADR is itself argued only from documents and takes `provisional` on ADR-0042's terms.

## Considered alternatives

- **A paragraph in `docs/agents/domain.md`.** Rejected on ADR-0042's own objection, which applies
  unchanged: the misreading happens to a session that arrived at a single document from a search,
  and guidance in the map does not reach that reader.
- **Rely on ADR-0042 alone.** It grades ADRs by evidence and would eventually mark ADR-0005
  `provisional`, but it says nothing about prose or inherited defaults, which is where four of
  this session's five demotions were.
- **Mark the stale prose instead** — a banner on `docs/gdd.md`. Useful, and not a substitute:
  it states that one document is unreliable rather than which document to believe, and it would
  need repeating on every planet and spec document.

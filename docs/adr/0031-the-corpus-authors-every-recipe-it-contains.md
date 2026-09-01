---
status: accepted
supersedes: [55, 62]
---

# The corpus authors every recipe it contains, and an override needs a recorded reason

`data/factorio/recipe.json` holds 163 Nauvis pre-launch recipes, extracted rather than transcribed
(ADR-0022's precedent, applied to recipes by ADR-0026). `#87`'s converter reads them and emits pack
recipe JSON. What was never stated is which of the two — the corpus or the pack author — wins when
both have an opinion about the same recipe.

The default in force until now was the author's, and it was never decided: it accreted. Two closed
tickets ruled the circuit tiers' ingredient lists "recipe iteration, the dev's own"
([`#55`](https://github.com/adamico/planetary-factory/issues/55),
[`#62`](https://github.com/adamico/planetary-factory/issues/62)), and both did so while assuming the
circuits had to be re-based onto mod items — Create's Electron Tube, Mekanism's Control Circuit —
each of which is redstone-bound, and redstone is on ADR-0021's cut list. The premise was a dead end,
not a preference, and `#125` found the corpus's own ladder closes cleanly on Terra's four ores plus
oil:

| Recipe | Corpus ingredients |
| --- | --- |
| `copper-cable` | 1 copper plate → 2 |
| `electronic-circuit` | 1 iron plate + 3 copper cable |
| `advanced-circuit` | 2 electronic circuit + 2 plastic bar + 4 copper cable |
| `processing-unit` | 20 electronic circuit + 2 advanced circuit + 5 sulfuric acid |

No redstone anywhere. The problem `#125` existed to solve was an artefact of mapping onto mod items,
and it disappeared the moment the corpus was allowed to author.

**The corpus authors every recipe it contains.** An override is a departure from Nauvis, and every
departure is recorded in the overrides file with its reason. The overrides file thereby stops being
a place for awkward cases — barrelling, the `!`-routed categories — and becomes the register of
every place the pack knowingly differs from Factorio.

This inverts the default. A recipe the author has taste about is not thereby exempt: taste is an
argument to be written down in the overrides file, where the next reader can weigh it, not a silent
win over the extracted data.

## Considered Options

- **The author authors, the corpus fills the gaps.** The status quo by accretion. It produced two
  closed tickets asserting a ruling made under a false premise, and would have produced a
  hand-authored circuit ladder chasing a redstone substitute Factorio never needed.
- **The corpus authors everything, no overrides.** Purest, and unshippable: barrelling is a free
  Create mechanic (ADR-0017), twelve `*-barrel` recipes would duplicate it, and five categories are
  deliberately unrouted. Departures exist; the question is only whether they are recorded.
- **Per-recipe judgement, as now, but written down.** Indistinguishable in practice from the status
  quo — with no stated default, "written down" is whatever the session remembers to write.

## Consequences

- **`#55` and `#62` are superseded in part.** The three circuit tiers stay first-party items on
  their own tags — that ruling holds, and `#62`'s removal of GregTech's and Mekanism's competing
  circuit lines holds with it. What does not hold is their ingredient lists being the author's:
  they are the corpus's. Neither ticket knows this;
  [`#132`](https://github.com/adamico/planetary-factory/issues/132) is the ticket for that gap.
- **`copper-cable` maps onto `electroenergetics:copper_wire`** rather than becoming a fourth
  first-party item. The item map's rule, stated in its header: borrow an existing item unless the
  row sits on a rung boundary or a mod's competing line would give a parallel escape — then author
  it and recipe-remove the competitor. Circuits author because they carry progression; cable
  borrows because it carries none.
- **The overrides file needs a `reason` field**, and `#87`'s static check should assert every entry
  has one. Without it the register is a list of names and the rule is decorative.
- **`data/pack/item-map.json`'s remaining rows get cheaper.** With the corpus authoring and the
  borrow/author test stated, most rows are transcription rather than decision — which is what
  ADR-0026 claimed the item map would be and what the "hardest rows" framing had made it not.

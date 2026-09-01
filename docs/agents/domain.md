# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root, or
- **`CONTEXT-MAP.md`** at the repo root if it exists — it points at one `CONTEXT.md` per context. Read each one relevant to the topic.
- **`docs/adr/`** — read ADRs that touch the area you're about to work in. In multi-context repos, also check `src/<context>/docs/adr/` for context-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo (most repos):

```
/
├── CONTEXT.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

Multi-context repo (presence of `CONTEXT-MAP.md` at the root):

```
/
├── CONTEXT-MAP.md
├── docs/adr/                          ← system-wide decisions
└── src/
    ├── ordering/
    │   ├── CONTEXT.md
    │   └── docs/adr/                  ← context-specific decisions
    └── billing/
        ├── CONTEXT.md
        └── docs/adr/
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal — either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders) — but worth reopening because…_

## An ADR names the tickets it supersedes

Tickets are the route; the ADRs are the state. ADRs cite the tickets they came from, and nothing
points the other way, so a closed ticket keeps asserting a decision an ADR has since overridden and
the next session to read it is misled by a document that looks settled. Two rules close that gap,
and neither replaces the other — the first covers the session that reads the map, the second the
session that opens a ticket from a search.

**Read the ADRs before asking the first question.** Grep `docs/adr/`, `CONTEXT.md`,
`docs/factorio-mechanics.md` and `docs/gdd.md` for the ticket's own nouns before composing a
grilling round. A question the ADRs answer is not a question.

**Declare the supersede in frontmatter, and back-link the ticket.** When an ADR **contradicts a
closed ticket's stated answer**, name it:

```yaml
---
status: accepted
supersedes: [55, 62]
---
```

Then comment on each named ticket, saying what no longer holds and what to read instead. The comment
must contain the literal `ADR-00NN`; the convention is a `## Superseded by ADR-00NN` heading, but any
comment naming the ADR satisfies the check.

Two limits keep the rule from being ignored within a month:

- **Contradiction only.** An ADR that merely narrows or re-scopes a ticket gets a courtesy comment,
  not a frontmatter entry. A rule that fires on every ADR/ticket overlap gets skipped.
- **Closed tickets only.** An open ticket is still live and is read as route, not state; its
  staleness is the frontier's problem.

`supersedes:` is a flat list and an index — it carries no reasons. The degree and the argument live
in the ADR's prose, in exactly one place.

**After committing an ADR that declares a `supersedes:` key, run `scripts/adr-backlink-check.sh`.**
It reads every ADR's frontmatter, asks `gh` for each named ticket's comments and fails on any that
does not name the ADR. It touches the network, so it is a script and not a pytest, and it belongs to
this workflow step rather than to any aggregate check run.

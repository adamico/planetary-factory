# Out-of-scope knowledge base

One file per **rejected concept**, so that a `wontfix` keeps its reasoning after the issue scrolls
away, and so a request that has already been settled is recognised instead of re-argued.

`/triage` reads every file here while gathering context and surfaces a match by concept rather than
by keyword — a ticket asking for "quarries" should match `mining-automation.md` without sharing a
word with it.

## What belongs here

**A rejected enhancement, and nothing else.**

- **Not bugs.** A bug closed as `wontfix` gets a polite explanation on the issue and stops there.
- **Not things already built.** Closing "we already have this" is not a rejection, and filing it here
  would teach the dedup check to reject a feature the pack ships. Point at where it lives instead.
- **Not deferrals.** "Not now", "after Terra ships", "once #25 lands" are scheduling, not scope. They
  stay open, or become a ticket. A reason that will expire is not a reason.

## What belongs somewhere else

Two existing records already own most of this pack's exclusions, and a concept file must not
duplicate or contradict either:

- **A Factorio mechanic the pack does not reproduce** → `docs/factorio-mechanics.md`. That ledger has
  a finer vocabulary than this directory does: `excluded` means argued and rejected, `blocked` means
  wanted with no known implementation. Collapsing the second into a rejection here is exactly the
  failure ADR-0028 exists to prevent.
- **A design decision with a considered alternative** → `docs/adr/`. An ADR records what was chosen
  *and* what was turned down, in place, and is amended rather than superseded.

What is left over is what this directory is for: rejected requests about tooling, workflow, mod
choices and pack scope that are not Factorio mechanics and did not warrant an ADR.

## File format

`kebab-case-concept.md`, named so the directory listing is legible without opening anything. A short
design document, not a database row: what the request was, why it is out of scope in terms of the
pack's scope or a technical constraint, and a `## Prior requests` list of every issue that has asked.
A second issue asking the same thing appends to that list rather than starting a new file.

## Changing your mind

Delete the file. Old issues stay closed as historical record; the new request that prompted the
reconsideration goes through normal triage.

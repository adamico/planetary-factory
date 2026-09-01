---
status: accepted
---

# Factorio's circuit network is Minecraft's redstone

`#101` built the mechanic ledger to catch mechanics dropped by silence, and the circuit network was
the worst of them: `data/pack/subgroup-owner.json` parked `logistics/circuit-network` as `undecided`
with a note saying *"Most likely `not_emitted`, but that is a decision this table cannot take from
ADR-0017"*, and nothing anywhere else took it either. A Factorio player who wires a chest to an
inserter would have found nothing, and no document would have said why.

The ledger row that answers it carries more argument than a row should — it overturns a prior verdict
and decides where an entire Factorio system lives — so it is recorded here and the row points at it.

## The decision

**The pack has Factorio's circuit network, `adapted`, and it is supplied by vanilla redstone plus
Create's redstone line. No mod is missing and none is wanted.**

Vanilla supplies the wire, the comparator, the repeater and the observer. **Create ships its own
redstone layer on top** — Redstone Link, Powered Latch, Pulse Repeater, Threshold Switch, Stockpile
Switch, Smart Observer, Display Link and Nixie Tubes — which between them cover most of what
Factorio's combinators, lamps and display panels exist to do.

The mandatory `notice`, because Minecraft's shape differs: **the wires are redstone**, so a signal is
a strength from 0 to 15 on a block-to-block circuit rather than a named channel on a coloured wire.
There is no reading a whole belt's contents off one wire, and no arithmetic on a signal beyond what a
comparator does.

## Why this was nearly recorded as `blocked`

An earlier draft of the row read `blocked` — *wanted, no known implementation* — on the grounds that
no installed mod owns a circuit network.

**That was a category error, and it is the reason this ADR exists.** The ledger's `via` field names
mods, so the question "which mod supplies this?" comes naturally and returns nothing here. But the
mechanic is not missing; it is in the base game. `blocked` would have put a system Minecraft has
shipped since 2009 on a list of things the pack cannot do, and a later reader would have gone looking
for a mod to fill a hole that was never there.

The general lesson, which applies past this row: **`via: native_mechanic` is a real answer, and
looking for a capability one mod owns can miss a mechanic the game already has.** Barrelling
(ADR-0017) is the other instance already in the repo.

## What this does not decide

**The supply question is separate and still open.** `#58` cut redstone from Terra entirely — no vein,
an empty `underground_ores` step — so the mechanic exists while its crafting material does not.
`#62` records the same problem hitting the authored green circuit. That is a resource question for
`#25`, not a verdict on the mechanic, and the two were conflated in the row before this ADR split
them. It is tracked as `#119`, and the row stays `adapted` whatever the answer.

**Whether the combinator recipes are emitted is the other axis.** ADR-0028 keeps the ledger and
`subgroup-owner.json` from reading each other, and this is the case that shows why: a mechanic
supplied by vanilla and Create needs no emitted recipe to exist, so `logistics/circuit-network`
staying `not_emitted` would contradict nothing here. That routing decision remains
`subgroup-owner.json`'s to take.

**Circuit-controlled inserters and belts stay `unargued`**, because they depend on `#102`'s answer
about whether Create's Mechanical Arm is Factorio's inserter. Nothing here presumes it.

## Consequences

- `docs/factorio-mechanics.md`'s Circuit network row is owned by this ADR rather than `unargued`.
- `#101`'s fourth follow-on grilling — *"does the pack have a circuit network?"* — is answered and is
  not filed as a ticket.
- The two-networks-on-one-wire trick (red and green) stays `excluded`: redstone has one channel and
  the mechanic has no analogue.

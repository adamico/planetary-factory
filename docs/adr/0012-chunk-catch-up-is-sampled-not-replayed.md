---
status: accepted
---

# Chunk catch-up is sampled in closed form, never replayed

Decay runs everywhere, which includes chunks nobody is standing in. With no per-stack timestamp
there is nothing to compute elapsed time from, so ADR 0011's fork tracks a last-swept tick per chunk
in a `SavedData` and catches up on chunk load.

The obvious implementation of "catch up" is to replay the missed passes. It cannot be done. A chunk
unloaded overnight is on the order of a million missed 30-tick passes, and the roll is **binomial
per item**, so a replay is a loop over passes wrapping a loop over items wrapping a loop over stacks
— on the chunk-load path, where the cost lands as a visible freeze on a player flying into fresh
terrain.

**We sample the outcome directly instead.** Advancing `n` items through up to four stages over `t`
passes is a negative-binomial / Erlang-4 problem with a closed form; one sample per stack gives the
same distribution as the replay would, in constant time. Total catch-up is **capped at the item's
full four-stage lifetime** — beyond that everything in the stack is Spoilage regardless, so an
unbounded elapsed time collapses to a constant and an eight-hour absence costs no more than a
four-minute one.

## Considered Options

- **Replay the missed passes.** The only implementation that is obviously correct by construction,
  and the reason this ADR has to explain why the code does something cleverer. Ruled out on cost
  above.
- **Batch the replay** — collapse passes into larger steps and roll fewer times. Cheaper, but it is
  an approximation with no clean error bound, and it still scales with elapsed time. The closed form
  is both exact and O(1); there is no reason to take an approximate middle.
- **Freeze Decay in unloaded chunks.** Free, and issue #14 named the outcome unacceptable: a chunk
  loader becomes a fridge, which is the same class of exploit as the ME network in ADR 0013.
- **Per-stack timestamps to compute elapsed time properly.** This is ADR 0010's rejected design
  returning through the back door — it reintroduces the stack fragmentation that ADR ruled out.

## Consequences

The catch-up path and the sweep path compute the same thing by different means and share no code.
That is the surprise this ADR exists to explain: a reader finding two implementations of Decay will
assume one is dead. Neither is. They must be kept in agreement by test, not by inspection.

That test is a plain JVM unit test in the fork, no Minecraft involved — assert the sampler's mean
and variance against the Erlang-4 expectation for each published shelf life, and assert that
sampling `t` passes agrees with replaying `t` passes for small `t`. The arithmetic is exactly the
kind that is wrong by a factor of four and goes unnoticed for months, and it is also the only part
of Decay that can be tested without a world.

Per-chunk timestamps become save data. Once shipped, changing the catch-up model means either
migrating that data or accepting that every chunk catches up wrongly once.

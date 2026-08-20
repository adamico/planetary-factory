# The Decay sampler test

Decay's arithmetic is the only part of the pack that can be tested without launching Minecraft, and
it is the part most likely to be wrong by a constant factor and stay wrong for months. So it has a
plain JVM unit test, required by ADR 0012.

## Running it

The test lives in the `respoiled` fork, not in this repo. From the sibling clone:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd ../respoiled-src && ./gradlew :decay:test
```

`:decay` is a Gradle module with no Minecraft, no NeoForge and no mixins on its classpath. That is
deliberate: it is what makes "testable without a world" structural rather than a promise someone has
to keep. The run takes a few seconds. `./gradlew build` runs it too, so a jar cannot be built past a
failing sampler.

## What it asserts

- **Erlang-4 moments per published shelf life.** For each `spoiltime` the pack ships — 1200, 600, 50,
  40, 30 and 10 passes — twenty thousand sampled lifetimes must match the analytic mean `4/p` and
  variance `4(1-p)/p²`, and the coefficient of variation must match `sqrt(1-p)/2`. This is the
  assertion that catches a factor-of-four error in how stages divide a shelf life.
- **Sampling agrees with replaying.** For small elapsed times, the closed-form distribution over
  ending stages is compared bucket by bucket against a literal loop that rolls one pass at a time.
  The closed form is the only thing that runs in production; the replay exists solely as the obvious
  implementation to check it against.
- **Saturation and bounded cost.** Past the catch-up cap the answer stops moving, so an eight-hour
  absence costs no more than a four-minute one.
- **Conservation.** A sampled stack never gains or loses items, and decay never runs backwards.

## When it fails

A moment assertion failing by a clean factor means the stage/shelf-life conversion moved. A
replay-agreement failure means the closed form and the Bernoulli model have diverged, which is a real
bug in the sampler. Both are statistical assertions with fixed seeds, so they do not flake — a
failure is a change in behaviour, not noise.

Note that these bounds are tolerances around a distribution, not exact equalities. Tightening them
without recomputing the standard error is how a stable test becomes a flaky one.

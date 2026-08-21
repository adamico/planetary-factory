# The worldgen registry check

One automated check covers every body's worldgen: `scripts/worldgen-check.py`. It launches
the pack into a freshly created world, reads the ore vein, bedrock ore, bedrock fluid and
worldgen layer registries the game actually loaded, and compares them to
`tests/worldgen/expected.json`.

```bash
scripts/worldgen-check.py                # launch, dump, compare, tear the world down
scripts/worldgen-check.py --keep-world   # leave saves/WorldgenCheck for inspection
scripts/worldgen-check.py --dump-only    # re-compare the last dump without launching
```

Exit codes: `0` everything matched, `1` expectations failed (each one printed), `2` no dump
was produced — the game failed to reach world load, so read `logs/latest.log` first.

## Adding a body

Add an entry under `bodies` in `tests/worldgen/expected.json`. Nothing else changes:

```json
"ignus": {
  "dimension": "planetaryfactory:vulcanus",
  "ore_veins": { "planetaryfactory:ignus_coal": { "weight": 120, "layer": "ignus_rock" } },
  "bedrock_ores": { "planetaryfactory:ignus_tungsten_deposit": {
      "materials": ["gtceu:tungsten"], "depleted_yield_at_least": 1 } },
  "bedrock_fluids": { "gtceu:lava_deposit": {
      "fluid": "minecraft:lava", "depleted_yield_at_least": 1 } },
  "forbidden_ore_veins": ["gtceu:iron"],
  "worldgen_layer": "ignus_rock",
  "biomes": ["planetaryfactory:ignus_barren_plains"]
}
```

Every field is optional except `dimension`. For each named vein the check asserts that it
loaded, that its `dimension_filter` includes the body's dimension, that it names the expected
layer, that its weight is the expected one, and — the failure a codec cannot catch — that the
layer it names actually covers that dimension. `forbidden_ore_veins` asserts the reverse: that
a vein does *not* reach this body.

An `ore_veins`, `bedrock_ores` or `bedrock_fluids` object that is **present but empty** means
something stronger than "nothing to check": it asserts the body carries none of that kind of
thing at all, and the check walks the *entire* loaded registry and fails if any entry reaches
that dimension. That is the form a barren body needs, because a list of forbidden names goes
stale the moment another ticket adds one. A vein whose `dimension_filter` is empty reaches
nothing — GregTech matches a level against that set with `anyMatch` — so the walk sees it as
reaching nowhere, which is the same answer the game gives. Electro asserts this of its veins
(ADR-0009); Sapros asserts it of all three (ADR-0016).

`worldgen_layer` asserts the body's layer loaded and covers its dimension. Named veins already
imply that for the layer they name, so this field is for a body with no veins at all, whose
layer nothing else would mention — and whose absence a player only discovers by prospecting.

`biomes` lists biomes the body's generator must actually emit. This is not the same as being
registered or being listed in a biome source: a biome parameter point that is closest to no
point in the reachable noise space parses fine and generates nowhere, and no codec catches it.
The dump answers it by sampling the generator's own biome source over a grid of quart positions
around the origin, through the climate sampler worldgen itself uses — it loads no chunks, so
it costs nothing. Sapros's five biomes are the reason it exists.

Bedrock ore deposits are checked for presence, dimension,
material set, and a depleted yield above zero; bedrock fluid deposits for presence,
dimension, the fluid they hold, and the same non-zero floor, with
`forbidden_bedrock_fluids` as their reverse.

The comparison itself has its own test, `tests/worldgen/test_compare.py`, which runs it
against synthetic dumps in a second and needs no game:

```bash
tests/worldgen/test_compare.py
```

Run it after editing the fixture. It answers whether the check would notice a given
failure; only the full run answers what the game actually loaded.

## How it works, and why this way

The read half is `kubejs/server_scripts/registry_dump.js`, which writes
`local/registry-dump.json` on world load — but only when `local/registry-dump.request.json`
exists, which the harness creates and removes. In normal play the script does nothing. It also
logs the same JSON behind a `WORLDGEN_DUMP ` marker, and the harness reads the log when the
file is missing: KubeJS's class filter blocks `java.nio`, so file access goes through KubeJS's
own path constants and JSON helper, and the log is the handle that cannot be taken away.

The plan for this seam named two handles for reading the registries. Neither survived contact:
`/gtceu dump_data` does not exist in GTCEu 7.0.2 (its commands are `cape`, `place_vein` and
`ui_editor`), and KubeJS's local web server exposes `/api/registries/{ns}/{path}/keys`, which
answers which veins exist but not which dimension one filters to or which layer it names. So
the dump reads the live registry through `GTRegistries` and writes it itself.

The weights themselves are not scripted either. `GTCEuServerEvents.oreVeins` cannot be used in
GTCEu 7.0.2: it resolves the ore vein registry from a registry access that does not contain it
during world load, and throws `Missing registry: gtceu:ore_vein` before any handler runs. So
Terra's weights ship as datapack overrides, generated by `scripts/build-terra-vein-weights.py`.

## The world is rebuilt every run

`tests/worldgen/world-template/` holds a bare `level.dat`, copied into `saves/WorldgenCheck`
at the start of each run and deleted at the end. A world persists the dimension list it was
created with, so reusing one answers a question about the past. Everything the check reads is
reloaded from the datapacks on every load.

Changing a dimension's *generator* — its noise settings, its biome source, its biomes — needs no
regeneration either; only the dimension list is persisted. Regenerate the template when a body
ticket adds a **dimension**: create a world in-game once,
copy its `level.dat` over the template, and note the change in the commit. Ore veins, deposits
and layers need no regeneration — they are not persisted in the save.

## What still needs a human

The check runs unattended, but it drives the real client, so it needs a graphical session —
it cannot run over a bare SSH connection or in CI without one, and fails at
`glfwGetPrimaryMonitor` if there is no display.

Four things per body remain a human's job on delivery, because the registries cannot show
them: that the sky is right, that the ground is the intended stone, that digging into a
vein yields the intended ore variant under its intended name, and that a Bedrock Ore Miner
and a Fluid Drilling Rig actually draw from the body's deposits. The registries can say a
deposit loaded, filters to the right dimension and never depletes to zero; only a rig can
say it turns.

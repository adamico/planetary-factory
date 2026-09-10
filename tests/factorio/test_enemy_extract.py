#!/usr/bin/env python3
"""Assert the extracted enemy corpus still says what ADR-0055 reads off it.

`scripts/factorio-enemy-extract.py` reads a 28MB Factorio dump that is not in the repo, so
nothing here re-runs it. What is checkable without the dump -- and without launching the
game -- is whether the *committed* output still supports the decision:

  - **the evolution factor is re-derived from Factorio's own published formula, not
    trusted.** Factorio updates evolution every 60 ticks by
    `(time + pollution*pollution_factor + kills*destroy_factor) * (1 - evolution)^3`, and
    that differential equation has a closed form: after `t` seconds of time alone,
    `1 - 1/sqrt(1 + 2*time_factor*t)`. Two things are asserted and they are different
    assertions: stepping the corpus's own coefficient must agree with the closed form of
    that same coefficient, which proves the *algebra*; and an hour stepped from the corpus
    must land where an hour of Factorio's own published 4e-06 lands, which proves the
    *file*. A hand-edited coefficient fails here and nowhere in a running game.
  - **absorption is stated as a rate against the thing that emits.** A nest absorbs
    `absolute + proportional * chunk` per second, and both halves are the rule -- dropping
    either is a different loop. The success line reports it against the Boiler's own
    emission rate so the two are read together, but no balance between them is asserted:
    what a nest ought to keep up with is a decision, and a decision is a diff to a design
    document.
  - **an ammo-fed turret's damage is its magazine's, and the magazine is extracted.** A gun
    turret and a rocket turret state no damage at all, so a corpus that stopped at the
    turret would hold no damage figure for two of the three turrets #227 names. Every
    turret whose `damage_source` is `ammo` must have extracted ammo of its own category
    that deals some.
  - **the damage walk still walks.** Four prototypes are named as the ones it must get
    right, each for a different reason and each of which produced a different
    plausible-looking wrong number while #227 was being written: a premature wriggler's
    `source_effects` hold a *negative* poison damage the attacker pays itself, and its two
    `target_effects` sum to a hit neither of them is; a small spitter's damage is 1 in a
    `stream` prototype and 12 in the game, so reading the base without the modifier is a
    twelvefold error; a laser turret's is in a `beam` prototype and reads as "no damage at
    all" if the reference is not followed; and a gun turret genuinely has none, because a
    magazine decides it -- a number appearing there means the walk picked up something that
    is not the turret's.
  - **the Nauvis discriminant still discriminates, in both directions.** The dump holds
    Gleba's spawners and wrigglers too, and they are extracted rather than filtered: a run
    that marks them Nauvis would put pentapods in Terra's nests. The mark is read off the
    nests' own `result_units`, so the two must agree both ways -- a unit a Nauvis nest
    spawns and the file calls Gleba's is a tier that silently vanishes from every wave.
  - **every band spawns something.** A spawner's `result_units` are piecewise-linear weights
    against the evolution factor; interpolated across 0 to 1, some unit must always have
    weight. A band where every weight is zero is a nest that absorbs and sends nothing, and
    it is invisible in the file.
  - **emission is a map keyed by pollutant, and it keeps its sign.** Space Age emits
    `spores` beside `pollution`, and a biochamber's rate is negative. A scalar column would
    have silently dropped both.

Usage: tests/factorio/test_enemy_extract.py
"""
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

# Factorio's tick rate, and the interval its evolution update runs on -- which is also the
# seconds a minute holds, and so the conversion from an absorption rate to an emission one.
TICKS_PER_SECOND = 60

# Factorio's own published coefficients, which are the anchors the corpus is checked
# against: the closed form and the stepped simulation are fed the *same* extracted number,
# so their agreement proves the algebra rather than the file, and these prove the file.
PUBLISHED = {"time_factor": 4e-06, "pollution_factor": 9e-07, "destroy_factor": 0.002}

# An hour of game time with no pollution and no kills, which is the anchor the closed form
# and the stepped simulation have to agree on.
ONE_HOUR = 3600

# The four prototypes the damage walk must get right, and the wrong number each one gives
# when it is got wrong.
DAMAGE_TRAPS = {
    "small-spitter": "damage is 1 in a stream prototype and 12 in the game",
    "small-wriggler-pentapod-premature": "source_effects hold -0.95, which is not a hit",
    "laser-turret": "damage is in a beam prototype; unfollowed it reads as no damage",
    "gun-turret": "has no damage of its own -- a magazine decides it",
}

# Nauvis's two nests, and Gleba's two, which are extracted and must stay marked apart.
NAUVIS_SPAWNERS = {"biter-spawner", "spitter-spawner"}
GLEBA_SPAWNERS = {"gleba-spawner", "gleba-spawner-small"}

# The prototype ADR-0048's Boiler is authored against, and the one that emits most per
# minute of anything the pack has shipped.
BOILER = "boiler"


def evolution_closed_form(time_factor, seconds):
    """`1 - 1/sqrt(1 + 2*k*t)` -- the solution of `de/dt = k(1-e)^3` from e = 0.

    This is the re-derivation. It is not read off the corpus and it is not read off the
    simulation; both have to arrive at it.
    """
    return 1 - 1 / math.sqrt(1 + 2 * time_factor * seconds)


def evolution_stepped(settings, seconds, pollution=0.0, kills=0.0):
    """Factorio's own update, run a second at a time.

    `(time_factor + pollution*pollution_factor + kills*destroy_factor) * (1 - e)^3`, applied
    once per second because `time_factor` is stated per 60 ticks.
    """
    evolution = 0.0
    for _ in range(seconds):
        delta = (
            settings["time_factor"]
            + pollution * settings["pollution_factor"]
            + kills * settings["destroy_factor"]
        )
        evolution += delta * (1 - evolution) ** 3
    return evolution


def weight_at(points, evolution):
    """A spawn point's weight at an evolution factor, interpolated as Factorio does.

    Piecewise linear between the stated points, and clamped to the end values outside them.
    """
    if evolution <= points[0]["evolution_factor"]:
        return points[0]["weight"]
    if evolution >= points[-1]["evolution_factor"]:
        return points[-1]["weight"]
    for low, high in zip(points, points[1:]):
        if low["evolution_factor"] <= evolution <= high["evolution_factor"]:
            span = high["evolution_factor"] - low["evolution_factor"]
            if span == 0:
                return high["weight"]
            share = (evolution - low["evolution_factor"]) / span
            return low["weight"] + share * (high["weight"] - low["weight"])
    return 0.0


def main():
    path = ROOT / "data" / "factorio" / "enemy.json"
    if not path.is_file():
        sys.exit(f"missing {path} -- run scripts/factorio-enemy-extract.py")
    data = json.loads(path.read_text(encoding="utf-8"))

    failures = []
    units = {u["name"]: u for u in data["units"]}
    spawners = {s["name"]: s for s in data["spawners"]}
    turrets = {t["name"]: t for t in data["turrets"] + data["worms"]}
    emitters = {e["name"]: e for e in data["emissions"]}

    # -- evolution, re-derived rather than trusted -------------------------------------
    evolution = data["enemy_evolution"]
    for coefficient in ("time_factor", "pollution_factor", "destroy_factor"):
        if not evolution.get(coefficient):
            failures.append(f"enemy_evolution has no {coefficient} -- ADR-0055 names all three")
    for coefficient, published in sorted(PUBLISHED.items()):
        if evolution.get(coefficient) != published:
            failures.append(
                f"enemy_evolution.{coefficient} is {evolution.get(coefficient)}, not "
                f"Factorio's own {published} -- the corpus was edited by hand"
            )
    if not failures:
        closed = evolution_closed_form(evolution["time_factor"], ONE_HOUR)
        stepped = evolution_stepped(evolution, ONE_HOUR)
        if abs(closed - stepped) > 1e-6:
            failures.append(
                f"an hour of evolution steps to {stepped:.8f} but the closed form of the "
                f"same coefficient gives {closed:.8f} -- one of them is not Factorio's rule"
            )
        published_hour = evolution_closed_form(PUBLISHED["time_factor"], ONE_HOUR)
        if abs(stepped - published_hour) > 1e-6:
            failures.append(
                f"an hour of time alone now evolves to {stepped:.6f} where Factorio's own "
                f"coefficient gives {published_hour:.6f}"
            )
        # The destroy term is the counter-intuitive one ADR-0055 leans on: clearing nests
        # evolves the swarm. One kill at zero evolution is exactly the coefficient.
        one_kill = evolution["destroy_factor"] * (1 - 0.0) ** 3
        if abs(one_kill - evolution["destroy_factor"]) > 1e-12:
            failures.append("the destroy term no longer scales by (1 - evolution)^3 at zero")

    # -- absorption, re-derived against what emits ---------------------------------------
    boiler = emitters.get(BOILER)
    if boiler is None:
        failures.append(
            f"{BOILER} is not in emissions -- it is the prototype ADR-0048's Boiler is "
            "authored against and the dirtiest thing the pack has shipped"
        )
    for name in sorted(NAUVIS_SPAWNERS):
        spawner = spawners.get(name)
        if spawner is None:
            failures.append(f"{name} is missing -- Terra's nests are the two Nauvis spawners")
            continue
        if not spawner["nauvis"]:
            failures.append(f"{name} is marked not-Nauvis; it is on `enemy-base`")
        rate = (spawner["absorptions_per_second"] or {}).get("pollution") or {}
        if not rate.get("absolute"):
            failures.append(f"{name} absorbs no flat rate -- absorption is ADR-0055's input")
            continue
        if not rate.get("proportional"):
            failures.append(
                f"{name} has no proportional absorption; the rule is a flat rate PLUS a "
                "share of the chunk, and dropping either half changes the loop"
            )
        if boiler and not boiler["emissions_per_minute"].get("pollution"):
            failures.append(
                f"{BOILER} emits no pollution -- there is then nothing for {name} to "
                "absorb, and the two halves of ADR-0055's loop no longer meet"
            )

    # -- the damage walk, and the four numbers it gets wrong when it is wrong ------------
    for name, why in DAMAGE_TRAPS.items():
        entity = units.get(name) or turrets.get(name)
        if entity is None:
            failures.append(f"{name} is gone from the corpus; it is one of the check's controls")
            continue
        hit = entity["attack"]
        if hit is None:
            failures.append(f"{name} has no attack at all -- {why}")
            continue
        if name == "gun-turret":
            if hit["damage_source"] != "ammo" or hit["damage_per_shot"] is not None:
                failures.append(
                    f"gun-turret now states {hit['damage_per_shot']} damage from "
                    f"{hit['damage_source']}; {why}, so the walk picked up something else"
                )
            continue
        if hit["damage_per_shot"] is None or hit["damage_per_shot"] <= 0:
            failures.append(f"{name} deals {hit['damage_per_shot']} -- {why}")
            continue
        # The product is the game's, so it is re-derived from its own two terms.
        derived = sum(d["amount"] for d in hit["damages"]) * hit["damage_modifier"]
        if abs(derived - hit["damage_per_shot"]) > 1e-9:
            failures.append(
                f"{name} states {hit['damage_per_shot']} but its own damages and modifier "
                f"derive {derived} -- the file was edited by hand"
            )
        if any(d["amount"] < 0 for d in hit["damages"]):
            failures.append(
                f"{name} carries a negative damage; {why} and source_effects are what the "
                "attacker pays, not what the target takes"
            )
    spitter = units.get("small-spitter")
    if spitter and spitter["attack"]["damage_base"] == spitter["attack"]["damage_per_shot"]:
        failures.append(
            "small-spitter's base and per-shot damage are equal -- its stream states 1 and "
            "its modifier is 12, so the modifier has stopped being applied"
        )

    # -- Nauvis stays Nauvis, and Gleba stays extracted -----------------------------------
    for name in sorted(GLEBA_SPAWNERS):
        spawner = spawners.get(name)
        if spawner is None:
            failures.append(
                f"{name} is gone; Gleba's spawners are extracted rather than filtered, and "
                "they are what shows the Nauvis discriminant discriminating"
            )
        elif spawner["nauvis"]:
            failures.append(f"{name} is marked Nauvis -- that puts pentapods in Terra's nests")
    nauvis_units = {u["name"] for u in data["units"] if u["nauvis"]}
    if not nauvis_units:
        failures.append("no unit is marked Nauvis -- every nest would spawn nothing")
    for name in nauvis_units:
        if "pentapod" in name:
            failures.append(f"{name} is marked Nauvis; pentapods are Gleba's")

    # A unit is Nauvis's because a Nauvis nest lists it, so the two have to agree both ways.
    listed = {
        row["unit"]
        for name in NAUVIS_SPAWNERS
        for row in (spawners.get(name) or {}).get("result_units", [])
    }
    for name in sorted(listed - nauvis_units):
        failures.append(
            f"{name} is spawned by a Nauvis nest but is not marked Nauvis -- the mark is "
            "read off the nests, so the two cannot disagree without one of them being wrong"
        )
    for name in sorted(nauvis_units - listed):
        failures.append(f"{name} is marked Nauvis but no Nauvis nest spawns it")

    # -- an ammo-fed turret's damage is its magazine's, and the magazine is extracted -------
    ammo_by_category = {}
    for row in data["ammo"]:
        ammo_by_category.setdefault(row["ammo_category"], []).append(row)
    for name, turret in sorted(turrets.items()):
        hit = turret["attack"] or {}
        if hit.get("damage_source") != "ammo":
            continue
        category = hit.get("ammo_category")
        armed = [a for a in ammo_by_category.get(category, []) if a["damage_per_shot"]]
        if not armed:
            failures.append(
                f"{name} takes `{category}` ammo and no extracted ammo of that category "
                "deals damage -- the corpus then holds no damage figure for it at all"
            )

    # -- every evolution band spawns something -------------------------------------------
    for name in sorted(NAUVIS_SPAWNERS):
        spawner = spawners.get(name)
        if not spawner:
            continue
        rows = spawner["result_units"]
        if not rows:
            failures.append(f"{name} has no result_units -- it spawns nothing at any band")
            continue
        for step in range(0, 101):
            band = step / 100
            total = sum(weight_at(r["spawn_points"], band) for r in rows)
            if total <= 0:
                failures.append(
                    f"{name} has no unit with weight at evolution {band:.2f} -- a nest that "
                    "absorbs and sends nothing"
                )
                break
        for row in rows:
            if row["unit"] not in units:
                failures.append(f"{name} spawns {row['unit']}, which is not in units")

    # -- emissions keep their key and their sign -----------------------------------------
    if not emitters:
        failures.append("emissions is empty -- it is the input ADR-0055 moves Emission onto")
    pollutants = {p for row in data["emissions"] for p in row["emissions_per_minute"]}
    if "spores" not in pollutants:
        failures.append(
            "no prototype emits `spores`; the field is a map keyed by pollutant precisely "
            "because Space Age adds one, and a scalar column would have dropped it"
        )
    negative = [
        row["name"]
        for row in data["emissions"]
        if any(v < 0 for v in row["emissions_per_minute"].values())
    ]
    if not negative:
        failures.append(
            "no prototype emits a negative rate; a biochamber cleans the air and dropping "
            "the sign would make it dirty"
        )
    for row in data["emissions"]:
        if not row["emissions_per_minute"]:
            failures.append(f"{row['name']} is in emissions with no rate at all")

    # -- the settings blocks ADR-0055's rules are argued in --------------------------------
    expansion = data["enemy_expansion"]
    if expansion["min_expansion_cooldown"] >= expansion["max_expansion_cooldown"]:
        failures.append("expansion cooldowns are not a range")
    if expansion["settler_group_min_size"] > expansion["settler_group_max_size"]:
        failures.append("settler group sizes are not a range")
    if expansion["min_expansion_distance"] >= expansion["max_expansion_distance"]:
        failures.append("expansion distances are not a range")
    group = data["unit_group"]
    if group["min_group_gathering_time"] >= group["max_group_gathering_time"]:
        failures.append("group gathering times are not a range")
    if not group["max_unit_group_size"]:
        failures.append("unit_group has no max_unit_group_size -- a wave with no cap")
    if not data["pollution"].get("diffusion_ratio"):
        failures.append("pollution has no diffusion_ratio -- ADR-0005's cloud keeps that rule")

    for failure in failures:
        print(f"FAIL  {failure}")
    if failures:
        sys.exit(1)

    hours = evolution_stepped(data["enemy_evolution"], ONE_HOUR)
    absorbed = (
        spawners["biter-spawner"]["absorptions_per_second"]["pollution"]["absolute"]
        * TICKS_PER_SECOND
    )
    print(
        f"ok  {len(data['units'])} units ({len(nauvis_units)} Nauvis), "
        f"{len(data['spawners'])} spawners, {len(data['turrets'])} turrets, "
        f"{len(data['emissions'])} emitters; an hour evolves to {hours:.5f} by Factorio's "
        f"own formula and a nest absorbs {absorbed:.0f}/min against a boiler's "
        f"{emitters[BOILER]['emissions_per_minute']['pollution']}/min"
    )


if __name__ == "__main__":
    main()

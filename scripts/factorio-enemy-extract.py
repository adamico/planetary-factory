#!/usr/bin/env python3
"""Extract Factorio's enemy, turret and emission prototypes -- the numbers ADR-0055 reads.

ADR-0055 decides Terra's enemy model as one loop: entities emit, a nest absorbs what its
chunk holds, absorbed emission buys an attack group, a global evolution scalar decides which
tier the group is made of, and expansion founds new nests on its own timer. Every figure in
that decision -- absorption rates, the three evolution coefficients, expansion cooldowns,
group sizes, unit health, damage and speed, and the per-entity emission rate -- is Factorio
prototype data the pack had never extracted. This is that extraction, and it is the eighth.

Same dump as the other seven extractors and the same provenance block in
`data/factorio/README.md`, so a dump still on disk feeds all eight.

Five things this script decides, because they are properties of the data:

  - **Scope is every enemy prototype, with Nauvis marked rather than filtered.** The dump's
    fourteen `unit` prototypes include six Gleba wrigglers, and its four `unit-spawner`s
    include two Gleba spawners. They are extracted and carry `nauvis: false`, the way
    `fuel.json` carries `in_corpus`: a filter here would make a prototype the pack has no
    use for indistinguishable from one nobody extracted, and the Gleba rows are what show
    that the Nauvis discriminant discriminates. **Nauvis membership is read off the
    spawners' own `autoplace.control`, not off a name list** -- `enemy-base` is Nauvis's
    control, and a unit is Nauvis's when a Nauvis spawner can spawn it.
  - **Damage is resolved through the delivery chain, and the modifier is applied.** A
    biter's damage sits inline in its `attack_parameters` (`behemoth-biter`, 90 physical). A
    spitter's and a worm's does not: the ammo's delivery names a `stream` prototype and the
    damage lives in *that* prototype's area action, at an amount of **1**, multiplied by the
    attack's own `damage_modifier` of 12. Reading the inline field alone yields nothing for
    half the enemies in the game; reading the stream's amount without the modifier yields 1
    where the game does 12. Both halves are recorded -- `damage_base`, `damage_modifier` and
    the product -- so the check re-derives rather than trusts.
  - **A player turret's damage is its ammo's, and that is recorded as an absence.** The
    gun turret and the rocket turret state a range, a cooldown and no damage at all: what
    they do is decided by the magazine loaded into them, which is an `ammo` item and not a
    turret field. `damage_source` says `ammo` there, `inline` for a biter and `stream` for a
    spitter. A null with no reason beside it would read as a failed extraction.
  - **Emissions are extracted from every prototype that states one, crafting or not.** The
    field is `energy_source.emissions_per_minute` and 24 prototypes across seven types carry
    it -- furnaces, assemblers, the boiler, drills, a reactor, a lab and an agricultural
    tower. `machine.json`'s "its own item recipe is in the corpus" scope rule would drop the
    heating tower and the biolab while ADR-0055 is still deciding what emits; `in_corpus`
    records the distinction without acting on it, as `fuel.json` does. The value is a map
    keyed by pollutant, because Space Age adds `spores` beside `pollution` and a biochamber's
    rate is **negative**.
  - **The map settings are carried whole.** `enemy_evolution`, `enemy_expansion`,
    `unit_group` and `pollution` are small, flat and entirely coefficients; taking a subset
    would be deciding which of ADR-0055's rules gets numbers, and that is the ADR's call and
    not a script's.

What this script does not do is decide anything. ADR-0055 holds the rule.

Usage:

    scripts/factorio-enemy-extract.py           # finds the dump, writes data/factorio/enemy.json
    scripts/factorio-enemy-extract.py --dump PATH
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

# Nauvis's own worldgen control for enemy bases. Gleba's spawners are not on it.
NAUVIS_CONTROL = "enemy-base"

# The prototype types a player-built turret lands under. Factorio splits them by what feeds
# the gun -- magazine, grid power, or a fluid -- and the pack reads all three the same way.
TURRET_TYPES = ("ammo-turret", "electric-turret", "fluid-turret")

# Worms are `turret` proper: enemy-force, stationary, no item and no recipe.
WORM_TYPE = "turret"

# What a wall is, and what a gate is. Same body, one of them opens.
WALL_TYPES = ("wall", "gate")

# What a target takes is `target_effects`; `source_effects` is what the attacker pays
# itself, and reading it as damage is a sign error rather than a rounding one. This is a
# property of the data and is kept apart from the art keys below, which are only a walk that
# would otherwise spend most of its time in sprites.
NOT_DAMAGE = ("source_effects",)
NO_ACTIONS = ("animation", "sound", "cyclic_sound", "graphics_set", "icon")

# The four map-settings blocks ADR-0055's rules are argued in.
SETTINGS_BLOCKS = ("enemy_evolution", "enemy_expansion", "unit_group", "pollution")

UNITS = {"k": 1e3, "M": 1e6, "G": 1e9, "T": 1e12}


def energy(raw):
    """Factorio's `801kJ`/`9600kW` strings as a number, or `None`.

    The same parse `factorio-machine-extract.py` does; the raw string is kept beside the
    number wherever a check re-derives it.
    """
    if raw is None:
        return None
    if isinstance(raw, (int, float)):
        return float(raw)
    text = str(raw).strip()
    for suffix in ("J", "W"):
        if text.endswith(suffix):
            text = text[: -len(suffix)]
            break
    multiplier = 1.0
    if text and text[-1] in UNITS:
        multiplier = UNITS[text[-1]]
        text = text[:-1]
    try:
        return float(text) * multiplier
    except ValueError:
        return None


def resistances(prototype):
    """Resistances as a list of rows, whatever shape the prototype states them in.

    A unit with no resistances writes `{}` and one with them writes a list, so a caller
    that assumes either gets the other on the very next prototype.
    """
    stated = prototype.get("resistances")
    if isinstance(stated, dict):
        stated = list(stated.values())
    if not isinstance(stated, list):
        return []
    return [
        {
            "type": entry.get("type"),
            "decrease": entry.get("decrease", 0),
            "percent": entry.get("percent", 0),
        }
        for entry in stated
        if isinstance(entry, dict)
    ]


def collect_damage(node, referenced, source, out, seen):
    """Every damage a *target* takes from an action tree, following the delivery chain.

    Three properties of the data are load-bearing here, and each of them was a wrong number
    before it was handled:

      - **`source_effects` are not damage dealt.** A premature wriggler's attack heals its
        own poison by -0.95 in `source_effects` while dealing 3.75 physical and 3.75 poison
        in `target_effects`. A walker that takes the first `damage` it finds reads that
        prototype as healing the player, at a negative number that no check would think to
        look for.
      - **One attack can deal several damages.** That same wriggler deals two, and its hit
        is their sum -- 7.5, which is neither of them.
      - **The delivery may be a reference.** A biter's damage is inline; a spitter's and a
        worm's is in the `stream` prototype the delivery names, a laser turret's is in the
        `beam` one, and a shell's is in the `projectile` one. Following none of them leaves
        half the enemies in the game, every electric turret and most of the ammo stating no
        damage at all.
    """
    if isinstance(node, list):
        for entry in node:
            collect_damage(entry, referenced, source, out, seen)
        return
    if not isinstance(node, dict):
        return

    if node.get("type") == "damage" and isinstance(node.get("damage"), dict):
        damage = node["damage"]
        out.append({"amount": damage.get("amount"), "type": damage.get("type"), "via": source})
        return

    for field in ("stream", "beam", "projectile"):
        name = node.get(field)
        if isinstance(name, str) and (field, name) not in seen:
            seen.add((field, name))
            prototype = referenced.get(field, {}).get(name)
            if prototype:
                collect_damage(
                    [prototype.get("initial_action"), prototype.get("action")],
                    referenced,
                    f"{field}:{name}",
                    out,
                    seen,
                )

    for key, value in node.items():
        if key in NOT_DAMAGE or key in NO_ACTIONS:
            continue
        if isinstance(value, (dict, list)):
            collect_damage(value, referenced, source, out, seen)


def attack(prototype, referenced):
    """An entity's attack, flattened to what ADR-0055's arithmetic reads.

    `damage_per_shot` is the product the game does: every damage the target takes, summed,
    times the attack's own `damage_modifier`, which is 1 when unstated. Both terms are kept
    so the check re-derives rather than trusts -- a small spitter's stream states 1 and hits
    for 12.
    """
    parameters = prototype.get("attack_parameters")
    if not isinstance(parameters, dict):
        return None

    modifier = parameters.get("damage_modifier", 1)
    damages = []
    collect_damage(parameters, referenced, "inline", damages, set())
    cooldown = parameters.get("cooldown")

    base = sum(d["amount"] for d in damages) if damages else None
    # `ammo` is not a null: a gun turret's damage is decided by the magazine loaded into it,
    # which is an item prototype and not a turret field. A bare null would read as a failed
    # extraction rather than as the absence it is.
    source = damages[0]["via"] if damages else "ammo"
    per_shot = base * modifier if base is not None else None

    return {
        "type": parameters.get("type"),
        "ammo_category": parameters.get("ammo_category"),
        "range": parameters.get("range"),
        "min_range": parameters.get("min_attack_distance", parameters.get("min_range")),
        "cooldown": cooldown,
        "cooldown_deviation": parameters.get("cooldown_deviation"),
        "damage_base": base,
        "damages": damages,
        "damage_modifier": modifier,
        "damage_per_shot": per_shot,
        # Shots a second at the stated cooldown, which is in ticks at Factorio's 60.
        "shots_per_second": round(60 / cooldown, 6) if cooldown else None,
        "damage_source": source,
    }


def unit_row(name, prototype, referenced, nauvis_units):
    return {
        "name": name,
        "nauvis": name in nauvis_units,
        "max_health": prototype.get("max_health"),
        "healing_per_tick": prototype.get("healing_per_tick"),
        "movement_speed": prototype.get("movement_speed"),
        "vision_distance": prototype.get("vision_distance"),
        "distraction_cooldown": prototype.get("distraction_cooldown"),
        "max_pursue_distance": prototype.get("max_pursue_distance"),
        # What a unit costs a nest to send: the absorbed emission it takes to join a group.
        "absorptions_to_join_attack": prototype.get("absorptions_to_join_attack") or {},
        "spawning_time_modifier": prototype.get("spawning_time_modifier"),
        "resistances": resistances(prototype),
        "attack": attack(prototype, referenced),
    }


def spawn_points(entry):
    """One `result_units` row: the unit, and its weight against the evolution factor.

    Factorio states these as `[evolution_factor, weight]` pairs and interpolates between
    them; the pairs are what the band table is, so they are carried as pairs rather than
    flattened into a tier-per-band guess.
    """
    name, points = entry
    return {
        "unit": name,
        "spawn_points": [
            {"evolution_factor": pair[0], "weight": pair[1]} for pair in points
        ],
    }


def spawner_row(name, prototype, referenced):
    autoplace = prototype.get("autoplace") or {}
    return {
        "name": name,
        "nauvis": autoplace.get("control") == NAUVIS_CONTROL,
        "max_health": prototype.get("max_health"),
        "healing_per_tick": prototype.get("healing_per_tick"),
        # The absorption rule itself: a flat rate plus a share of what the chunk holds.
        "absorptions_per_second": prototype.get("absorptions_per_second") or {},
        # `[max, min]` ticks -- the cooldown shortens as the nest fills with pollution.
        "spawning_cooldown": prototype.get("spawning_cooldown"),
        "spawning_radius": prototype.get("spawning_radius"),
        "spawning_spacing": prototype.get("spawning_spacing"),
        "max_count_of_owned_units": prototype.get("max_count_of_owned_units"),
        "max_friends_around_to_spawn": prototype.get("max_friends_around_to_spawn"),
        "call_for_help_radius": prototype.get("call_for_help_radius"),
        "autoplace_control": autoplace.get("control"),
        "autoplace_probability": autoplace.get("probability_expression"),
        "result_units": [spawn_points(entry) for entry in prototype.get("result_units") or []],
        "resistances": resistances(prototype),
        "attack": attack(prototype, referenced),
    }


def turret_row(name, prototype, referenced, corpus):
    source = prototype.get("energy_source") or {}
    return {
        "name": name,
        "type": prototype.get("type"),
        "in_corpus": name in corpus,
        "max_health": prototype.get("max_health"),
        "resistances": resistances(prototype),
        "energy_type": source.get("type"),
        "energy_buffer": energy(source.get("buffer_capacity")),
        "drain": energy(source.get("drain")),
        "attack": attack(prototype, referenced),
    }


def ammo_row(name, prototype, referenced, corpus):
    """One `ammo` item, and what firing it does.

    A gun turret and a rocket turret state no damage of their own -- the magazine decides
    it -- so a corpus that stops at the turret has no damage figure for two of the three
    turrets ADR-0055 would arm a base with. The ammo's `ammo_type` is the same action tree
    the enemies' is, walked the same way, and `ammo_category` is the join back to the turret
    that accepts it.
    """
    ammo_type = prototype.get("ammo_type")
    if isinstance(ammo_type, list):
        # Quality-varying ammo states a list; the first entry is the base one.
        ammo_type = ammo_type[0] if ammo_type else None
    damages = []
    if isinstance(ammo_type, dict):
        collect_damage(ammo_type, referenced, "inline", damages, set())
    return {
        "name": name,
        "in_corpus": name in corpus,
        "ammo_category": prototype.get("ammo_category")
        or (ammo_type or {}).get("ammo_category"),
        "magazine_size": prototype.get("magazine_size"),
        "damages": damages,
        "damage_per_shot": sum(d["amount"] for d in damages) if damages else None,
    }


def wall_row(name, prototype, corpus):
    return {
        "name": name,
        "type": prototype.get("type"),
        "in_corpus": name in corpus,
        "max_health": prototype.get("max_health"),
        "resistances": resistances(prototype),
        "opening_speed": prototype.get("opening_speed"),
    }


def emission_rows(dump, corpus):
    """Every prototype stating an `energy_source.emissions_per_minute`, whatever its type.

    ADR-0055 moves Emission off ADR-0005's EU/t proxy and onto this field, so the scope is
    the field itself rather than any one prototype type: seven types carry it, and the
    negative rates (`biochamber`, `captive-biter-spawner`) are as real as the positive ones.
    """
    rows = []
    for type_name, prototypes in sorted(dump.items()):
        if not isinstance(prototypes, dict):
            continue
        for name, prototype in sorted(prototypes.items()):
            if not isinstance(prototype, dict):
                continue
            source = prototype.get("energy_source")
            if not isinstance(source, dict):
                continue
            rates = source.get("emissions_per_minute")
            if not isinstance(rates, dict):
                continue
            rows.append(
                {
                    "name": name,
                    "type": type_name,
                    "in_corpus": name in corpus,
                    "emissions_per_minute": rates,
                    "energy_type": source.get("type"),
                    "energy_usage": energy(
                        prototype.get("energy_usage")
                        or prototype.get("energy_consumption")
                        or prototype.get("consumption")
                    ),
                }
            )
    return rows


def corpus_names(path):
    """The Nauvis pre-launch recipe corpus's names, for `in_corpus`.

    Missing is not fatal: this extractor's own numbers do not depend on it, and a fresh
    clone that has not run the recipe extractor should still be able to run this one.
    """
    if not path.is_file():
        return set()
    return {r["name"] for r in json.loads(path.read_text(encoding="utf-8"))}


def extract(dump, corpus):
    referenced = {
        field: dump.get(field) or {} for field in ("stream", "beam", "projectile")
    }
    spawners = dump.get("unit-spawner") or {}

    nauvis_units = {
        entry[0]
        for prototype in spawners.values()
        if (prototype.get("autoplace") or {}).get("control") == NAUVIS_CONTROL
        for entry in prototype.get("result_units") or []
    }

    units = [
        unit_row(name, prototype, referenced, nauvis_units)
        for name, prototype in sorted((dump.get("unit") or {}).items())
    ]
    spawner_rows = [
        spawner_row(name, prototype, referenced) for name, prototype in sorted(spawners.items())
    ]
    worms = [
        turret_row(name, prototype, referenced, corpus)
        for name, prototype in sorted((dump.get(WORM_TYPE) or {}).items())
    ]
    turrets = [
        turret_row(name, prototype, referenced, corpus)
        for type_name in TURRET_TYPES
        for name, prototype in sorted((dump.get(type_name) or {}).items())
    ]
    ammo = [
        ammo_row(name, prototype, referenced, corpus)
        for name, prototype in sorted((dump.get("ammo") or {}).items())
    ]
    walls = [
        wall_row(name, prototype, corpus)
        for type_name in WALL_TYPES
        for name, prototype in sorted((dump.get(type_name) or {}).items())
    ]

    settings = (dump.get("map-settings") or {}).get("map-settings") or {}
    if not settings:
        sys.exit("no map-settings in the dump -- every coefficient ADR-0055 reads lives there")

    out = {
        "units": units,
        "spawners": spawner_rows,
        "worms": worms,
        "turrets": turrets,
        "ammo": ammo,
        "walls": walls,
        "emissions": emission_rows(dump, corpus),
    }
    for block in SETTINGS_BLOCKS:
        if block not in settings:
            sys.exit(f"map-settings has no `{block}` -- ADR-0055 argues its rules in it")
        out[block] = settings[block]
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument(
        "--out", type=Path, default=REPO / "data" / "factorio" / "enemy.json"
    )
    parser.add_argument(
        "--recipes",
        type=Path,
        default=REPO / "data" / "factorio" / "recipe.json",
        help="the recipe corpus `in_corpus` is read against",
    )
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    out = extract(dump, corpus_names(args.recipes))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")

    nauvis = [u for u in out["units"] if u["nauvis"]]
    print(
        f"{len(out['units'])} units ({len(nauvis)} Nauvis), "
        f"{len(out['spawners'])} spawners, {len(out['worms'])} worms, "
        f"{len(out['turrets'])} turrets, {len(out['ammo'])} ammo, "
        f"{len(out['walls'])} walls, "
        f"{len(out['emissions'])} emitters"
    )
    print(f"wrote      {args.out.relative_to(REPO)}\n")

    print(f"{'unit':34} {'hp':>5} {'speed':>6} {'dmg':>6}  {'source':26} join")
    for unit in out["units"]:
        hit = unit["attack"] or {}
        join = ", ".join(f"{k} {v}" for k, v in sorted(unit["absorptions_to_join_attack"].items()))
        print(
            f"{unit['name']:34} {unit['max_health'] or 0:5} "
            f"{unit['movement_speed'] or 0:6.3f} {hit.get('damage_per_shot') or 0:6.1f}  "
            f"{hit.get('damage_source') or '-':26} {join or '-'}"
        )
    print()
    for spawner in out["spawners"]:
        rate = spawner["absorptions_per_second"].get("pollution") or {}
        print(
            f"{spawner['name']:24} {spawner['max_health'] or 0:5} hp  "
            f"absorbs {rate.get('absolute', 0)}/s + {rate.get('proportional', 0)} of chunk  "
            f"cooldown {spawner['spawning_cooldown']}  units "
            f"{', '.join(r['unit'] for r in spawner['result_units'])}"
        )
    print()
    evolution = out["enemy_evolution"]
    print(
        f"evolution  time {evolution['time_factor']}  pollution "
        f"{evolution['pollution_factor']}  destroy {evolution['destroy_factor']}"
    )
    expansion = out["enemy_expansion"]
    print(
        f"expansion  cooldown {expansion['min_expansion_cooldown']}-"
        f"{expansion['max_expansion_cooldown']} ticks  group "
        f"{expansion['settler_group_min_size']}-{expansion['settler_group_max_size']}"
    )
    group = out["unit_group"]
    print(
        f"groups     gather {group['min_group_gathering_time']}-"
        f"{group['max_group_gathering_time']} ticks  max size {group['max_unit_group_size']}"
    )
    print()
    for row in out["emissions"]:
        rates = ", ".join(f"{k} {v}/min" for k, v in sorted(row["emissions_per_minute"].items()))
        print(
            f"{row['name']:28} {row['type']:20} {rates:24} "
            f"{'in corpus' if row['in_corpus'] else '-'}"
        )


if __name__ == "__main__":
    main()

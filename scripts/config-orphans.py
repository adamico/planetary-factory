#!/usr/bin/env python3
"""Report config/ entries that belong to no installed mod (ADR-0024 territory).

Mods come and go through the CurseForge client, which removes the jar and leaves
`config/` untouched. `scripts/pack-check.sh --prune` catches up the manifest;
nothing catches up the configs, and they are tracked in git, so the orphans
accumulate in the repo forever.

There is no registry mapping a config file to the mod that wrote it — the name is
the only evidence. So this reports, it does not delete: matching is a heuristic
and a wrong guess deletes a live mod's settings. `--fix` deletes, and only after
you have read the list.

Matching, in order of trust:
  1. the modIds declared in the installed jars' mods.toml / fabric.mod.json
  2. the display names in the same files, and the jar filenames themselves,
     for mods whose config is named after the product rather than the id
  3. IGNORE below — entries owned by the loader, the launcher or the game
     rather than by any mod, which would otherwise read as orphans forever

Usage: scripts/config-orphans.py [--fix]
"""
import glob
import io
import os
import re
import shutil
import subprocess
import sys
import zipfile

# Not written by any mod, so no modId will ever claim them.
IGNORE = {
    "fml.toml",                 # the NeoForge loader
    "defaultoptions",           # Default Options' own payload, not its config
    "configuration-options.json",
    "test-config.json",
    "yacl.json5",               # YetAnotherConfigLib, named for its abbreviation
}

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
META = ("META-INF/neoforge.mods.toml", "META-INF/mods.toml", "fabric.mod.json")


def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower())


def read_jar(z, names):
    """Collect the names one jar declares, including the ones it nests."""
    listing = z.namelist()
    for entry in META:
        if entry in listing:
            text = z.read(entry).decode("utf-8", "replace")
            if entry.endswith(".json"):
                names.update(norm(m) for m in re.findall(r'"(?:id|name)"\s*:\s*"([^"]+)"', text))
            else:
                names.update(norm(m) for m in re.findall(r'^\s*(?:modId|displayName)\s*=\s*"([^"]+)"', text, re.M))
            break
    # Jar-in-jar: a nested library writes its own config while having no jar of
    # its own in mods/, so skipping these reports live configs as orphaned.
    for nested in listing:
        if nested.startswith("META-INF/jarjar/") and nested.endswith(".jar"):
            names.add(norm(re.split(r"-\d", os.path.basename(nested))[0]))
            try:
                with zipfile.ZipFile(io.BytesIO(z.read(nested))) as inner:
                    read_jar(inner, names)
            except (zipfile.BadZipFile, OSError):
                continue


def owners():
    """Every string an installed jar might have named its config after."""
    names = set()
    for jar in glob.glob(os.path.join(REPO, "mods", "*.jar")):
        names.add(norm(re.split(r"-\d", os.path.basename(jar))[0]))
        try:
            z = zipfile.ZipFile(jar)
        except (zipfile.BadZipFile, OSError):
            continue
        with z:
            read_jar(z, names)
    return {n for n in names if len(n) > 2}


def orphans():
    known = owners()
    out = []
    for entry in sorted(os.listdir(os.path.join(REPO, "config"))):
        if entry in IGNORE or entry.startswith("."):
            continue
        path = os.path.join(REPO, "config", entry)
        stem = entry if os.path.isdir(path) else entry.rsplit(".", 1)[0]
        n = norm(stem)
        # A config is claimed either way round: `sodium-mixins.properties` extends
        # its owner's name, `craftingtweaks-common.toml` for modId `craftingtweaks`
        # is exact, and `ae2-client.toml` is a truncation of `appliedenergistics2`.
        if any(n.startswith(o) or o.startswith(n) for o in known):
            continue
        out.append(entry)
    return out


def main():
    fix = "--fix" in sys.argv[1:]
    if sys.argv[1:] and not fix:
        print(f"unknown argument: {sys.argv[1]}", file=sys.stderr)
        print("Usage: scripts/config-orphans.py [--fix]", file=sys.stderr)
        return 2

    found = orphans()
    if not found:
        print("OK — every config entry maps to an installed mod.")
        return 0

    print(f"ORPHAN — {len(found)} config entr(ies) match no installed mod:")
    for e in found:
        print(f"  config/{e}")
    print()
    if not fix:
        print("Read the list before deleting any of it. A mod whose config is named")
        print("after neither its modId nor its jar lands here too — that is a false")
        print("positive, and belongs in IGNORE in this script, not in the bin.")
        print("Then: scripts/config-orphans.py --fix")
        return 1

    paths = [os.path.join("config", e) for e in found]
    # Most of config/ is tracked, but not all of it — .gitignore excludes the
    # per-user client config (config/InventoryHUD/ and friends), and `git rm`
    # fails the whole batch on the first path it does not know. So the two kinds
    # are separated and deleted their own way.
    tracked = set(subprocess.run(
        ["git", "ls-files", "-z", "--"] + paths,
        cwd=REPO, check=True, capture_output=True, text=True,
    ).stdout.split("\0")) - {""}
    git_paths = [p for p in paths if any(t == p or t.startswith(p + "/") for t in tracked)]
    plain_paths = [p for p in paths if p not in git_paths]

    # git rm, not rm, wherever git knows the file: the deletion is then reviewable
    # in the diff and recoverable from history if a guess turns out wrong.
    if git_paths:
        subprocess.run(["git", "rm", "-r", "-q", "--"] + git_paths, cwd=REPO, check=True)
    for p in plain_paths:
        full = os.path.join(REPO, p)
        shutil.rmtree(full) if os.path.isdir(full) else os.remove(full)

    print(f"Deleted {len(git_paths)} tracked entr(ies) with git rm — review the diff and commit.")
    if plain_paths:
        print(f"Deleted {len(plain_paths)} untracked (gitignored) entr(ies) outright:")
        for p in plain_paths:
            print(f"  {p}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env bash
# Assert the installed jars still match the tracked packwiz manifest (ADR-0024).
#
# `mods/` is gitignored, so nothing in git notices a jar being added, removed or
# swapped. The manifest does: `packwiz refresh` rewrites index.toml from what is
# actually on disk, so if refreshing changes a tracked file, the pack has drifted
# from what git records.
#
# Three separate things are checked, because none alone is enough:
#   1. refresh changed something tracked — catches edits to indexed pack content
#      and to the two unmanaged fork jars
#   2. MISSING: a metafile names a jar that is not installed — refresh hashes the
#      *metafiles*, not the jars they point at, so a metafile bumped to a version
#      nobody downloaded refreshes perfectly clean
#   3. STRAY: a jar is installed that nothing accounts for — the managed jars are
#      excluded from the index (.packwizignore), so refresh cannot see these
#
# packwiz has no author-side check of its own — upstream's cmd/update.go still
# carries `// TODO: --check flag?` — so this is the substitute.
#
# Usage: scripts/pack-check.sh [--fix]
#   (default)  fail on drift, leaving the manifest as git has it
#   --fix      keep the refreshed manifest, for when the drift is intended

set -uo pipefail

# The packwiz commit this pack is pinned to (ADR-0024). packwiz publishes no
# releases and no tags, so a SHA is the only way to say which one built the
# manifest. This assignment is the single source of truth for that pin — the
# workflow doc quotes it but does not define it. Install with:
#   go install github.com/packwiz/packwiz@$PACKWIZ_SHA
PACKWIZ_SHA="dfd8b68a4796c763e25bad50265ea1f1233e24f1"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT" || exit 1

FIX=0
case "${1:-}" in
    "")      ;;
    --fix)   FIX=1 ;;
    *)       echo "unknown argument: $1" >&2
             echo "Usage: scripts/pack-check.sh [--fix]" >&2
             exit 2 ;;
esac

PACKWIZ="$(command -v packwiz || echo "$HOME/go/bin/packwiz")"
if [[ ! -x "$PACKWIZ" ]]; then
    echo "packwiz not found. Install the pinned build with:" >&2
    echo "  go install github.com/packwiz/packwiz@$PACKWIZ_SHA" >&2
    exit 2
fi

# The metafiles are tracked, so `mods/` exists in any checkout — including a git
# worktree, which has the manifest but none of the jars it describes. What marks
# an uncheckable checkout is therefore the absence of *jars*, not of the folder.
if ! compgen -G 'mods/*.jar' >/dev/null; then
    echo "no jars in mods/ — nothing to check against." >&2
    echo "This checkout has the manifest but not the pack (a git worktree looks" >&2
    echo "like this). Run it from the pack instance." >&2
    exit 2
fi

TRACKED=(index.toml pack.toml)

# Metafiles this run creates must be distinguishable from ones the user wrote and
# has not committed yet — `packwiz cf install <slug>` followed by this check must
# not lose the new metafile. Snapshot the untracked set before refreshing.
BEFORE="$(git ls-files --others --exclude-standard -z -- 'mods/*.pw.toml' | tr '\0' '\n' | sort)"

echo "Refreshing manifest with packwiz $PACKWIZ_SHA ..."
if ! "$PACKWIZ" refresh; then
    echo "packwiz refresh failed." >&2
    exit 2
fi

# Drift = refresh rewrote something tracked, or produced a metafile git has never
# seen. Diff against HEAD, not the index: drift that has already been `git add`ed
# is still drift, and a plain `git diff` would call it clean.
has_drift() {
    git diff --quiet HEAD -- "${TRACKED[@]}" mods/ && \
        [[ -z "$(git ls-files --others --exclude-standard -- mods/)" ]] && return 1
    return 0
}

# `packwiz refresh` hashes the metafiles, not the jars they name, and the jars are
# excluded from the index anyway (see .packwizignore), so the manifest and the
# installed jars have to be compared here or not at all. Both directions matter:
# a metafile with no jar, and a jar nothing accounts for.
JARCHECK="$(python3 - <<'PY'
import glob, os, re
named = {}
for meta in sorted(glob.glob("mods/*.pw.toml")):
    m = re.search(r'^filename\s*=\s*"(.+)"', open(meta).read(), re.M)
    if m:
        named[m.group(1)] = os.path.basename(meta)

missing = [f"{v} -> {k}" for k, v in sorted(named.items())
           if not os.path.exists(os.path.join("mods", k))]

# A jar is accounted for if a metafile names it, if the index hashes it directly
# (the two forks), or if it is the first-party mod, which is deliberately not
# indexed at all.
index = open("index.toml").read() if os.path.exists("index.toml") else ""
stray = [os.path.basename(j) for j in sorted(glob.glob("mods/*.jar"))
         if os.path.basename(j) not in named
         and os.path.basename(j) not in index
         and not os.path.basename(j).startswith("planetaryfactory_core-")]

for m in missing:
    print("MISSING\t" + m)
for s in stray:
    print("STRAY\t" + s)
PY
)"
MISSING="$(printf '%s\n' "$JARCHECK" | sed -n 's/^MISSING\t//p')"
STRAY="$(printf '%s\n' "$JARCHECK" | sed -n 's/^STRAY\t//p')"

# Undo only what this run wrote: restore the tracked files, and delete just those
# metafiles that did not exist before the refresh. Anything the user had already
# written is left alone.
restore() {
    if ! git checkout -- "${TRACKED[@]}" mods/; then
        echo >&2
        echo "WARNING: could not restore the manifest. The refresh's changes are" >&2
        echo "still in the working tree — inspect with 'git status' before doing" >&2
        echo "anything else." >&2
        return 1
    fi
    local after
    after="$(git ls-files --others --exclude-standard -z -- 'mods/*.pw.toml' | tr '\0' '\n' | sort)"
    comm -13 <(printf '%s\n' "$BEFORE") <(printf '%s\n' "$after") | while IFS= read -r f; do
        [[ -n "$f" ]] && rm -f "$f"
    done
    return 0
}

if [[ -n "$MISSING" ]]; then
    COUNT="$(printf '%s\n' "$MISSING" | wc -l | tr -d ' ')"
    echo >&2
    echo "MISSING — $COUNT metafile(s) name a jar that is not installed:" >&2
    printf '%s\n' "$MISSING" | head -20 >&2
    [[ "$COUNT" -gt 20 ]] && echo "  ... and $((COUNT - 20)) more" >&2
    echo >&2
    echo "Either install the jars the manifest names, or bump the manifest to" >&2
    echo "match what is installed." >&2
fi

if [[ -n "$STRAY" ]]; then
    echo >&2
    echo "STRAY — jar(s) installed that the manifest does not account for:" >&2
    printf '%s\n' "$STRAY" | sed 's/^/  /' >&2
    echo >&2
    echo "Add each to the manifest with 'packwiz cf install <slug>'. A locally" >&2
    echo "built jar needs a '!mods/<name>' negation in .packwizignore first —" >&2
    echo "managed jars are excluded there, so refresh will not pick it up on its" >&2
    echo "own. Otherwise, delete it." >&2
fi

if has_drift; then
    echo >&2
    echo "DRIFT — the pack content on disk does not match the tracked manifest:" >&2
    git status --short -- "${TRACKED[@]}" mods/ >&2
    echo >&2
    echo "The index covers config/, kubejs/, data/ and packs/ as well as mods/," >&2
    echo "so an unrefreshed edit to any of those shows up here too." >&2
    echo "If the change is intended, re-run with --fix and commit the result." >&2
fi

if [[ -z "$MISSING" && -z "$STRAY" ]] && ! has_drift; then
    echo "OK — the pack content matches the manifest."
    exit 0
fi

# --fix can rewrite the manifest, but it cannot conjure a jar that is not there,
# and it will not silently adopt a jar nobody declared — so both fail even here.
if [[ "$FIX" -eq 1 && -z "$MISSING" && -z "$STRAY" ]]; then
    echo
    echo "Manifest updated to match what is installed. Review and commit:"
    git status --short -- "${TRACKED[@]}" mods/
    exit 0
fi

restore
exit 1

#!/usr/bin/env bash
# Assert the installed jars still match the tracked packwiz manifest (ADR-0024).
#
# `mods/` is gitignored, so nothing in git notices a jar being added, removed or
# swapped. The manifest does: `packwiz refresh` rewrites index.toml from what is
# actually on disk, so if refreshing changes a tracked file, the pack has drifted
# from what git records.
#
# packwiz has no author-side check of its own — upstream's cmd/update.go still
# carries `// TODO: --check flag?` — so this refresh-and-diff pairing is the
# substitute.
#
# Usage: scripts/pack-check.sh [--fix]
#   (default)  fail on drift, leaving the manifest as git has it
#   --fix      keep the refreshed manifest, for when the drift is intended

set -uo pipefail

# The packwiz commit this pack is pinned to (ADR-0024). packwiz publishes no
# releases and no tags, so a SHA is the only way to say which one built the
# manifest. Install with:
#   go install github.com/packwiz/packwiz@$PACKWIZ_SHA
PACKWIZ_SHA="dfd8b68a4796c763e25bad50265ea1f1233e24f1"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT" || exit 1

FIX=0
[[ "${1:-}" == "--fix" ]] && FIX=1

PACKWIZ="$(command -v packwiz || echo "$HOME/go/bin/packwiz")"
if [[ ! -x "$PACKWIZ" ]]; then
    echo "packwiz not found. Install the pinned build with:" >&2
    echo "  go install github.com/packwiz/packwiz@$PACKWIZ_SHA" >&2
    exit 2
fi

# The manifest describes mods/, so a missing mods/ is not "no drift" — it is a
# checkout that cannot be checked. Worktrees hit this: mods/ is gitignored and
# never copied into them.
if [[ ! -d mods ]]; then
    echo "no mods/ directory — nothing to check against." >&2
    echo "This checkout has no installed jars; run from the pack instance." >&2
    exit 2
fi

TRACKED=(index.toml pack.toml)

echo "Refreshing manifest with packwiz $PACKWIZ_SHA ..."
if ! "$PACKWIZ" refresh; then
    echo "packwiz refresh failed." >&2
    exit 2
fi

# Metafiles are tracked but untracked-new ones (a jar added by hand, then
# refreshed) show up as untracked files rather than as a diff, so check both.
# Diff against HEAD, not the index: a drift that has already been `git add`ed is
# still drift, and a plain `git diff` would call it clean.
DRIFT=0
git diff --quiet HEAD -- "${TRACKED[@]}" mods/ || DRIFT=1
if [[ -n "$(git ls-files --others --exclude-standard -- mods/)" ]]; then
    DRIFT=1
fi

if [[ "$DRIFT" -eq 0 ]]; then
    echo "OK — installed jars match the manifest."
    exit 0
fi

if [[ "$FIX" -eq 1 ]]; then
    echo "Manifest updated to match the installed jars. Review and commit:"
    git status --short -- "${TRACKED[@]}" mods/
    exit 0
fi

echo >&2
echo "DRIFT — the installed jars do not match the tracked manifest:" >&2
git status --short -- "${TRACKED[@]}" mods/ >&2
echo >&2
echo "If the change is intended, re-run with --fix and commit the result." >&2
echo "If it is not, restore the jars the manifest names." >&2

# Leave the tree as git has it, so a failing check is not also a mutation. That
# means restoring the tracked files AND removing metafiles this refresh just
# created — scoped to *.pw.toml so nothing else in mods/ is ever touched.
git checkout -- "${TRACKED[@]}" mods/ 2>/dev/null
git ls-files --others --exclude-standard -- 'mods/*.pw.toml' | while read -r f; do
    rm -f "$f"
done
exit 1

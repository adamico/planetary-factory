#!/usr/bin/env bash
# Scan the most recent launch for mod-loading failures.
#
# Mod loading failures surface far away from their cause: a mod that fails to
# construct puts NeoForge into a broken state, mod resource packs never register,
# and the crash you actually see comes from whichever unrelated mod first trips
# over the missing assets. This greps for the real signal instead.
#
# Usage: scripts/check-launch.sh [logfile]   (default: logs/latest.log)

set -uo pipefail

cd "$(dirname "$0")/.."
LOG="${1:-logs/latest.log}"

if [[ ! -f "$LOG" ]]; then
  echo "no log at $LOG — has the pack been launched?" >&2
  exit 2
fi

fail=0

# The cause: a mod that could not be constructed.
if grep -q "Failed to create mod instance" "$LOG"; then
  fail=1
  echo "MOD CONSTRUCTION FAILED"
  grep -n "Failed to create mod instance" "$LOG" | while IFS= read -r line; do
    echo "  ${line%%$'\n'*}" | cut -c1-200
  done
  # The first stack frame after the error names the code that actually threw.
  echo
  echo "  first frame after each failure:"
  grep -A2 "Failed to create mod instance" "$LOG" \
    | grep -oE '^\s+at [A-Za-z]+/[^ ]+' | sed 's/^/    /' | head -5
fi

# The consequence: everything downstream refusing to run.
broken=$(grep -c "broken mod state" "$LOG")
if [[ "$broken" -gt 0 ]]; then
  fail=1
  echo
  echo "BROKEN MOD STATE: $broken events refused"
fi

# Mod resources missing from the reload is the tell that assets never registered,
# which is what turns a load failure into a confusing crash somewhere else.
if grep -q "Reloading ResourceManager" "$LOG" \
   && ! grep "Reloading ResourceManager" "$LOG" | grep -q "mod_resources"; then
  fail=1
  echo
  echo "MOD RESOURCES ABSENT from the resource reload"
  grep "Reloading ResourceManager" "$LOG" | tail -1 | cut -c1-200 | sed 's/^/  /'
fi

# A mod compiled against a class that has moved throws at the call site, not at
# construction, so everything above stays quiet. Minecraft then *recovers* -- it
# discards the resource packs and carries on into a playable world -- which is how a
# KubeJS bump once passed both this script and the worldgen check while GTCEu's
# recipe hook was dead (ADR-0023). A recovered error is still a failure.
# Match only a thrown stack trace, anchored at the start of the line. Mixin logs
# `Error loading class: ... (ClassNotFoundException)` at WARN for every optional mod
# that is not installed, which is normal and must not fail the run.
if grep -qE "^java\.lang\.NoClassDefFoundError" "$LOG"; then
  fail=1
  echo
  echo "MISSING CLASS AT RUNTIME — a mod references a class that is not there"
  grep -nE "^java\.lang\.NoClassDefFoundError" "$LOG" | head -3 | cut -c1-200 | sed 's/^/  /'
  # The first stack frame naming a mod jar is the mod holding the stale reference.
  grep -A3 -E "^java\.lang\.NoClassDefFoundError" "$LOG" \
    | grep -m1 -oE "at TRANSFORMER/[^ ]+" | sed 's/^/  culprit: /'
fi

if grep -q "Caught error loading resourcepacks" "$LOG"; then
  fail=1
  echo
  echo "RESOURCE PACKS DISCARDED — the game recovered from an error by dropping state"
fi

if [[ "$fail" -eq 0 ]]; then
  echo "launch clean — no mod construction failures in $LOG"
  exit 0
fi

echo
echo "^ fix the construction failure first; later crashes are probably symptoms."
exit 1

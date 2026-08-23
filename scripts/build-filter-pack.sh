#!/usr/bin/env bash
# Rebuild kubejs/data/<name>.zip from packs/<name>/, for every pack under packs/
# (or just the ones named as arguments).
#
# The zips exist for one reason: a pack.mcmeta "filter" section, which blocks
# matching paths in every pack below it in the list. KubeJS serves kubejs/data
# three ways and only one of them reads a real pack.mcmeta — a .zip, which
# KubeFileResourcePack.scanAndLoad wraps in a vanilla FilePackResources. The
# loose folder and the generated-data pack both synthesise their metadata.
#
# Everything a pack *adds* stays loose JSON under kubejs/data/<namespace>/.

set -euo pipefail
cd "$(dirname "$0")/.."

packs=("$@")
if [ ${#packs[@]} -eq 0 ]; then
  for dir in packs/*/; do
    packs+=("$(basename "$dir")")
  done
fi

for name in "${packs[@]}"; do
  SRC="packs/$name"
  OUT="kubejs/data/$name.zip"

  python3 -c "import json,sys;json.load(open('$SRC/pack.mcmeta'))"

  rm -f "$OUT"
  (cd "$SRC" && zip -qrX "$OLDPWD/$OUT" .)
  echo "built $OUT"
  unzip -l "$OUT"
done

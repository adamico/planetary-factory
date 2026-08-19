#!/usr/bin/env bash
# Rebuild kubejs/data/remove-stock-bodies.zip from packs/remove-stock-bodies/.
#
# The zip exists for one reason: a pack.mcmeta "filter" section, which blocks
# matching paths in every pack below it in the list. KubeJS serves kubejs/data
# three ways and only one of them reads a real pack.mcmeta — a .zip, which
# KubeFileResourcePack.scanAndLoad wraps in a vanilla FilePackResources. The
# loose folder and the generated-data pack both synthesise their metadata.
#
# Everything the pack *adds* stays loose JSON under kubejs/data/<namespace>/.

set -euo pipefail
cd "$(dirname "$0")/.."

SRC="packs/remove-stock-bodies"
OUT="kubejs/data/remove-stock-bodies.zip"

python3 -c "import json,sys;json.load(open('$SRC/pack.mcmeta'))"

rm -f "$OUT"
(cd "$SRC" && zip -qrX "$OLDPWD/$OUT" .)
echo "built $OUT"
unzip -l "$OUT"

#!/usr/bin/env bash
# Assert every ticket an ADR declares superseded carries a comment naming that ADR.
#
# An ADR that contradicts a closed ticket's stated answer declares it in frontmatter:
#
#     supersedes: [55, 62]
#
# The ticket learns by a comment containing the literal `ADR-00NN`. Without it the ticket
# keeps asserting an answer the ADR has overridden, and the next session to read it is
# misled by a document that looks settled (#132).
#
# Network-touching: needs an authenticated `gh`. Run it after committing an ADR that
# declares a `supersedes:` key — it is not part of any offline check.
set -uo pipefail

cd "$(dirname "$0")/.."

fail=0
checked=0

for adr in docs/adr/[0-9][0-9][0-9][0-9]-*.md; do
  base=$(basename "$adr")
  num=${base%%-*}
  tag="ADR-$num"

  # `supersedes: [55, 62]` inside the frontmatter block only.
  line=$(awk 'NR==1 && $0!="---"{exit} NR>1 && $0=="---"{exit} /^supersedes:/{print; exit}' "$adr")
  [ -n "$line" ] || continue

  tickets=$(printf '%s\n' "$line" | sed 's/^supersedes:[[:space:]]*//; s/[][,]/ /g')

  for t in $tickets; do
    case "$t" in
      ''|*[!0-9]*) echo "MALFORMED $tag: supersedes entry '$t' is not a ticket number"; fail=1; continue ;;
    esac

    checked=$((checked + 1))

    if ! body=$(gh issue view "$t" --json comments --jq '.comments[].body' 2>/dev/null); then
      echo "UNREADABLE $tag: cannot read #$t"
      fail=1
      continue
    fi

    if printf '%s' "$body" | grep -q "$tag"; then
      echo "ok   $tag supersedes #$t"
    else
      echo "MISSING $tag: #$t carries no comment naming $tag"
      echo "        gh issue comment $t --body '## Superseded by $tag ...'"
      fail=1
    fi
  done
done

if [ "$checked" -eq 0 ]; then
  echo "no ADR declares a supersedes: key"
fi

exit "$fail"

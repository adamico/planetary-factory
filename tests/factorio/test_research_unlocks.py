#!/usr/bin/env python3
"""Assert every Researchd `unlocks` id names a recipe the pack actually emits.

This is #97's second half -- the "cross-file references resolve" check in
`docs/testing/what-to-check.md`'s terms. The first half already lives in
`tests/factorio/test_recipe_sweep.py`: nothing the pack emits, and nothing the allowlist admits,
is a vanilla grid recipe.

The hazard is that Researchd gates by recipe *id*, and a recipe's id is derived from its type, so
changing a recipe's surface changes its id and leaves the research holding a lock on an id nothing
emits. There is no error and no log line -- the player receives a research that unlocks nothing.
That is why it is asserted here rather than left to a world load.

  - every `unlocks` entry in `researchd.js` resolves to a recipe under
    `kubejs/data/planetaryfactory/recipe/`
  - a declaration this parser cannot read is a failure, not a skip. A silently unparsed `unlocks`
    is exactly the unchecked coupling the check exists to catch

The check passes on an empty set: `researchd.js` currently declares no `unlocks` at all, because
the research tree is provisional and due to be harvested. It is landed ahead of #138 -- the first
ticket to write unlock ids in volume -- so the guard is in place before there is anything to guard.

An unlock id in a foreign namespace fails too. A surviving stock recipe (ADR-0034's allowlist) is
third-party and cannot be resolved statically, so gating research on one is a decision that has to
be recorded rather than a case this check quietly waves through.

WHAT IT CANNOT PROVE is that Researchd accepted the lock in a running game -- that the recipe
manager holds the id at the moment the research is registered. That is #138's world load: 0 failed
recipes and 0 unresolved research names.

Usage: tests/factorio/test_research_unlocks.py
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
SCRIPTS = ROOT / "kubejs/server_scripts"
EMITTED = ROOT / "kubejs/data/planetaryfactory/recipe"
EMITTED_NAMESPACE = "planetaryfactory"

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def emitted_ids():
    """The recipe ids the converter writes, keyed the way a datapack keys them.

    `kubejs/data/<namespace>/recipe/<path>.json` is loaded as `<namespace>:<path>`, so the id is
    read off the file's location rather than out of its contents -- the JSON does not carry it.
    """
    ids = set()
    for path in sorted(EMITTED.rglob("*.json")):
        ids.add("%s:%s" % (EMITTED_NAMESPACE, path.relative_to(EMITTED).with_suffix("").as_posix()))
    return ids


def balanced(text, start, opener, closer):
    """The index just past the `closer` matching the `opener` at `start`, or None.

    Deliberately naive about strings and comments: a declaration it misreads becomes an
    unparsed call, and an unparsed call is a failure below rather than a skip.
    """
    depth = 0
    for index in range(start, len(text)):
        if text[index] == opener:
            depth += 1
        elif text[index] == closer:
            depth -= 1
            if depth == 0:
                return index + 1
    return None


def strip_comments(text):
    """The script with its comments removed, so prose about `fromFactorio()` is not a declaration.

    The header of `researchd.js` explains the DSL by name, and a check that counted those would
    report a call it cannot read on a file that declares nothing at all.
    """
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def declarations():
    """Every `fromFactorio(name, {...})` call in researchd.js, as (name, unlocks) pairs.

    Parsed rather than imported: the file is a KubeJS script whose only reader is Rhino, and its
    `fromFactorio` is defined in another script that only exists inside a running game.
    """
    text = strip_comments((SCRIPTS / "researchd.js").read_text())
    calls = []
    for opening in re.finditer(r"\bfromFactorio\s*\(", text):
        end = balanced(text, opening.end() - 1, "(", ")")
        if end is None:
            continue
        body = text[opening.end():end - 1]
        found = re.match(r"\s*(['\"])(.*?)\1", body)
        if not found:
            continue
        calls.append((found.group(2), body[found.end():]))

    total = len(re.findall(r"\bfromFactorio\s*\(", text))
    check(len(calls) == total,
          "researchd.js makes %d fromFactorio() calls and %d are readable -- a call must open "
          "with a quoted technology name" % (total, len(calls)))

    declared = []
    for name, rest in calls:
        # `unlocks:` and not `unlocksDimensions:` -- a dimension is not a recipe id.
        found = re.search(r"\bunlocks\s*:\s*\[", rest)
        if not found:
            declared.append((name, []))
            continue
        end = balanced(rest, found.end() - 1, "[", "]")
        check(end is not None,
              "`%s` declares an `unlocks` array this check cannot read to its close" % name)
        if end is None:
            continue
        inner = rest[found.end():end - 1]
        ids = [match.group(2) for match in re.finditer(r"(['\"])(.*?)\1", inner)]
        # A non-empty array whose entries are not plain string literals is unreadable, and an
        # unreadable unlock is an unchecked one.
        check(bool(ids) or not inner.strip(),
              "`%s` declares an `unlocks` array whose entries are not string literals -- this "
              "check reads recipe ids, and an entry it cannot read is an unchecked lock" % name)
        declared.append((name, ids))
    return declared


def main():
    recipes = emitted_ids()
    check(bool(recipes), "no emitted recipes found -- has the converter been run?")

    declared = declarations()
    locks = 0
    for name, ids in declared:
        for recipe_id in ids:
            locks += 1
            check(recipe_id in recipes,
                  "research `%s` unlocks `%s`, which the pack does not emit -- Researchd gates by "
                  "recipe id and a lock on a missing id fails silently, unlocking nothing (#97)"
                  % (name, recipe_id))

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok: %d research declaration(s), %d unlock(s), %d emitted recipe(s)"
          % (len(declared), locks, len(recipes)))
    return 0


if __name__ == "__main__":
    sys.exit(main())

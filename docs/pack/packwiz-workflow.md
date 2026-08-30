# The pack manifest

The jar set is tracked as a [packwiz](https://packwiz.infra.link/) manifest. The reasoning — why
packwiz, why the local jars are handled the way they are, what was deferred — is
[ADR-0024](../adr/0024-the-jar-set-is-a-packwiz-manifest.md); the tool evaluation behind it is
[the research note](../research/modpack-track-update-publish-workflows.md). This document is the
how.

## Installing packwiz

packwiz publishes **no releases and no tags**; distribution is CI artifacts off `main`. The pack
therefore pins a commit, so that "which packwiz built this manifest" has an answer:

```sh
go install github.com/packwiz/packwiz@dfd8b68a4796c763e25bad50265ea1f1233e24f1
```

The binary lands in `~/go/bin/packwiz`. **`PACKWIZ_SHA` in `scripts/pack-check.sh` is the source of
truth for that pin** — the SHA above is quoted from it. Change it there, and the check reports the
new value in its own output.

`packwiz` has no `--version` flag, which is a fair summary of the situation.

## What the manifest is

| File | What it holds |
| --- | --- |
| `pack.toml` | pack name, version, Minecraft and NeoForge versions, and the index's hash |
| `index.toml` | every indexed file with its sha256 |
| `mods/*.pw.toml` | one metafile per externally-sourced mod: its CurseForge project and file id |

All three are tracked in git. The jars are not — `.gitignore` keeps `mods/*` out and re-includes
only `!mods/*.pw.toml`. **Do not rewrite that as a bare `mods`**: git will not re-include a file
beneath an excluded *directory*, so the negation silently stops working and the manifest quietly
falls out of version control. That is the exact bug this whole setup exists to fix.

`.packwizignore` limits what gets indexed to pack content — `mods/`, `config/`, `kubejs/`,
`defaultconfigs/`, `data/`, `packs/`. Docs, tests, scripts and `mod/` are excluded so that editing
prose does not dirty the manifest. Per-user state inside `config/` (`config/jei/`, `config/spark/`,
the `*client*.toml` files) is excluded too — it is not pack behaviour.

**The jars themselves are excluded, and this is the subtle part.** packwiz assumes `mods/` holds
metafiles and that an installer fetches the jars. Here the pack root *is* the playable instance, so
the jars sit right next to their metafiles — and `refresh` would happily index each managed mod
twice, once as its metafile and once as a raw hashed jar (465 index entries become 586). So
`.packwizignore` carries `mods/*.jar`, with negations re-including only the two forks, whose hash is
the sole record of which build is installed:

```
mods/*.jar
!mods/gcyr-*.jar
!mods/Respoiled-*.jar
```

A consequence worth knowing: because `refresh` cannot see the managed jars, it cannot notice one
going missing or a new one appearing either. That is what the MISSING and STRAY checks below exist
for.

## The three local jars

Three jars are built rather than downloaded, and none exists on a public index.

| Jar | How it is tracked |
| --- | --- |
| `gcyr` fork | unmanaged entry in `index.toml` — path plus sha256, no metafile |
| `Respoiled` fork | unmanaged entry in `index.toml` — path plus sha256, no metafile |
| `planetaryfactory_core` | **not indexed at all** |

`planetaryfactory_core` is excluded in `.packwizignore`. It is rebuilt into `mods/` by
`installToPack` on every `./gradlew build`, Gradle jars are not byte-reproducible, and its source is
fully tracked in `mod/` — so indexing it would dirty the manifest on every build while recording
nothing git does not already have.

`packwiz update --all` prints this for the two unmanaged jars:

```
A supported update system for "gcyr-1.21.1-0.2.4+gt7.0.2-src.jar" cannot be found.
```

**That is expected, non-fatal and non-mutating.** It is not a defect to fix. Publishing the forks as
GitHub release assets would make them ordinary updatable mods, and that is deliberately deferred —
see ADR-0024 and the publish ticket.

## Checking for drift

```sh
scripts/pack-check.sh          # fail if installed jars differ from the manifest
scripts/pack-check.sh --fix    # keep the refreshed manifest, when the drift is intended
scripts/pack-check.sh --prune  # drop the metafiles whose jar is gone, then --fix
```

It checks three things, because none alone is enough:

1. **`packwiz refresh` changed something tracked** — an edit to indexed pack content, or to one of
   the two unmanaged fork jars.
2. **MISSING** — a metafile names a jar that is not installed. `refresh` hashes the *metafiles*, not
   the jars they point at, so a metafile bumped to a version nobody downloaded refreshes perfectly
   clean.
3. **STRAY** — a jar is installed that nothing accounts for. The managed jars are excluded from the
   index (see below), so `refresh` cannot see these at all.

A failing run restores the tree, so a failed check never leaves the manifest half-updated — and it
deletes only metafiles that *this run* created, never one you wrote and have not committed yet.
`--fix` rewrites the manifest but will neither conjure a missing jar nor silently adopt a stray one,
so MISSING and STRAY fail even under `--fix`.

**What it still does not check:** that an installed jar's *contents* match the CurseForge file id its
metafile names. Filenames are compared, not hashes. Swapping a jar's bytes while keeping its name
would pass. Closing that needs packwiz-installer, which is not set up here.

Run it after touching `mods/`. Nothing runs it automatically yet — there is no CI in this repo —
which is its own ticket.

**It needs the jars, not just the manifest.** The metafiles are tracked, so `mods/` exists in every
checkout — but a git worktree has the manifest and none of the jars it describes. The check detects
that by the absence of jars and exits 2, rather than reporting 121 false failures.

## Removing a mod, and the CurseForge client

The jars are managed in the CurseForge client, which knows nothing about the manifest. packwiz's own
CurseForge integration does not close that loop: `cf import` and `cf detect` are one-way and
**additive** — they only ever add — and `cf detect` additionally *deletes* every jar it fingerprints
(see ADR-0024), so neither is a sync. There is no two-way sync command, in packwiz or anywhere else.

Removal is therefore the manifest's own business, and `--prune` is it:

```sh
scripts/pack-check.sh --prune
```

It is the MISSING check wired to `packwiz remove`: every metafile naming a jar that is not installed
is dropped, then the run continues as an ordinary `--fix`. Review and commit the deleted metafiles
along with the changed `index.toml` and `pack.toml`.

Only prune when the jars really are gone on purpose. A jar missing because a download failed, or
because you are in a worktree, is the same MISSING signal — `--prune` cannot tell the two apart, and
will happily delete the manifest entry for a mod you meant to keep. Plain `scripts/pack-check.sh`
first, if you are not sure which you are looking at.

Removing a mod leaves its `config/` behind; that is not the manifest's problem, but it is worth a
sweep now and then.

Adding still goes the other way by hand — see below. A mod added in the CurseForge client shows up
as STRAY, and `packwiz cf install <slug>` writes the metafile the jar needs.

## Adding a mod

```sh
packwiz cf install <slug>       # from CurseForge
packwiz mr install <slug>       # from Modrinth
```

Then commit the new `mods/<slug>.pw.toml` along with the changed `index.toml` and `pack.toml`.

For a jar with no index entry — a new fork, a hand-built jar — add a `!mods/<name>` negation to
`.packwizignore` first, then run `scripts/pack-check.sh --fix`. Without the negation the jar is
excluded along with every other managed jar and `refresh` will not index it; the check reports it as
STRAY until you decide which it is.

## Bumping a mod, and rolling it back

A bump touches three tracked things: the mod's metafile, its line in `index.toml`, and the index
hash in `pack.toml`.

```sh
packwiz update <slug>
scripts/pack-check.sh --fix     # only if the jar on disk changed too
git add -u && git commit -m "chore: bump <slug> to <version>"
```

Rolling back is then an ordinary git operation:

```sh
git revert <commit>
```

This is the point of the whole exercise. The previous rollback left an untracked `mods/.replaced/`
directory and no record of *why*, and the reason had to be re-derived experimentally months later
(ADR-0023). A reverted commit carries the version, the date and the message.

`packwiz update` is interactive by default; every packwiz command takes a global `-y` to accept
prompts non-interactively.

## Reinstalling the jars

The manifest names what should be installed but does not install it. That is
[packwiz-installer](https://github.com/packwiz/packwiz-installer)'s job, and setting it up for this
pack has not been done — the jars in `mods/` are currently maintained by hand and the manifest
records them.

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

The binary lands in `~/go/bin/packwiz`. The same SHA is recorded in `scripts/pack-check.sh` as
`PACKWIZ_SHA`; if you bump it, bump it in both places and say so in the commit.

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
prose does not dirty the manifest.

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
```

It runs `packwiz refresh` and fails if that changed anything tracked. A failing run restores the
tree, so a failed check never leaves the manifest half-updated.

Run it after touching `mods/`. Nothing runs it automatically yet — there is no CI in this repo —
which is its own ticket.

**It needs a real `mods/`.** A git worktree does not have one (the jars are gitignored and never
copied in), so the check exits 2 there rather than reporting a false pass.

## Adding a mod

```sh
packwiz cf install <slug>       # from CurseForge
packwiz mr install <slug>       # from Modrinth
```

Then commit the new `mods/<slug>.pw.toml` along with the changed `index.toml` and `pack.toml`.

For a jar with no index entry — a new fork, a hand-built jar — drop it in `mods/` and run
`scripts/pack-check.sh --fix`. `packwiz refresh` picks it up as an unmanaged hashed entry.

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

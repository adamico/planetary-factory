---
status: accepted
---

# The jar set is a packwiz manifest

`mods/` is gitignored wholesale (`.gitignore:27`). The pack's 124 jars — the thing that *is* the
modpack — have had no representation in version control at all, and the cost has already been paid
twice. The KubeJS bump was attempted and rolled back with no record of the failure mode, leaving
only an untracked `mods/.replaced/` directory; the constraint that came out of it survived as an
agent memory rather than as a fact anyone could check, and ADR-0023 had to re-run the experiment to
recover a reason that was known once and lost. `docs/pack/mods-snapshot.md` was a stopgap: a flat
`ls`, manual, carrying no source or version metadata, stale the day after it was written.

This ADR records what replaces it. The jar set becomes a **packwiz** manifest, tracked in git.

## Why packwiz and not the alternatives

The evaluation is in [the research note](../research/modpack-track-update-publish-workflows.md).
The pack has a hard requirement that decided it: three jars are built locally and exist on no public
index — `planetaryfactory_core` (built from `mod/`, ADR-0014), a `gcyr` fork, and a `Respoiled`
fork. Any tool that handles those as a hand-maintained exception list reproduces the current
problem in a new file.

Only packwiz expresses them as first-class entries. `packwiz refresh` walks the pack root and
hashes every non-ignored file into `index.toml` with `metafile = false`, so a locally built jar is
an ordinary index entry with no exception list anywhere. The alternatives fail structurally, not
fixably: CurseForge's `manifest.json` has no field capable of holding a local jar — `files[]` is
`(projectID, fileID)` and nothing else — ferium's `user/` folder is the manual exception by its own
README and moves foreign files aside, Prism carries no mod list, and Cat-Downloader-Legacy cannot
author a manifest and has had no release since February 2024.

**The prior workflow is not a candidate.** `mechanical-mastery-plus` ran NillerMedDild's
ModpackUploader with Cat-Downloader-Legacy, and its dead remnants are why this repo's `.gitignore`
still names `automation/`, `secrets.ps1` and `Cat-Downloader-Legacy.jar` — none of which exist here.
That stack generates `manifest.json` fresh at release time from `minecraftinstance.json` and deletes
it again. It is a *publish* tool; it pins nothing between releases, and its one concession to local
jars is `$FILES_TO_INCLUDE_IN_MODS_FOLDER_IN_CLIENT_FILES`, a hardcoded array of filenames. Its
input, `minecraftinstance.json`, is a file this repo deliberately gitignores.

## What is decided

**Local jars are unmanaged hashed entries, not GitHub releases.** packwiz's `[update.github]`
updater would make the forks ordinary updatable mods, and it is the right answer *when publishing*,
because `github.com` is on Modrinth's allowlist and such an entry exports as a real hashed `files[]`
row rather than a bundled binary. Publishing is out of scope (below), so that benefit is currently
worth nothing, while the cost — cutting a GitHub release for every rebuild of every fork — is
recurring and immediate. For `planetaryfactory_core` it would also contradict ADR-0014 directly:
`installToPack` exists so that `./gradlew build` is the whole install step, and routing the jar
through a release breaks that. Route A costs nothing and records the exact hash.

Revisit per jar when publish lands. `gcyr` will be the cheap one — it already has `auto_publish.yml`.

**`planetaryfactory_core` is excluded from the index entirely.** Its provenance is `mod/`, which is
fully tracked; hashing the build output records nothing git does not already have. Gradle jars are
not byte-reproducible by default, so indexing it would dirty `index.toml` on every `./gradlew build`
and train everyone to ignore the drift check. This slightly weakens the "the manifest reconstructs
the pack" property, and the trade is deliberate: a check that cries wolf is worth less than the
property it protects. Two genuinely external jars remain unmanaged entries.

**packwiz itself is pinned by commit SHA.** It has zero releases and zero tags; distribution is CI
artifacts off `main`. Installing it as `go install github.com/packwiz/packwiz@<sha>` with the SHA
recorded makes the manifest reproducible. Leaving the tool that pins every mod itself unpinned would
re-create, one level up, the exact problem this ADR closes. This follows ADR-0001's precedent of
building a source-only dependency at a known point rather than trusting a floating binary.

**The pack root is the repo root**, with a `.packwizignore` limiting the index to pack content —
`mods/`, `config/`, `kubejs/`, `defaultconfigs/`, `data/`, `packs/` — and excluding `docs/`,
`tests/`, `scripts/`, `mod/` and the Gradle files. Indexing those would give files git already
tracks a second, hash-based source of truth and dirty the manifest on every documentation edit.

**Metafiles stay in `mods/`, and `.gitignore` gains a negation for them.** packwiz writes one
`.pw.toml` per mod into `mods/`, which is precisely the directory ignored today — so the manifest
would be untracked by default, silently reproducing the bug. The rule is rewritten as `mods/*` plus
`!mods/*.pw.toml`; git will not re-include a file beneath an excluded *directory*, so the bare
`mods` form cannot be negated and must go.

**Bootstrapping runs against a copy.** `packwiz cf detect` fingerprints all 124 jars against the
CurseForge API and **deletes** each one it matches, on the assumption that an installer re-downloads
it. Run against the live `mods/`, that empties the playable instance and permanently couples "launch
the game" to "have packwiz working" — against a tool with no releases. The three local jars fall out
of the sieve unmatched and `refresh` sweeps up the two that are indexed.

## What is deferred, and why it is written down

**Publish is a separate ticket.** The issue asked for track, update and publish; only track is
load-bearing today. The pack is pre-release and nothing consumes a distributable, and publish
carries the only external gate in the design — CurseForge's Approved Non-CurseForge Mods process,
which the `overrides/` fallback for local jars would trigger. Licensing is *not* the obstacle:
`gcyr` is LGPL-3.0 and `Respoiled` is MIT, both redistributable. When it returns, `packwiz cf export`
replaces `New-ManifestJson`, and ModpackUploader's changelog, client-config stripping and upload
logic are prior art worth mining.

**CI is a separate ticket.** The drift check is `packwiz refresh && git diff --exit-code` in a
checked-in script, run on demand exactly as `worldgen-check.py` is. Nothing in this repo runs
automatically — there is no `.github/workflows` — and standing up the first workflow is a larger
decision than this change should smuggle in. packwiz has no author-side diff of its own;
`cmd/update.go` still carries `// TODO: --check flag?`.

Both are filed as issues rather than left as understanding. An unrecorded deferral is the same
failure this ADR exists to fix.

## Consequences

- `docs/pack/mods-snapshot.md` is deleted; the manifest supersedes it and git history keeps it.
- Rolling a mod back becomes `git revert` over three text files — the mod's `.pw.toml`, its
  `index.toml` hash line, and `pack.toml`'s index hash — instead of a `.replaced/` directory.
- The dead ModpackUploader lines in `.gitignore` go with it.
- `packwiz update --all` will report *"A supported update system for X cannot be found"* for the two
  unmanaged jars. This is expected, non-fatal and non-mutating, not a defect to fix.
- `packwiz update` prompts interactively and has no `--yes`, so bumps are not scriptable unattended.

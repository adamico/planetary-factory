# Tracking, updating and publishing a 124-jar pack whose three key jars are built locally

**Retrieved**: 2026-08-26. Sources are packwiz `dfd8b68` (2026-02-18, tip of `main`), the packwiz
docs site, the Modrinth and CurseForge format specs, and GitHub REST for every maintenance figure.

**Answer in one line: packwiz is the only tool of the five that can express a locally-built jar as a
first-class entry, and it can do it two ways — as an unmanaged index file (`metafile = false`, hashed
by `packwiz refresh`, exported into `overrides/`) or, better for the two forks, as a `[update.github]`
metafile pointed at a GitHub release of the fork, which is on Modrinth's download allowlist; every
other tool examined either has no local-file concept at all or has one that is explicitly a
manual-copy escape hatch, and nothing anywhere diffs your installed jars against the manifest except
`packwiz cf detect` (which runs in the wrong direction) and `packwiz-installer` (which runs on the
consumer's machine).**

---

## Method and trust levels

Every claim below is tagged:

- **[src]** — read out of the tool's own source at a named file and commit. Highest trust here,
  because most of these behaviours are undocumented.
- **[doc]** — stated by the tool's or platform's own documentation.
- **[api]** — GitHub REST, retrieved 2026-08-26.
- **[repo]** — observed in this repository.
- **[unverified]** — inferred, not confirmed against a primary source; called out as such.

packwiz source was read from a fresh clone of `github.com/packwiz/packwiz` at commit
`dfd8b68a4796c763e25bad50265ea1f1233e24f1`, *"Support neoforge for 26.1 and above (#386)"*,
2026-02-18. Paths below are relative to that repo root.

### First, a correction to the brief

The task description says this repo "already ships `Cat-Downloader-Legacy.jar` and has `automation/`
config for it". **It does not** **[repo]**:

- `automation/` does not exist on disk.
- `Cat-Downloader-Legacy.jar` does not exist on disk, and `git ls-files` returns nothing for it.
- `.gitignore` does contain `!Cat-Downloader-Legacy.jar` and four `automation/…` ignore lines, under
  a `# Automation stuff` heading — alongside `!server_files/**`, `.github_changelog_generator` and
  `CHANGELOG-GENERATED.md`, none of which exist either.

Those lines are **inherited boilerplate from whatever pack template this instance was seeded from**,
not evidence of an adopted workflow. Nothing in this pack is tracked by Cat-Downloader today. Cat
Downloader is still evaluated below on its merits, but it should not be treated as an incumbent.

For reference, the state being described **[repo]**: `mods/` holds **124 jars**, `mods` is ignored
wholesale, and the three special ones are `planetaryfactory_core-0.1.0.jar` (8.6K),
`gcyr-1.21.1-0.2.4+gt7.0.2-src.jar` (1.5M) and `Respoiled-neoforge-1.21.1-6.2.3-pf1.jar` (120K) —
note that two of the three already carry a **fork marker in the filename** (`-src`, `-pf1`), which is
the manual convention a manifest would formalise.

---

## 1. packwiz

**Maintenance** **[api]**: `packwiz/packwiz`, 975 stars, `archived: false`, last push
**2026-02-18**. **There are no tags and no releases** — `GET /repos/packwiz/packwiz/tags` returns
`[]` and the releases list is empty. packwiz is distributed as CI nightlies or `go install`, so
"pin the tool version" means pinning a commit hash, not a release. That is a real operational
cost and should be recorded as such.

`packwiz-installer` (the consumer-side half) is a separate repo: 71 stars, last release **v0.5.14,
2024-04-21**, last push 2024-06-27 **[api]**. `packwiz-installer-bootstrap` last released
**v0.0.3, 2020-07-12** **[api]**. The authoring tool is alive; the *installer* half has been quiet
for two years. That matters only if you publish via the packwiz-native route (§1.8).

### 1.1 The three file formats

**`pack.toml`** **[doc]** — required `name`, `pack-format` (currently `1.1.0`; "All consumers should
fail to load the modpack if the major version is greater than the version they support"), and
`versions` (a map; only `minecraft` is mandatory, plus one of `fabric`/`forge`/`neoforge`/`quilt`/
`liteloader`). Plus an `index` table with `file`, `hash-format` and `hash` — i.e. **the pack file
carries the hash of the index**, so the whole tree is a hash chain rooted in one small file.
Optional `author`, `description`, `version`; the docs note `version` is *not* used to determine
outdated status. See <https://packwiz.infra.link/reference/pack-format/pack-toml/>.

**`index.toml`** **[doc]** — a `hash-format` and an array of `[[files]]`, each with `file`, `hash`,
optional `hash-format`, `alias`, `preserve` ("Don't overwrite the file when updating" **[src]**,
`core/indexfiles.go:29`) and **`metafile`**. The `metafile` flag is the whole distinction:
`metafile = true` means the entry "points to a .toml metadata file, which references a file outside
the pack"; `metafile = false` (the default) means the entry **is** a real file inside the pack repo,
downloaded verbatim. See <https://packwiz.infra.link/reference/pack-format/index-toml/>.

**`<mod>.pw.toml`** **[doc] [src]** — the struct is `core.Mod` in `core/mod.go`:

```go
type Mod struct {
	Name     string      `toml:"name"`
	FileName string      `toml:"filename"`
	Side     string      `toml:"side,omitempty"`
	Pin      bool        `toml:"pin,omitempty"`
	Download ModDownload `toml:"download"`
	Update   map[string]map[string]interface{} `toml:"update"`
	Option   *ModOption  `toml:"option,omitempty"`
}
type ModDownload struct {
	URL        string `toml:"url,omitempty"`
	HashFormat string `toml:"hash-format"`
	Hash       string `toml:"hash"`
	// Mode defaults to modeURL (i.e. use URL when omitted or empty)
	Mode string `toml:"mode,omitempty"`
}
const (
	ModeURL string = "url"
	ModeCF  string = "metadata:curseforge"
)
```

So `mode` has exactly **two** values **[src]**: `"url"` (or empty, equivalent) meaning "the `url`
field is a direct download link", and `"metadata:curseforge"` meaning "there is no usable direct
URL; resolve the file through the CurseForge API instead" — this is the distribution-disabled-mod
case. `core/download.go:686` is the only place `Mode` is branched on. **The docs page for
`.pw.toml` does not document `mode` at all**; the two constants were read from source.

**How a mod is pinned, precisely**: a `.pw.toml` records `download.hash` (sha256 by default) of the
*jar*; `index.toml` records the hash of the *`.pw.toml`*; `pack.toml` records the hash of
*`index.toml`*. Version identity for an updatable mod is carried in `[update.<provider>]` —
e.g. `[update.curseforge] file-id = … project-id = …`, `[update.modrinth] mod-id = … version = …`.

Separately, `pin = true` (`packwiz pin <name>` / `packwiz unpin`, `cmd/pin.go`) **[src]** freezes a
mod against `packwiz update`. In `--all` mode a pinned mod with an available update prints
`Update skipped for pinned mod %s`; targeted, it hard-exits with *"Version is pinned; run the unpin
command to allow updating"* **[src]** `cmd/update.go`.

### 1.2 Can a locally-built jar be a first-class entry? **Yes — two ways, both real.**

This is the crux, so it is answered from source rather than docs.

**Route A — unmanaged file (`metafile = false`).** `Index.Refresh()` (`core/index.go`) **[src]**
walks the entire pack root, honours `.packwizignore` (gitignore syntax, with built-in defaults that
already exclude `.git/**`, `.DS_Store`, `/*.zip`, `*.mrpack` and the packwiz binary), and for every
non-ignored file calls `updateFile`, which sha256-hashes it and sets `markAsMetaFile` **only** if
the basename ends in `.pw.toml`. Everything else lands in `index.toml` as a plain hashed entry.

So: **drop `planetaryfactory_core-0.1.0.jar` into `mods/`, run `packwiz refresh`, and it is a
tracked, hashed, first-class index entry.** No metafile, no URL, no exception list. It is exactly as
"in the pack" as `config/` or `kubejs/` are. `installToPack` writing a new jar plus a `packwiz
refresh` is a complete update cycle for it, and the diff is one hash line in `index.toml`.

The cost is that the jar itself must then live **in the git repo**, which for this pack means
reversing the `**/*.jar` ignore for those three paths. `planetaryfactory_core` at 8.6K is nothing;
Respoiled at 120K is fine; the GCyR fork at **1.5M per version** is the one to think about, since
git stores every historical copy.

**Route B — `[update.github]` metafile.** packwiz has a **third updater** beside CurseForge and
Modrinth, which the brief did not mention and which is the better answer for the two forks. From
`github/updater.go` **[src]**:

```go
type ghUpdateData struct {
	Slug   string `mapstructure:"slug"`
	Tag    string `mapstructure:"tag"`
	Branch string `mapstructure:"branch"`
	Regex  string `mapstructure:"regex"`
}
```

`packwiz github add [URL|slug]` — *"Add a project from a GitHub repository URL or slug"*
(`github/install.go`) **[src]**. Push the fork to a GitHub repo, cut a release with the built jar as
an asset, and `[update.github] slug = "you/gcyr-fork"` with a `regex` selecting the asset makes the
fork **an ordinary updatable entry**: `packwiz update gcyr` checks the fork's releases exactly as it
would check Modrinth. This is the closest thing to a documented story for "a jar you build yourself",
and it is decisive for §1.7 because **`github.com` is on Modrinth's download allowlist** (§2.2).

**Route C — `packwiz url add`, and why it is *not* the local-jar answer.** `url/install.go`
**[src]**: `packwiz url add [name] [url]` parses the URL, **rejects any scheme that is not `http` or
`https`** (`fmt.Println("Unsupported URL scheme:", dl.Scheme); os.Exit(1)`) — so there is **no
`file://` path, and no local-path form**. It then *downloads the file immediately* to compute its
sha256 (`getHash`), and writes a metafile with `download.url` and **no `[update]` table at all**.
It also refuses, absent `--force`, to add modrinth.com / curseforge.com / forgecdn.net URLs,
telling you to use the provider command instead.

So `url add` produces a perfectly valid, first-class, **non-updatable** entry — good for a jar you
host somewhere, useless for a jar that only exists on your disk.

**What is *not* possible** — stated plainly, because it constrains any thin layer:

- **You cannot invent your own `[update.<key>]` block.** `core.LoadMod` **[src]** iterates
  `mod.Update` and, for any key not in `core.Updaters`, returns `errors.New("Update plugin " + k + "
  not found!")` — a hard load failure for *every* packwiz command, not a warning. There are exactly
  three updaters: `curseforge`, `modrinth`, `github`. A hypothetical `[update.gradle]` would break
  the pack.
- **A metafile cannot point at a local path.** `download.url` is fetched by `core.GetWithUA`;
  there is no local-file mode.

### 1.3 What `packwiz update --all` does to such an entry: **nothing, harmlessly**

`cmd/update.go` **[src]**, `--all` branch: it calls `index.LoadAllMods()` (metafiles only —
`metafile = false` entries are never loaded as mods at all), then for each mod scans `modData.Update`
for a key present in `core.Updaters`. If none is found:

```go
if !updaterFound {
	fmt.Printf("A supported update system for \"%s\" cannot be found.\n", modData.Name)
}
```

It prints that line and **moves on**. No error, no exit code, no mutation. A Route-A unmanaged jar is
not even visible to `update`; a Route-C `url add` metafile produces one informational line per run.

The rough edge is ergonomic, not technical: with three special jars you get three such lines every
time, and they read like warnings. `packwiz pin` does **not** silence them — pinning only suppresses
mods that *have* an updater and *have* an update available.

### 1.4 Does anything diff installed jars against the manifest? **Not on the author's machine.**

Two things come close and neither is the tool you want:

- **`packwiz cf detect`** (`curseforge/detect.go`, marked *"(experimental)"* in its own help text)
  **[src]** walks `mods/`, computes CurseForge's murmur2 fingerprint over each jar with whitespace
  bytes stripped, submits them in bulk to the CF fingerprint API, and for every **exact match**
  creates the `.pw.toml` metafile **and deletes the loose jar**. Unmatched fingerprints are printed
  under `Failed to match the following %d files:` and **left on disk untouched**, where the
  subsequent `index.Refresh()` picks them up as Route-A unmanaged entries.

  Read that twice, because it is the **bootstrap path for this exact pack**: point it at the 124
  jars, and ~121 become CurseForge-pinned metafiles automatically while the three local ones fall
  out of the sieve into unmanaged entries. That is the whole TRACK problem solved in one command
  plus a `refresh`. (Partial matches print `The following fingerprints were partial and I don't know
  what to do!!!` and are dropped — expect a few, and check them by hand.)

  Note it runs in the *ingest* direction: jar → manifest. It will not tell you "your `mods/` has
  drifted from the manifest."

- **`packwiz-installer`** does the real diffing, but on the **consumer's** machine.
  `UpdateManager.kt` **[src]** keeps a local `ManifestFile` of `cachedFiles` with their hashes plus
  `packFileHash` / `indexFileHash`; it short-circuits entirely when `manifest.packFileHash ==
  packFileSource.hash`, re-downloads files whose cached hash no longer matches
  (`File ${fileUri.filename} invalidated, marked for redownloading`), and **deletes files that have
  been removed from the index** (`Files.deleteIfExists(file.cachedLocation!!.nioPath)`, with the
  warning string `"Failed to delete file removed from index"`). It invalidates *everything* if the
  side changes.

There is a `--check` flag TODO in `cmd/update.go` **[src]** and nothing else. **If you want "CI
fails when `mods/` doesn't match `index.toml`", you write it yourself** — but it is ten lines,
because `index.toml` already stores a sha256 for every entry and `packwiz refresh` is idempotent:
run `packwiz refresh` and fail if `git diff --exit-code` is non-empty. That is the thin layer.

### 1.5 Bump one mod, then roll it back, in git terms

Bumping a CurseForge mod with `packwiz update <name>` touches exactly three files **[src]**
(`cmd/update.go` writes the mod, then `index.RefreshFileWithHash`, `index.Write`,
`pack.UpdateIndexHash`, `pack.Write`):

1. `mods/<mod>.pw.toml` — `filename`, `download.url`, `download.hash`, and `[update.curseforge]
   file-id` change.
2. `index.toml` — one `hash` line, for that metafile.
3. `pack.toml` — the `index.hash` line.

**Rollback is `git revert` of that commit.** Nothing else is required: there is no lockfile to
regenerate, no cache to bust, and the hash chain is restored by construction because all three
hashes are content-derived and all three are in the diff. The jar itself is not in git and does not
need to be — the reverted `.pw.toml` fully identifies the old file.

For a **Route-A local jar** the shape differs: the diff is one `index.toml` hash line **plus the
binary jar blob**. `git revert` still works and is still atomic, but the repo carries every version
of the jar forever. For a **Route-B GitHub-release fork**, rollback is identical to the CurseForge
case — three text files — which is the strongest argument for Route B over Route A for the 1.5M
GCyR fork.

One caveat worth stating: `packwiz update --all` is **interactive** — it prints the update list and
calls `cmdshared.PromptYesNo("Do you want to update? [Y/n]: ")` **[src]**. There is no `--yes`. Any
automation must feed it stdin. Also, updating all mods at once produces one commit touching dozens
of metafiles, which makes per-mod rollback a `git revert -n` plus manual `git checkout` of the paths
you want. **Prefer one commit per mod bump** if rollback granularity matters.

### 1.6 `packwiz curseforge export` with a local/URL jar: **it is bundled into `overrides/`**

`curseforge/export.go` **[src]**. It refreshes the index, loads all metafiles, filters by `--side`
(**default `client`**, note — not `both`), then partitions:

```go
projectRaw, ok := mod.GetParsedUpdateData("curseforge")
if ok { cfFileRefs = append(...) } else { nonCfMods = append(nonCfMods, mod) }
```

Entries **with** CF update data become `{projectID, fileID, required}` records in `manifest.json`.
Everything else — a `url add` entry, a `[update.github]` fork, a `[update.modrinth]` mod — is
**downloaded and written into the zip under `overrides/`** via `cmdshared.AddToZip`. Route-A
unmanaged files are handled by `cmdshared.AddNonMetafileOverrides(&index, exp)` **[src]**, which
copies every `!v.IsMetaFile()` index entry into `overrides/` straight from disk.

So the answer to "what happens to an entry with neither project ID nor file ID" is: **it ships as a
bundled file, not as a manifest reference.** It works. But the exporter prints, and means, this
**[src]** (`cmdshared.PrintDisclaimer`):

> Disclaimer: you are responsible for ensuring you comply with ALL the licenses, or obtain
> appropriate permissions, for the files "added to zip" below
> Note that mods bundled within a CurseForge pack must be in the Approved Non-CurseForge Mods list

That is the real constraint on publishing to CurseForge, and it is a **policy** problem, not a tool
problem: `planetaryfactory_core` is yours, but a **fork of GCyR and a fork of Respoiled bundled into
a CurseForge pack** need to satisfy both those mods' licences and CurseForge's approved-non-CF list.
Flagged as an open question in §7 — it is not a decision to make from source code.

The mod also appears in `modlist.html` without a link, via the branch commented
`// TODO: how to handle mods that don't have metadata???` **[src]**.

### 1.7 `packwiz modrinth export` with a local/URL jar: **`overrides/` unless the host is allowlisted**

`modrinth/export.go` **[src]**. The gate is one function:

```go
var whitelistedHosts = []string{
	"cdn.modrinth.com", "github.com", "raw.githubusercontent.com", "gitlab.com",
}
func canBeIncludedDirectly(mod *core.Mod, restrictDomains bool) bool {
	if mod.Download.Mode == core.ModeURL || mod.Download.Mode == "" {
		if !restrictDomains { return true }
		modUrl, err := url.Parse(mod.Download.URL)
		if err == nil {
			if slices.Contains(whitelistedHosts, modUrl.Host) { return true }
		}
	}
	return false
}
```

Pass → the file becomes a `files[]` entry in `modrinth.index.json` with sha1+sha512+fileSize+env.
Fail → `cmdshared.AddToZip` into `overrides/`, `client-overrides/` or `server-overrides/` by side.
`--restrictDomains` defaults to `true`; setting it `false` produces an `.mrpack` that Modrinth will
reject on upload but that works for private distribution. `mode = "metadata:curseforge"` entries
always fail the check and are always bundled.

**This is the payoff of Route B.** A fork whose jar is a **GitHub release asset** is on the
allowlist, so `packwiz mr export` emits it as a proper `files[]` entry with hashes — a real,
resolvable, third-party-installable pin — rather than bundling the binary. A Route-A local jar
cannot be, and is bundled. If publishing to Modrinth is a goal, **put the two forks on GitHub.**

### 1.8 Other packwiz commands worth knowing

`packwiz serve` — *"Run a local development server"* **[src]**, i.e. serve the pack over HTTP so
`packwiz-installer` can install it without publishing anything. `packwiz remove` is documented as
*"equivalent to manually removing the file and running packwiz refresh"* **[src]** — which is a neat
statement of the whole model. `packwiz rehash` migrates hash formats. `packwiz list`, `packwiz init`,
`packwiz cf import` (imports an existing CF pack zip or `minecraftinstance.json`).

There is a `no-internal-hashes` setting **[src]** (`core/index.go`) that writes empty hashes into the
index — relevant only if the jar-blob churn in `index.toml` ever becomes a review nuisance.

---

## 2. The two publish formats, on their own terms

### 2.1 CurseForge `manifest.json`

CurseForge does not publish a formal spec; the authoritative readable description is the struct
every tool implements. packwiz's is `curseforge/packinterop/manifest.go` **[src]**:

```go
type cursePackMeta struct {
	Minecraft struct {
		Version    string
		ModLoaders []modLoaderDef   // {id: "neoforge-21.1.x", primary: bool}
	}
	ManifestType    string   // "minecraftModpack"
	ManifestVersion uint32
	NameInternal    string `json:"name"`
	Version, Author string
	ProjectID       uint32
	Files []struct{ ProjectID, FileID uint32; Required bool }
	Overrides string     // conventionally "overrides"
}
```

**Every entry in `files[]` is a numeric `(projectID, fileID)` pair.** There is no URL field, no hash
field, no name field. A jar that is not a CurseForge project **cannot be expressed in `files[]` at
all** — the format has no slot for it. Its only home is the `overrides/` directory, which is a plain
copied file tree with no hashes and no versions. That is a **format-level** limitation, so it binds
every tool that emits CF packs, not just packwiz.

### 2.2 Modrinth `.mrpack` / `modrinth.index.json`

Modrinth *does* publish a spec **[doc]**
(<https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack>): a zip containing
`modrinth.index.json` with `formatVersion` (1), `game` ("minecraft"), `versionId`, `name`, optional
`summary`, `files[]` and `dependencies{}`. Each file carries `path`, `hashes` (**sha1 and sha512
both required**), `downloads` (HTTPS URLs), `fileSize`, and optional `env.client` / `env.server` set
to `required` / `optional` / `unsupported`. Paths must not contain `..`, drive letters or absolute
prefixes.

Overrides are three layered directories — `overrides/`, then `server-overrides/`, then
`client-overrides/` — each overwriting the previous.

**The allowlist is part of the spec, not a packwiz invention**: files may only be downloaded from
`cdn.modrinth.com`, `github.com`, `raw.githubusercontent.com`, `gitlab.com` **[doc]**. packwiz's
`whitelistedHosts` is a verbatim copy **[src]**.

The consequence for us is the good news of this whole document: **`.mrpack` can express a
locally-built jar as a real hashed, versioned manifest entry — provided you publish it to a GitHub
or GitLab release.** CurseForge's format cannot, under any circumstance.

---

## 3. ferium

**Maintenance** **[api]**: `gorilla-devs/ferium`, 1,419 stars, `archived: false`, last push
**2026-05-16**, but the **last release is `v4.7.1`, 2024-09-17** — nearly two years of unreleased
commits. Users are on a two-year-old binary unless they build from source.

Sources: Modrinth, CurseForge, GitHub Releases; also imports Modrinth and CF modpacks **[doc]**.

**(1) Locally-built jar as a first-class entry: no — explicitly a manual exception.** The README's
own words **[doc]**:

> If you want to use files that are not downloadable by ferium, place them in a subfolder called
> `user` in the output directory. Files here will be copied to the output directory when upgrading.

That is the definition of what this evaluation is trying to avoid: an out-of-band folder, with no
version, no hash, no manifest entry, and no record of provenance. Worse, the same README states:

> When upgrading, any files not downloaded by ferium will be moved to the `.old` folder in the
> output directory.

So ferium's normal operation actively *evicts* anything it did not place — the `user/` folder exists
precisely because the default behaviour is hostile to foreign jars.

**(2) Diffing installed vs manifest: no** — it reconciles by moving unrecognised files to `.old`,
which is destructive reconciliation, not reporting.

**(3) Bump-then-rollback: not a git operation.** ferium's state lives in a **global** config at
`~/.config/ferium/config.json` (or `%APPDATA%/ferium/config/config.json`), overridable with
`FERIUM_CONFIG_FILE` / `--config-file` **[doc]**. It is a per-user, per-machine profile file, not a
pack artefact. You could `FERIUM_CONFIG_FILE=./ferium.json` it into the repo **[unverified — not
tested]**, but you would then be fighting the tool's design, and ferium's GitHub-Releases source
still cannot express "a jar built from the Gradle subproject next to this file".

**Verdict: disqualified on requirement one.**

---

## 4. Cat-Downloader-Legacy

**Maintenance** **[api]**: `Kanzaji/Cat-Downloader-Legacy`, **3 stars**, `archived: false`, last
push **2024-11-20**, last release **2.1.6, 2024-02-09**. Two and a half years since a release.

What it is, in the author's own words **[doc]** (repo README): *"an app that is meant to allow for
easy synchronization of minecraft mods between modpack developers, with use of, for example, Git
hooks."* It reads three input formats — `.mrpack` / Modrinth index, CurseForge `manifest.json`, and
the CF launcher's `minecraftinstance.json` — and downloads the referenced files, verifying SHA-512
and file size. It exists because Vazkii's InstanceSync was archived and lacked verification.

**(1) Locally-built jar: no.** It is a *downloader*. Its manifest is one of the three formats above,
all of which are `(projectID, fileID)` or allowlisted-URL based, so a local jar has no expression in
any of them. There is no local-file or override-passthrough concept in the described feature set.

**(2) Diffing: partially, and this is its one real strength** — it syncs a mods folder to match a
manifest and verifies every file by SHA-512 and length. That is genuine drift detection, but it is
drift *correction toward* a manifest it cannot author.

**(3) Bump-then-rollback:** it does not author manifests at all, so the bump happens in whatever
tool produced the `manifest.json` (in practice, the CurseForge launcher, whose
`minecraftinstance.json` this repo **already gitignores as "contains local paths"** **[repo]**).
Rollback is a git revert of that JSON, which is fine — but the manifest still cannot name the three
local jars.

**Verdict: complementary at best, and superseded by `packwiz-installer` for the sync role.
Not a TRACK solution.** The `.gitignore` lines referencing it should be deleted as dead weight
whatever else is decided.

---

## 5. MultiMC / Prism instance format

**Maintenance** **[api]**: `PrismLauncher/PrismLauncher`, 10,233 stars, last push **2026-08-26**
(today), latest release **11.1.0-pre1, 2026-08-18**. Extremely healthy — but it is a launcher, not a
pack manifest tool.

An instance is `instance.cfg` plus `mmc-pack.json` (the component list: Minecraft, LWJGL, the
loader) — `mmc-pack.json` is parsed in `launcher/minecraft/PackProfile.cpp` **[src]**. Components
are loaders and libraries, **not mods**; there is no mod list in `mmc-pack.json`.

The interesting finding is what Prism uses for mods instead: **Prism has adopted packwiz's `.pw.toml`
format as its internal mod index**, under `mods/.index/`. See `launcher/modplatform/packwiz/
Packwiz.h` and `Packwiz.cpp` **[src]**, whose `V1::Mod` struct mirrors `core.Mod` field for field —
`slug`, `name`, `filename`, `side`, `mode`, `url`, `hash_format`, `hash`, `provider`, `file_id`,
`project_id`, `version_number`, `dependencies`.

That is a strong independent endorsement of packwiz's format as the de-facto standard. But note the
validity test in the same header **[src]**:

```cpp
auto isValid() const -> bool { return !slug.isEmpty() && !project_id.isNull(); }
```

**Prism considers a metafile with no `project_id` invalid** — i.e. a `packwiz url add` entry or a
`[update.github]` fork is *not* a valid indexed mod to Prism. Prism will still run the jar (it is a
file in `mods/`); it just will not manage or update it. So the local-jar limitation reappears here,
one layer down.

**(1) no — mods are not in the instance manifest at all; (2) no; (3) not applicable.**

---

## 6. Comparison

| | packwiz | ferium | Cat-Downloader-Legacy | CF `manifest.json` (format) | `.mrpack` (format) | Prism instance |
| --- | --- | --- | --- | --- | --- | --- |
| Last release | **none — no tags at all** | v4.7.1, 2024-09-17 | 2.1.6, 2024-02-09 | n/a | n/a | 11.1.0-pre1, 2026-08-18 |
| Last push | 2026-02-18 | 2026-05-16 | 2024-11-20 | n/a | n/a | 2026-08-26 |
| Stars | 975 | 1,419 | 3 | n/a | n/a | 10,233 |
| **(1) Local jar as first-class entry** | **yes** — unmanaged index file, or `[update.github]` on a release | **no** — `user/` folder, manual copy | **no** | **no** — `files[]` is `(projectID,fileID)` only | **yes, if hosted on github/gitlab**; else `overrides/` | **no** — `isValid()` requires `project_id` |
| **(2) Diffs installed vs manifest** | not on the author side (`cf detect` ingests; `packwiz-installer` diffs on the consumer's machine); trivially added via `refresh` + `git diff --exit-code` | no — moves foreign files to `.old` | yes, SHA-512 + size, against a manifest it cannot author | n/a | n/a | no |
| **(3) Bump then rollback** | **3 text files** (`.pw.toml`, `index.toml`, `pack.toml`); `git revert` | global `~/.config` JSON, not a repo artefact | n/a — does not author | revert the JSON | revert the JSON | n/a |
| Loader support incl. NeoForge 1.21.1 | yes | yes | via manifest | yes | yes | yes |

---

## 7. Recommendation, ranked against the locally-built-jar requirement

**1. packwiz, plus a ~20-line thin layer. Nothing else clears the bar.** It is the only tool where a
jar you built is a real manifest entry rather than a folder you are told to copy things into, and it
is the only one whose bump-and-rollback is genuinely three text files under version control. The
format is independently validated by Prism having adopted it wholesale.

The concrete shape, in the order it would be done:

1. `packwiz init` at the repo root, then **`packwiz cf detect`** to fingerprint the 124 jars into
   metafiles in one shot, then `packwiz refresh`. Expect ~121 hits and a handful of partial matches
   to fix by hand.
2. **`planetaryfactory_core` → Route A** (unmanaged index file). 8.6K, built in-repo, changes with
   the repo; committing the jar is honest and cheap, and `installToPack` + `packwiz refresh` is the
   whole update cycle.
3. **The GCyR fork and the Respoiled fork → Route B** (`[update.github]` against a release of your
   fork). This buys three things Route A does not: a 1.5M binary stays out of git history, `packwiz
   update` treats the fork as an ordinary updatable mod, and — decisively — `github.com` is on
   Modrinth's allowlist so `mr export` emits them as real hashed `files[]` entries.
4. **The thin layer is a CI check, not a tool**: `packwiz refresh && git diff --exit-code`. Because
   `refresh` is idempotent and hashes everything, a non-empty diff *is* the drift report. This is the
   one gap in packwiz (`cmd/update.go` still carries a `// TODO: --check flag?`) and it is a
   two-line fix, not a bespoke tool.
5. Add `.packwizignore` for `saves/`, `logs/`, `crash-reports/`, `downloads/`, `docs/`, `mod/build/`,
   `tests/` — otherwise `refresh` will index the entire instance, including everything the current
   `.gitignore` excludes.

**2. Nothing.** Staying with the status quo is a more defensible second place than any of the
runners-up, because ferium and Cat-Downloader would each add a tool *and* leave the three jars as
manual exceptions — strictly worse than today, where at least the exception is explicit.

**3. ferium** — disqualified on requirement one; its `user/` folder is precisely the manual exception
the brief rules out, and its config is a machine-global file.

**4. Cat-Downloader-Legacy** — 3 stars, no release since Feb 2024, cannot author a manifest, and its
apparent presence in this repo is boilerplate (§Method). Its sync-and-verify role is done better by
`packwiz-installer`.

**5. Prism/MultiMC instance format** — not a candidate; it has no mod manifest, and where it does
have one it is packwiz's.

### Open questions a human must decide

- **Licensing and CurseForge policy for the two forks.** `packwiz cf export` will happily bundle a
  forked GCyR and a forked Respoiled into `overrides/`, and its own disclaimer says bundled mods
  "must be in the Approved Non-CurseForge Mods list". Whether the upstream licences permit
  redistributing modified jars, and whether CurseForge would approve it, is a legal/policy call.
  **This may be the question that decides whether CurseForge is a publish target at all.**
- **Do the forks get public GitHub repos?** Route B's entire advantage rests on it. If the forks must
  stay private, both drop to Route A and Modrinth publishing bundles two binaries into `overrides/`.
- **1.5M of GCyR jar per version in git history** — acceptable, or is Route B mandatory for that
  reason alone?
- **Pinning packwiz itself.** No tags, no releases **[api]**. Vendoring a built binary, pinning a
  commit for `go install`, or accepting nightly drift are three different answers with different
  reproducibility costs.
- **Is publishing actually a goal, or only tracking?** If the pack is never published to CF or
  Modrinth, §1.6/§1.7 and the entire licensing question fall away, and the recommendation collapses
  to "packwiz for TRACK/UPDATE, `packwiz serve` for distribution" — a much smaller commitment.
- **Whether `packwiz cf detect` matches all 124 jars.** The partial-match branch literally prints
  *"I don't know what to do!!!"* **[src]**. **[unverified]** — worth a dry run before committing to
  the migration.

### Not answered here

- Whether `packwiz-installer`'s two-year release gap matters in practice on NeoForge 1.21.1
  **[unverified]** — not tested against this pack.
- Whether `[update.github]`'s `regex` can reliably select one asset from a release that also carries
  sources and javadoc jars **[unverified]** — the field exists and is a `regexp2` pattern **[src]**,
  but no behaviour was tested.
- Whether `no-internal-hashes` interacts badly with the drift check in §7.4 (it writes empty hashes,
  which would make `git diff` blind to jar content changes) **[unverified]**.

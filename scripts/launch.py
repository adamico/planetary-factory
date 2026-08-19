#!/usr/bin/env python3
"""Assemble and run the CLI launch command for this instance.

CurseForge's own launcher is a GUI, but everything it needs is on disk: the
merged NeoForge + vanilla version JSONs under Install/versions, the library
tree, and a bundled JRE. This rebuilds the java command from those and runs it
in offline mode, which is enough for singleplayer.
"""
import json, os, subprocess, sys, uuid
from pathlib import Path

INSTALL = Path.home() / "Documents/curseforge/minecraft/Install"
INSTANCE = Path(__file__).resolve().parent.parent
VERSION = "neoforge-21.1.248"
JAVA = INSTALL / "java/java-runtime-delta/Contents/Home/bin/java"
LIBS = INSTALL / "libraries"


def load(v):
    return json.load(open(INSTALL / f"versions/{v}/{v}.json"))


def lib_path(name):
    group, artifact, version, *rest = (name.split(":") + [None])[:4]
    classifier = f"-{rest[0]}" if rest and rest[0] else ""
    return LIBS.joinpath(*group.split("."), artifact, version,
                         f"{artifact}-{version}{classifier}.jar")


def rules_pass(lib):
    for rule in lib.get("rules") or []:
        # Feature-gated args (quick play, demo, custom resolution) stay off: we
        # enable no features, and letting them through passes Minecraft
        # placeholders it then treats as real values.
        if "features" in rule:
            return False
        os_name = rule.get("os", {}).get("name")
        allow = rule["action"] == "allow"
        matches = os_name in (None, "osx")
        if allow and not matches:
            return False
        if not allow and matches:
            return False
    return True


def main():
    nf = load(VERSION)
    mc = load(nf["inheritsFrom"])

    classpath, seen = [], set()
    for lib in nf["libraries"] + mc["libraries"]:
        if not rules_pass(lib):
            continue
        p = lib_path(lib["name"])
        if p.exists() and p not in seen:
            seen.add(p)
            classpath.append(str(p))
    # NeoForge resolves the Minecraft jars itself out of libraryDirectory; putting
    # versions/1.21.1/1.21.1.jar on the classpath too makes it a second module
    # exporting net.minecraft and the module layer fails to resolve.
    classpath.append(str(INSTALL / f"versions/{VERSION}/{VERSION}.jar"))
    classpath.append(str(LIBS / "net/minecraft/client/1.21.1-20240808.144430/client-1.21.1-20240808.144430-extra.jar"))

    subs = {
        "library_directory": str(LIBS),
        "classpath_separator": os.pathsep,
        "version_name": VERSION,
        "natives_directory": str(INSTALL / "natives"),
        "launcher_name": "claude-cli",
        "launcher_version": "1",
        "classpath": os.pathsep.join(classpath),
        "auth_player_name": "Dev",
        "version_type": "release",
        "game_directory": str(INSTANCE),
        "assets_root": str(INSTALL / "assets"),
        "assets_index_name": mc["assets"],
        "auth_uuid": uuid.uuid4().hex,
        "auth_access_token": "0",
        "clientid": "0",
        "auth_xuid": "0",
        "user_type": "legacy",
        "resolution_width": "1280",
        "resolution_height": "720",
    }

    def expand(args):
        out = []
        for a in args:
            if isinstance(a, dict):
                if not rules_pass(a):
                    continue
                v = a["value"]
                out.extend(v if isinstance(v, list) else [v])
            else:
                out.append(a)
        return [_sub(s, subs) for s in out]

    def _sub(s, subs):
        for k, v in subs.items():
            s = s.replace("${%s}" % k, v)
        return s

    jvm = expand(nf["arguments"]["jvm"] + mc["arguments"]["jvm"])
    game = expand(mc["arguments"]["game"] + nf["arguments"]["game"])

    cmd = [str(JAVA), "-Xmx6G", "-XstartOnFirstThread", *jvm, nf["mainClass"], *game]
    if "--demo" in cmd:
        cmd.remove("--demo")
    cmd += sys.argv[1:]

    print(" ".join(cmd[:6]), "...", file=sys.stderr)
    os.chdir(INSTANCE)
    sys.exit(subprocess.call(cmd))


if __name__ == "__main__":
    main()

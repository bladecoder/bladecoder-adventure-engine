#!/usr/bin/env python3
"""Project diagnostics, validation and explicit Ink/puzzle/graphics automation; Python 3.10+."""

import argparse
import ctypes
import ctypes.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

sys.dont_write_bytecode = True
from art_sources import inspect_art
from asset_manifest import validate_manifests
from chapter_validate import validate_chapters
from ink_tools import compile_ink, test_ink
from ink_links import validate_ink_links
from engine_tests import test_puzzle, test_art
from project_config import load_project, toolkit_path, automation_path
from art_pipeline import export_preview, apply_preview
from atlas_tools import pack_preview

def find_project_root():
    tool_dir = Path(__file__).resolve().parent
    for candidate in (tool_dir, *tool_dir.parents):
        if (candidate / "adventure.json").is_file():
            return candidate
    return tool_dir.parents[1] if tool_dir.parent.name == "ai-toolkit" else tool_dir.parent


ROOT = find_project_root()
PATH_KEYS = {
    "java", "inklecate", "inkscape", "spine", "ffmpeg", "ffprobe", "sox",
    "magick", "fluidsynth_library", "soundfont", "gradle_cache", "gdx_jar",
    "gdx_tools_jar", "art_root",
}
PROFILES = ("foundation", "ink", "graphics", "spine", "audio", "art", "all")


class ConfigurationError(ValueError):
    pass


def project_path(value, root):
    path = Path(value).expanduser()
    return path if path.is_absolute() else root / path


def load_config(root, filename=None):
    explicit = filename or os.environ.get("ADVENTURE_TOOLS_CONFIG")
    path = project_path(explicit, root) if explicit else toolkit_path(root, "local.json")
    data = {}
    if explicit or path.exists():
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError) as exc:
            # Do not echo configuration contents, which may contain personal data.
            raise ConfigurationError(f"Cannot read tools configuration at {path}; use a valid JSON object.") from exc
        if not isinstance(data, dict) or set(data) - {"paths", "linked_roots"}:
            raise ConfigurationError("Configuration may contain only 'paths' and 'linked_roots' objects.")
    paths = data.get("paths", {})
    if not isinstance(paths, dict) or set(paths) - PATH_KEYS:
        raise ConfigurationError("Unknown path setting; see ai-toolkit/tools/local.example.json for supported keys.")
    if any(value is not None and (not isinstance(value, str) or not value.strip()) for value in paths.values()):
        raise ConfigurationError("Path settings must be nonempty strings or null.")
    result = {}
    linked = data.get("linked_roots", {})
    if not isinstance(linked, dict) or any(not re.fullmatch(r"[a-z][a-z0-9_-]*", key) or not isinstance(value, str) or not value.strip() for key, value in linked.items()):
        raise ConfigurationError("linked_roots must map logical names to nonempty local path strings.")
    if linked:
        result["linked_roots"] = linked
    for key in PATH_KEYS:
        value = os.environ.get("ADVENTURE_" + key.upper(), paths.get(key))
        if value is not None:
            if not value.strip():
                raise ConfigurationError(f"ADVENTURE_{key.upper()} must not be empty.")
            result[key] = value
    try:
        metadata = load_project(root)
    except (OSError, ValueError) as error:
        raise ConfigurationError('Cannot read a valid version-1 adventure.json project declaration.') from error
    if metadata.get('art_storage', 'repository') == 'repository' and 'art_root' not in result:
        result['art_root'] = 'raw/art'
    return result


def check(checks, identifier, status, message, **details):
    checks.append({"id": identifier, "status": status, "message": message, **details})


def unavailable(profile, *required_by):
    return "error" if profile in required_by else "warning"


def properties(path):
    result = {}
    if path.is_file():
        for line in path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith(("#", "!")):
                key, value = line.split("=", 1)
                result[key.strip()] = value.strip()
    return result


def executable(key, config, root, names, candidates=()):
    if key in config:
        value = config[key]
        # Bare names refer to PATH; paths are always relative to the project root.
        candidate = shutil.which(value) if "/" not in value and "\\" not in value else None
        candidate = Path(candidate).absolute() if candidate else project_path(value, root)
        return candidate, "configured"
    for name in names:
        found = shutil.which(name)
        if found:
            return Path(found).absolute(), "PATH"
    for candidate in candidates:
        if candidate.is_file():
            return candidate, "known installation location"
    return None, "not found"


def probe(command, root):
    """Run only a bounded version/help query, never through a shell."""
    try:
        result = subprocess.run(
            [str(part) for part in command], cwd=root, stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
            errors="replace", timeout=5, check=False,
        )
        return result.returncode, result.stdout.strip()
    except subprocess.TimeoutExpired:
        return None, "Version/help query timed out after 5 seconds."
    except OSError:
        return None, "Executable could not be started."


def tool_check(checks, key, config, root, profile, required_by, names, args, pattern, candidates=(), java=None):
    path, origin = executable(key, config, root, names, candidates)
    severity = unavailable(profile, *required_by)
    remedy = f"Install the tool or set ADVENTURE_{key.upper()} (or paths.{key} in ai-toolkit/tools/local.json)."
    if path is None or not path.is_file():
        check(checks, f"tool.{key}", severity, f"{key}: executable not found.", remedy=remedy,
              path=str(path) if path else None)
        return None
    if key == "spine":
        if not os.access(path, os.X_OK):
            check(checks, "tool.spine", severity, "Spine launcher is not executable.", path=str(path), remedy=remedy)
            return None
        # Launchers can activate/update the editor even for CLI queries. Inspect files only.
        updates = Path.home() / ".spine/updates"
        cached = sorted(p.name for p in updates.iterdir() if p.is_file() and re.fullmatch(r"\d+\.\d+\.\d+", p.name)) if updates.is_dir() else []
        check(checks, "tool.spine", "warning", "Spine located; editor version and runtime compatibility are unverified (launcher not executed).",
              path=str(path), version=None, cached_versions=cached, origin=origin,
              remedy="Use spine inspect/test with a pinned recipe to verify compatibility; cached versions do not identify the active editor.")
        return path
    command = [path, *args]
    if key == "inklecate" and path.suffix.lower() == ".jar":
        if java is None:
            check(checks, "tool.inklecate", severity, "The configured Ink compiler JAR needs a working Java executable.",
                  path=str(path), remedy="Configure paths.java and rerun this profile.")
            return None
        command = [java, "-jar", path, *args]
    elif not os.access(path, os.X_OK):
        check(checks, f"tool.{key}", severity, f"{key}: file is not executable.", path=str(path), remedy=remedy)
        return None
    code, output = probe(command, root)
    match = re.search(pattern, output, re.IGNORECASE)
    valid = code == 0 and match is not None
    # Native inklecate prints usage and returns 1 when invoked without a source.
    if key == "inklecate":
        valid = code in (0, 1) and "Usage: inklecate" in output
    if not valid:
        check(checks, f"tool.{key}", severity, f"{key}: version/help query failed or returned unexpected output.",
              path=str(path), remedy=remedy)
        return None
    version = match.group(1) if match else None
    check(checks, f"tool.{key}", "ok", f"{key}: available." if version else f"{key}: compiler responds; version not exposed by its help output.",
          path=str(path), version=version, origin=origin)
    return path


def skills_check(root, checks):
    from skill_tools import check_skills
    checks.extend(check_skills(root))


def foundation(root, checks):
    check(checks, "tool.python", "ok" if sys.version_info >= (3, 10) else "error",
          "Automation requires Python 3.10 or newer.", path=sys.executable, version=sys.version.split()[0])
    for filename in ("gradlew", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"):
        present = (root / filename).is_file()
        check(checks, "project." + filename, "ok" if present else "error",
              f"{filename}: {'present' if present else 'missing'}.")
    skills_check(root, checks)


def jar_check(root, config, checks, profile):
    gradle_home = project_path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")), root)
    cache = project_path(config["gradle_cache"], root) if "gradle_cache" in config else gradle_home / "caches/modules-2/files-2.1"
    game_version = properties(root / "gradle.properties").get("gdxVersion")
    # Prefer a matching pair; a newer gdx alone does not make a usable packer.
    cache_version = "1.10.0"
    if game_version and all(list((cache / "com.badlogicgames.gdx" / artifact / game_version).glob(f"*/{artifact}-{game_version}.jar"))
                            for artifact in ("gdx", "gdx-tools")):
        cache_version = game_version
    found_versions = []
    for key, artifact, member in (
        ("gdx_jar", "gdx", "com/badlogic/gdx/Gdx.class"),
        ("gdx_tools_jar", "gdx-tools", "com/badlogic/gdx/tools/texturepacker/TexturePacker.class"),
    ):
        candidates = []
        if key in config:
            candidates = [project_path(config[key], root)]
        else:
            base = cache / "com.badlogicgames.gdx" / artifact
            for version in dict.fromkeys((cache_version, game_version, "1.10.0")):
                if version:
                    candidates.extend(sorted((base / version).glob(f"*/{artifact}-{version}.jar")))
        path = candidates[0] if candidates else None
        if path is None:
            check(checks, f"resource.{key}", unavailable(profile, "graphics"), f"{artifact}: no binary JAR found in the configured cache.",
                  remedy=f"Set ADVENTURE_{key.upper()} to a binary JAR or prepare matching gdx/gdx-tools JARs; sources JARs are insufficient.")
            continue
        try:
            with zipfile.ZipFile(path) as archive:
                if member not in archive.namelist():
                    raise ValueError("Required class is missing")
        except (OSError, ValueError, zipfile.BadZipFile):
            check(checks, f"resource.{key}", unavailable(profile, "graphics"), f"{artifact}: unreadable JAR or required class missing.",
                  path=str(path), remedy=f"Configure paths.{key} with a binary {artifact} JAR.")
            continue
        match = re.fullmatch(re.escape(artifact) + r"-(\d+\.\d+\.\d+[^/]*)\.jar", path.name)
        version = match.group(1) if match else None
        found_versions.append(version)
        check(checks, f"resource.{key}", "ok", f"{artifact}: binary JAR found; transitive dependencies and pack execution not tested.",
              path=str(path), version=version)
    if len(found_versions) == 2 and (None in found_versions or len(set(found_versions)) != 1):
        check(checks, "resource.packer-pair", unavailable(profile, "graphics"), "Packing JAR versions differ or could not be determined.",
              remedy="Configure matching, versioned gdx and gdx-tools binary JARs.")
    elif len(found_versions) == 2 and found_versions[0] != game_version:
        check(checks, "resource.packer-game-version", "warning", f"Packing JARs use {found_versions[0]}; the game declares LibGDX {game_version}.",
              remedy="The maintained packer deliberately pins 1.10.0; atlas-preview verifies exact binary hashes and output geometry. See ai-toolkit/docs/graphics-workflow.md.")


def audio_resources(root, config, checks, profile):
    library = config.get("fluidsynth_library") or ctypes.util.find_library("fluidsynth")
    if library and ("/" in library or "\\" in library):
        library = str(project_path(library, root))
    version = None
    try:
        if library:
            handle = ctypes.CDLL(library)
            handle.fluid_version_str.restype = ctypes.c_char_p
            version = handle.fluid_version_str().decode("ascii")
    except (OSError, AttributeError, UnicodeError):
        pass
    check(checks, "tool.fluidsynth", "ok" if version else unavailable(profile, "audio"),
          "FluidSynth shared library available." if version else "FluidSynth shared library unavailable or version API missing.",
          path=library, version=version, remedy="Install libfluidsynth or set ADVENTURE_FLUIDSYNTH_LIBRARY; the CLI executable is not required for library-based rendering.")
    candidates = [project_path(config["soundfont"], root)] if "soundfont" in config else [
        Path("/usr/share/sounds/sf2/TimGM6mb.sf2"), Path("/usr/share/sounds/sf2/FluidR3_GM.sf2"),
    ]
    path = next((p for p in candidates if p.is_file()), None)
    valid = False
    if path:
        with path.open("rb") as stream:
            header = stream.read(12)
        valid = header[:4] == b"RIFF" and header[8:] == b"sfbk"
    check(checks, "resource.soundfont", "ok" if valid else unavailable(profile, "audio"),
          "SoundFont RIFF header found; instruments not audited." if valid else "No readable SoundFont with an SF2 header found.",
          path=str(path) if path else None, remedy="Set ADVENTURE_SOUNDFONT to an SF2 bank for MIDI rendering.")


def doctor(root, config, profile):
    checks = []
    foundation(root, checks)
    if profile == "foundation":
        return checks
    java = None
    if profile in ("all", "ink", "graphics", "spine"):
        candidates = [Path(os.environ["JAVA_HOME"]) / "bin/java"] if os.environ.get("JAVA_HOME") else []
        # JAVA_HOME has the same precedence as Gradle, below explicit project configuration.
        java_config = dict(config)
        if "java" not in java_config and candidates:
            java_config["java"] = str(candidates[0])
        java = tool_check(checks, "java", java_config, root, profile, ("ink", "graphics"),
                          ("java",), ("-version",), r'version "([^\"]+)"')
        if java:
            version = next(c["version"] for c in checks if c["id"] == "tool.java")
            major = re.match(r"(?:1\.)?(\d+)", version)
            if major and int(major.group(1)) < 17:
                check(checks, "tool.java.gradle", "warning", "This Java is too old to run the project's Gradle 9 wrapper.",
                      remedy="Use JDK 21 as documented in README.md and align JAVA_HOME with the diagnostic configuration.")
            javac = java.resolve().with_name("javac.exe" if os.name == "nt" else "javac")
            check(checks, "tool.javac", "ok" if javac.is_file() else "warning", "JDK compiler found." if javac.is_file() else "Java runtime found without an adjacent javac compiler.", path=str(javac))
    if profile in ("all", "ink"):
        metadata = load_project(root)
        if not metadata.get('capabilities', {}).get('ink'):
            check(checks, "tool.ink", "warning", "Ink is not selected for this project.")
        else:
            from ink_tools import compiler_environment
            environment = compiler_environment(root, config)
            if 'inklecate' not in config and environment['compilers'] and environment['runtime'] and environment['java']:
                check(checks, "tool.ink", "ok", "Matching Java Ink compiler and runtime are cached.", version=environment['version'],
                      remedy="Use ink-compile --check to verify the selected story against the actual runtime.")
            else:
                tool_check(checks, "inklecate", config, root, profile, ("ink",), ("inklecate",), (),
                           r"inklecate(?: version)?\s+(\d+\.\d+[^\s]*)", java=java)
                if not environment['runtime']:
                    check(checks, "tool.ink.runtime", unavailable(profile, "ink"), "Matching Ink runtime is not cached.",
                          remedy="Resolve :desktop:automationInkCompilerClasspath explicitly with -I ai-toolkit/tools/automation.gradle, then retry.")
    if profile in ("all", "graphics"):
        tool_check(checks, "inkscape", config, root, profile, ("graphics",), ("inkscape",), ("--version",), r"Inkscape\s+(\S+)")
        tool_check(checks, "magick", config, root, profile, (), ("magick",), ("-version",), r"ImageMagick\s+(\S+)")
        jar_check(root, config, checks, profile)
    if profile in ("all", "spine"):
        tool_check(checks, "spine", config, root, profile, ("spine",), ("Spine", "spine"), (), "",
                   candidates=(Path.home() / "apps/Spine/Spine.sh",))
    if profile in ("all", "audio"):
        for key, flag, pattern in (("ffmpeg", "-version", r"ffmpeg version\s+(\S+)"),
                                   ("ffprobe", "-version", r"ffprobe version\s+(\S+)")):
            tool_check(checks, key, config, root, profile, ("audio",), (key,), (flag,), pattern)
        audio_resources(root, config, checks, profile)
    return checks


# This is an inventory of existing workflows, not a build graph or a guarantee
# that every output has a source. Keep manual/imported assets explicit.
PIPELINES = (
    ("ink", ("raw/ink/*.ink",), ("assets/model/*.ink.json",), ("ai-toolkit/tools/project.py",), "Compile the entrypoint declared in adventure.json; source and compiled output remain distinct."),
    ("graphics", ("raw/art/**/*.svg", "raw/images/**/*.png"), ("assets/images/**/*.png", "assets/atlases/**/*.atlas", "assets/atlases/**/*.png"), ("ai-toolkit/tools/project.py",), "Explicit SVG manifests and atlas groups define exports; external originals use ignored local roots."),
    ("spine", ("raw/art/**/*.spine",), ("assets/spine/*.skel",), ("ai-toolkit/tools/project.py",), "Optional character recipes declare editable sources, versions and export-only or derived workflows."),
    ("sfx", ("raw/sounds/**/*",), ("assets/sounds/*.wav",), ("ai-toolkit/tools/project.py",), "Procedural or imported PCM16 WAV; channel selection is explicit."),
    ("music", ("raw/music/**/*",), ("assets/music/*.mp3",), ("ai-toolkit/tools/project.py",), "Programmatic MIDI composition and final MP3; large intermediates are temporary by default."),
    ("model", ("assets/model/world", "assets/model/*.chapter.json", "assets/model/*.properties"), (), (), "Authored model and localization resources."),
    ("ui", ("assets/ui/*.json", "assets/ui/**/*.ttf", "assets/ui/**/*.txt"), (), (), "Maintained/imported skin, font and attribution resources."),
    ("recordings", ("assets/tests/*.rec",), (), (), "Recordings require explicit compatibility/migration checks; file presence is not playback coverage."),
)


def glob_files(root, patterns):
    return sorted({p.relative_to(root).as_posix() for pattern in patterns for p in root.glob(pattern) if p.is_file()})


def inventory(root):
    groups = {}
    for name in ("raw", "assets"):
        files = sorted(p for p in (root / name).rglob("*") if p.is_file())
        extensions = {}
        for path in files:
            ext = path.suffix or "(none)"
            extensions[ext] = extensions.get(ext, 0) + 1
        groups[name] = {"file_count": len(files), "extensions": dict(sorted(extensions.items()))}
    pipelines = []
    for name, sources, outputs, scripts, note in PIPELINES:
        source_files = glob_files(root, sources)
        if name == "scene-images":
            source_files = [p for p in source_files if not p.startswith("raw/images/ui/")]
        pipelines.append({"id": name, "source_patterns": list(sources), "output_patterns": list(outputs),
                          "sources": source_files, "outputs": glob_files(root, outputs),
                          "scripts": list(scripts), "note": note})
    scripts = glob_files(root, ("*.sh", "raw/**/*.sh", "raw/images/pack", "gradlew", "gradlew.bat", "ai-toolkit/tools/*.py", "*.gradle", "*/build.gradle"))
    return {"groups": groups, "scripts": scripts, "pipelines": pipelines,
            "build_note": "Gradle builds desktop/Android/iOS outputs. core:setVersion rewrites assets/BladeEngine.properties. release.sh changes versions and can publish; diagnostics never run it."}


def inventory_checks(root, external_art=None):
    checks = []
    packall = root / "raw/images/packall.sh"
    if packall.is_file() and "./gen.sh" in packall.read_text() and not (packall.parent / "gen.sh").is_file():
        check(checks, "legacy.packall", "warning", "raw/images/packall.sh calls absent raw/images/gen.sh.",
              remedy="Repair the batch packing workflow in phase 4; do not run it as a regeneration command yet.")
    version = properties(root / "gradle.properties").get("gdxVersion")
    for name in ("pack", "pack-ui.sh"):
        path = root / "raw/images" / name
        text = path.read_text() if path.is_file() else ""
        match = re.search(r"^VERSION=(\S+)", text, re.MULTILINE)
        if match and match.group(1) != version:
            check(checks, "legacy." + name, "warning", f"raw/images/{name} pins LibGDX {match.group(1)}; game declares {version}.",
                  remedy="Version and reproduce the packing toolchain in phase 4; ai-toolkit/tools/local.json does not change legacy scripts.")
    compile_script = root / "raw/ink/compile.sh"
    if compile_script.is_file() and "$HOME/apps/inklecate/inklecate" in compile_script.read_text():
        present = (Path.home() / "apps/inklecate/inklecate").is_file()
        check(checks, "legacy.ink-path", "warning", "raw/ink/compile.sh hardcodes a personal compiler path" + ("." if present else " that does not exist."),
              remedy="Compiler discovery is available via doctor; migrate the compile workflow in phase 3. New path configuration does not alter this script.")
    if glob_files(root, ("assets/spine/*.skel",)) and not glob_files(root, ("raw/**/*.spine",)) and not (external_art or {}).get("spine_projects"):
        check(checks, "sources.spine", "warning", "Runtime skeletons exist without editable .spine projects under raw/.",
              remedy="Configure art_root and run inventory to inspect the external originals. This repository-only finding does not mean those sources are lost.")
    if glob_files(root, ("assets/sounds/*.wav",)) and not glob_files(root, ("raw/sounds/**/*", "raw/sfx/**/*")):
        check(checks, "sources.sfx", "warning", "Runtime WAV files exist without preserved sound sources under raw/sounds/ or raw/sfx/.",
              remedy="Establish source preservation during phase 6; no missing source is fabricated.")
    check(checks, "inventory.scope", "ok", "Inventoried current scripts and resource files; source/output groups are not one-to-one provenance or freshness checks.")
    return checks


def content_checks(root):
    checks = []
    # LibGDX's minimal JSON permits unquoted keys/values and trailing commas.
    # Do not label its existing skin/packer files as broken strict JSON.
    minimal = glob_files(root, ("assets/ui/*.json", "raw/images/**/pack.json"))
    paths = [root / name for name in glob_files(root, ("assets/**/*.json", "raw/**/*.json"))
             if name not in minimal and not name.endswith('.chapter.json')]
    valid = 0

    def reject_constant(value):
        raise ValueError("Nonstandard JSON numeric constant")

    for path in paths:
        try:
            json.loads(path.read_text(encoding="utf-8"), parse_constant=reject_constant)
            valid += 1
        except json.JSONDecodeError as exc:
            check(checks, "content.json", "error", f"Invalid JSON at line {exc.lineno}, column {exc.colno}.",
                  path=path.relative_to(root).as_posix(), remedy="Correct JSON syntax in the source file; generated Ink JSON must be regenerated from Ink sources.")
        except (OSError, UnicodeError):
            check(checks, "content.json", "error", "JSON file is missing or not readable as UTF-8.", path=path.relative_to(root).as_posix())
        except ValueError:
            check(checks, "content.json", "error", "JSON contains a nonstandard numeric constant (NaN or Infinity).",
                  path=path.relative_to(root).as_posix(), remedy="Use a valid finite JSON number.")
    check(checks, "content.json-summary", "ok", f"Read {valid} other strict JSON resource files; chapter/world models are checked separately.")
    if minimal:
        check(checks, "content.libgdx-json", "warning", f"{len(minimal)} LibGDX minimal-JSON files inventoried, not syntax-validated by Python's strict JSON parser.",
              paths=minimal, remedy="Validate these with the matching LibGDX parser when implementing the phase 4 asset tools.")
    return checks


def report(command, root, checks, **extra):
    return {"schema_version": 1, "command": command, "project_root": str(root), "checks": checks,
            "summary": {status: sum(item["status"] == status for item in checks) for status in ("ok", "warning", "error")}, **extra}


def print_report(result, as_json):
    if as_json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return
    print(f"{result['command']}: {result['project_root']}")
    for item in result["checks"]:
        print(f"[{item['status'].upper()}] {item['id']}: {item['message']}")
        if item.get("path"):
            print(f"  Path: {item['path']}")
        if 'json_path' in item:
            print(f"  JSON pointer: {item['json_path'] or '/'}" + (f" (line {item['line']})" if item.get('line') else ''))
        if item.get("version"):
            print(f"  Version: {item['version']}")
        if item.get("remedy") and item["status"] != "ok":
            print(f"  Next: {item['remedy']}")
        if item.get('diagnostics') and item['status'] != 'ok':
            print(item['diagnostics'].rstrip())
    if "inventory" in result:
        data = result["inventory"]
        for name, group in data["groups"].items():
            print(f"{name}/: {group['file_count']} files; extensions: {group['extensions']}")
        print("Scripts: " + ", ".join(data["scripts"]))
        for pipeline in data["pipelines"]:
            print(f"  {pipeline['id']}: {len(pipeline['sources'])} source files -> {len(pipeline['outputs'])} output files. {pipeline['note']}")
        print(data["build_note"])
    if "external_art" in result:
        data = result["external_art"]
        print(f"External art: {data['status']}; {data.get('file_count', 0)} files, {data.get('svg_count', 0)} SVGs, {len(data.get('spine_projects', []))} Spine projects.")
        if data["root"]:
            print("  Root: " + data["root"])
    summary = result["summary"]
    print(f"Result: {summary['ok']} OK, {summary['warning']} warning(s), {summary['error']} error(s).")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    for name in ("doctor", "inventory", "validate", "manifests", "chapters", "ink-compile", "ink-test", "ink-links", "puzzle-test", "art-preview", "art-apply", "atlas-preview", "art-test", "runtime", "spine", "audio", "visual", "qa", "art-root", "skills-check", "skills-sync"):
        sub = subparsers.add_parser(name)
        sub.add_argument("--json", action="store_true", help="Emit a versioned JSON report on stdout.")
        sub.add_argument("--strict", action="store_true", help="Exit 1 for warnings as well as errors.")
        sub.add_argument("--config", help="Optional JSON configuration path, relative to project root or absolute.")
        if name not in ("inventory", "manifests", "chapters", "ink-compile", "ink-test", "ink-links", "puzzle-test", "art-preview", "art-apply", "atlas-preview", "art-test", "runtime", "spine", "audio", "visual", "qa", "art-root", "skills-check", "skills-sync"):
            sub.add_argument("--profile", choices=PROFILES, default="all" if name == "doctor" else "foundation")
        if name == "skills-sync":
            sub.add_argument("--dry-run", action="store_true")
        if name == "art-root":
            sub.add_argument("--path", required=True, help="Existing art root, or a new directory with --seed-starter.")
            sub.add_argument("--seed-starter", action="store_true", help="Copy staged raw/art sources to a new root without overwriting existing art.")
            sub.add_argument("--dry-run", action="store_true")
        if name == "qa":
            sub.add_argument("--profile", default="fast")
            sub.add_argument("--out", required=True, help="New local/CI evidence directory; upload only artifacts/ and root reports.")
            sub.add_argument("--jar", help="Concrete packaged game for native profiles.")
            sub.add_argument("--baseline", help="Explicit reviewed visual baseline directory.")
            sub.add_argument("--allow-unavailable", action="store_true", help="Permit unavailable checks as warnings; result.complete still remains false.")
        if name == "visual":
            sub.add_argument("action", choices=("run", "accept", "compare"))
            sub.add_argument("--spec", required=True, help="Matrix specification (run) or capture result.json (accept/compare).")
            sub.add_argument("--out", required=True, help="New evidence or explicitly reviewed baseline directory.")
            sub.add_argument("--jar", help="Concrete packaged game (run).")
            sub.add_argument("--baseline", help="Reviewed baseline directory (compare).")
            sub.add_argument("--reviewed-by", help="Actual baseline reviewer identity (accept).")
            sub.add_argument("--review-note", help="What was visually reviewed, including masks/tolerances (accept).")
        if name == "audio":
            sub.add_argument("action", choices=("music", "sfx", "test", "normalize-music", "normalize-sfx"))
            sub.add_argument("name", help="Versioned cue or sound-family ID.")
            sub.add_argument("--channels", choices=("source","mono","stereo"), default="source",
                             help="normalize-sfx only: preserve source channels by default, or convert explicitly.")
            sub.add_argument("--keep-intermediates", metavar="NEW_DIRECTORY", help="Explicitly retain WAVs and listening previews in a new directory.")
            sub.add_argument("--out", help="New runtime audio evidence directory (test only).")
            sub.add_argument("--jar", help="Packaged game for the audio runtime test.")
        if name == "spine":
            sub.add_argument("action",choices=("inspect","preview","export","apply","test"))
            sub.add_argument("recipe",help="Spine recipe, or preview report for apply/test.")
            sub.add_argument("--out",help="New preview/evidence directory.")
        if name == "runtime":
            sub.add_argument("action", choices=("start","status","state","events","command","job","cancel","play","capture","evidence","stop","run","prepare-overrides","audit-recordings","migrate-recording","reduce","explore","trace-compare"))
            sub.add_argument("--session", help="Session directory or private discovery JSON.")
            sub.add_argument("--out", help="New isolated session/evidence directory.")
            sub.add_argument("--mode", choices=("player","debug"), default="debug")
            sub.add_argument("--scene")
            sub.add_argument("--chapter", default="00", help="Chapter for an explicitly selected start scene; scene IDs remain literal.")
            sub.add_argument("--spec", help="Override preparation, recording migration or seeded exploration JSON specification.")
            sub.add_argument("--overrides", help="Explicit external persistent content store (start only).")
            sub.add_argument("--hidden-window", action="store_true", help="Start an interactive session with an invisible real GL window (start only).")
            sub.add_argument("--engine-replay", action="store_true", help="Run the scenario in the engine queue, also available from the in-game inspector.")
            sub.add_argument("--max-trials", type=int, default=12, help="Candidate replay budget for reduce (baseline and confirmation are additional).")
            sub.add_argument("--budget-seconds", type=int, default=600, help="Reduction time budget checked between candidate runs.")
            sub.add_argument("--scenario", help="Versioned declarative scenario JSON for runtime run.")
            sub.add_argument("--reference", help="Reference run evidence directory for trace-compare.")
            sub.add_argument("--candidate", help="Candidate run evidence directory for trace-compare.")
            sub.add_argument("--jar", help="Packaged game JAR; otherwise prepare the desktop classpath.")
            sub.add_argument("--path", help="Focused debug-state JSON pointer.")
            sub.add_argument("--operation")
            sub.add_argument("--command-id")
            sub.add_argument("--no-wait", action="store_true", help="Return command acceptance without waiting for gameplay completion.")
            sub.add_argument("--arguments", default="{}", help="Operation argument JSON object.")
            sub.add_argument("--timeout-ms", type=int, default=10000)
            sub.add_argument("--after", type=int, default=0)
            sub.add_argument("--wait-ms", type=int, default=0)
            sub.add_argument("--kind", choices=("ui","scene"), default="ui")
            sub.add_argument("--suite", default="smoke")
        if name == "art-test":
            sub.add_argument("--out", required=True)
        if name == "art-preview":
            sub.add_argument("files", nargs="+")
            sub.add_argument("--out", required=True)
        if name == "art-apply":
            sub.add_argument("report")
        if name == "atlas-preview":
            sub.add_argument("group")
            sub.add_argument("--out", required=True)
        if name == "manifests":
            sub.add_argument("files", nargs="*", help="Manifest JSON paths, default: automation/asset-manifests/*.json.")
            sub.add_argument("--structure-only", action="store_true", help="Validate recipes without reading external SVG sources.")
        if name == 'chapters':
            sub.add_argument('files', nargs='*', help='Chapter paths relative to project root; default: assets/model/*.chapter.json.')
        if name == 'ink-compile':
            sub.add_argument('--source', help='Defaults to adventure.json ink.source.')
            sub.add_argument('--output')
            sub.add_argument('--check', action='store_true', help='Compile/verify freshness without replacing packaged output.')
        if name == 'ink-test':
            sub.add_argument('files', nargs='*', help='Narrative scenario JSON files; defaults to automation/ink-scenarios/*.json.')
        if name == 'puzzle-test':
            sub.add_argument('puzzle', help='Project-declared scenario/suite ID.')
    args = parser.parse_args(argv)
    try:
        config = load_config(ROOT, args.config)
    except ConfigurationError as exc:
        print_report(report(args.command, ROOT, [{"id": "config", "status": "error", "message": str(exc)}]), args.json)
        return 2
    checks = []
    extra = {}
    try:
        profile = getattr(args, "profile", "foundation")
        if args.command == "inventory" or (args.command == "doctor" and profile in ("art", "all")) or (args.command == "validate" and profile in ("art", "all")):
            extra["external_art"], art_checks = inspect_art(config, ROOT, required=profile == "art")
            checks.extend(art_checks)
        if args.command in ("doctor", "validate"):
            checks.extend(doctor(ROOT, config, args.profile))
            extra["profile"] = args.profile
            if args.profile in ('audio','all'):
                from audio_tools import validate_formats
                checks.extend(validate_formats(ROOT))
        if args.command in ("inventory", "validate"):
            extra["inventory"] = inventory(ROOT)
            checks.extend(inventory_checks(ROOT, extra.get("external_art")))
        if args.command == "validate":
            checks.extend(content_checks(ROOT))
            checks.extend(validate_chapters(ROOT))
            checks.extend(validate_manifests(ROOT, config, structure_only=profile != "art"))
            if load_project(ROOT).get('capabilities', {}).get('ink'):
                checks.extend(validate_ink_links(ROOT))
                if profile in ('ink','all'):
                    freshness=compile_ink(ROOT,config,check_only=True)
                    checks.extend(freshness)
                    if not any(c['status']=='error' for c in freshness):checks.extend(test_ink(ROOT,config))
        if args.command == "audio":
            from audio_tools import execute
            audio_checks, extra["audio"] = execute(ROOT, config, args)
            checks.extend(audio_checks)
        if args.command == "spine":
            from spine_tools import execute
            spine_checks,extra["spine"]=execute(ROOT,config,args)
            checks.extend(spine_checks)
        if args.command == "runtime":
            from runtime_tools import execute
            runtime_checks, extra["runtime"] = execute(ROOT, config, args)
            checks.extend(runtime_checks)
        if args.command == "visual":
            from runtime_visual import execute
            visual_checks, extra["visual"] = execute(ROOT, config, args)
            checks.extend(visual_checks)
        if args.command == "skills-check":
            skills_check(ROOT, checks)
        if args.command == "skills-sync":
            from skill_tools import sync_skills
            extra["skills"] = sync_skills(ROOT, args.dry_run)
            conflicts = extra["skills"]["conflicts"]
            check(checks, "skills.sync", "error" if conflicts else "ok", "Discovery conflicts preserved: " + ", ".join(conflicts) if conflicts else "Skill discovery previewed." if args.dry_run else "Skill discovery synchronized.")
        if args.command == "art-root":
            if args.config or os.environ.get("ADVENTURE_TOOLS_CONFIG") or os.environ.get("ADVENTURE_ART_ROOT"):
                raise ConfigurationError("art-root writes ai-toolkit/tools/local.json; unset alternate configuration/art-root environment overrides first.")
            from art_storage import configure_art_root
            extra["art_root"] = configure_art_root(ROOT, args.path, args.seed_starter, args.dry_run)
            check(checks, "art.root", "ok", "Art-root configuration previewed." if args.dry_run else "Local art root configured; portable recipes preserved.")
        if args.command == "qa":
            from qa_tools import execute
            qa_checks, extra["qa"] = execute(ROOT, config, args)
            checks.extend(qa_checks)
        if args.command == "art-test":
            checks.extend(test_art(ROOT, config, args.out))
        if args.command == "art-preview":
            checks.extend(export_preview(ROOT, config, args.files, args.out))
        if args.command == "art-apply":
            checks.extend(apply_preview(ROOT, args.report))
        if args.command == "atlas-preview":
            checks.extend(pack_preview(ROOT, config, args.group, args.out))
        if args.command == "manifests":
            checks.extend(validate_manifests(ROOT, config, args.files, args.structure_only))
        if args.command == 'chapters':
            checks.extend(validate_chapters(ROOT, args.files))
        if args.command == 'ink-compile':
            checks.extend(compile_ink(ROOT, config, args.source, args.output, args.check))
        if args.command == 'ink-test':
            checks.extend(test_ink(ROOT, config, args.files))
        if args.command == 'ink-links':
            checks.extend(validate_ink_links(ROOT))
        if args.command == 'puzzle-test':
            checks.extend(test_puzzle(ROOT, config, args.puzzle))
    except (OSError, UnicodeError, ValueError) as exc:
        check(checks, "inspection.io", "error", f"Inspection could not complete ({type(exc).__name__}): {exc}",
              remedy="Check file permissions and configured paths; inspect retained output before retrying.")
    result = report(args.command, ROOT, checks, **extra)
    print_report(result, args.json)
    return int(bool(result["summary"]["error"] or (args.strict and result["summary"]["warning"])))


if __name__ == "__main__":
    sys.exit(main())

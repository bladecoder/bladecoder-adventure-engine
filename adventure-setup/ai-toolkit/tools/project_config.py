"""Validate portable project declarations separately from local installations."""
import json
import re
from pathlib import Path
from art_sources import relative_path


def toolkit_dir(root):
    root = Path(root)
    modern = root / "ai-toolkit" / "tools"
    return modern if modern.is_dir() or (root / "ai-toolkit").exists() else root / "tools"


def toolkit_path(root, *parts):
    return toolkit_dir(root).joinpath(*parts)


def automation_path(root, *parts):
    root = Path(root)
    base = root / "automation" if (root / "ai-toolkit").exists() else root / "tools"
    return base.joinpath(*parts)


def skills_dir(root):
    root = Path(root)
    modern = root / "ai-toolkit" / "skills"
    return modern if modern.is_dir() or (root / "ai-toolkit").exists() else root / "skills"


def _unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate project declaration key: " + key)
        result[key] = value
    return result


def load_project(root):
    path = Path(root) / "adventure.json"
    data = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=_unique_object)
    if not isinstance(data, dict) or type(data.get("schema_version")) is not int or data["schema_version"] != 1:
        raise ValueError("Use adventure.json schema_version 1.")
    capabilities = data.get("capabilities", {})
    if not isinstance(capabilities, dict) or any(type(value) is not bool for value in capabilities.values()):
        raise ValueError("capabilities must map names to booleans.")
    if data.get("art_storage", "repository") not in ("repository", "external"):
        raise ValueError("art_storage must be repository or external.")
    if "model_contract" in data:
        relative_path(data["model_contract"])
    ink = data.get("ink")
    if ink is not None:
        if not isinstance(ink, dict) or set(ink) != {"source", "output"}:
            raise ValueError("ink must declare source/output paths.")
        for value in ink.values():
            relative_path(value)
        if capabilities.get("ink") is False:
            raise ValueError("Disabled Ink capability cannot declare Ink sources.")
    elif capabilities.get("ink") is True:
        raise ValueError("Enabled Ink capability requires source/output paths.")
    tests = data.get("tests", {})
    if not isinstance(tests, dict):
        raise ValueError("tests must be an object.")
    suites = tests.get("suites", {})
    if not isinstance(suites, dict):
        raise ValueError("tests.suites must be an object.")
    for name, row in suites.items():
        if not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]*", name):
            raise ValueError("Suite names must be portable identifiers.")
        if not isinstance(row, dict) or set(row) - {"scenario", "engine_replay"} or "scenario" not in row:
            raise ValueError("Suite " + name + " must declare a scenario.")
        relative_path(row["scenario"])
        if type(row.get("engine_replay", True)) is not bool:
            raise ValueError("engine_replay must be boolean.")
    audio = tests.get("audio", {})
    if not isinstance(audio, dict):
        raise ValueError("tests.audio must be an object.")
    for name, profile in audio.items():
        if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*", name) or not isinstance(profile, dict):
            raise ValueError("Audio test profiles require portable names and objects.")
        if set(profile) - {"chapter", "scene", "music", "sounds", "capture_seconds", "min_music_wraps", "scenario"} or not isinstance(profile.get("scene"), str) or not profile["scene"]:
            raise ValueError("Audio profile must declare a scene and supported probe settings.")
        relative_path(profile.get("chapter", "00"))
        seconds, wraps = profile.get("capture_seconds", 8), profile.get("min_music_wraps", 0)
        if type(seconds) not in (int, float) or not 2 <= seconds <= 180 or type(wraps) is not int or not 0 <= wraps <= 8:
            raise ValueError("Audio capture duration/wrap count exceeds its bounded range.")
        if profile.get("music") is not None:
            relative_path(profile["music"])
            if not profile["music"].endswith(".mp3"):
                raise ValueError("Music probe resource must be MP3.")
        if "scenario" in profile:
            relative_path(profile["scenario"])
        sounds = profile.get("sounds", [])
        if not isinstance(sounds, list) or len(sounds) > 32 or not (sounds or profile.get("music")):
            raise ValueError("Audio profile must probe music or a bounded SFX list.")
        identifiers = set()
        for sound in sounds:
            if not isinstance(sound, dict) or set(sound) != {"id", "resource"} or not isinstance(sound.get("id"), str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_-]*", sound["id"]) or sound["id"] in identifiers:
                raise ValueError("SFX probes require unique portable IDs and resource paths.")
            identifiers.add(sound["id"]); relative_path(sound["resource"])
            if not sound["resource"].endswith(".wav"):
                raise ValueError("SFX probe resource must be WAV.")
    spine = tests.get("spine", {})
    if not isinstance(spine, dict):
        raise ValueError("tests.spine must be an object.")
    for name, profile in spine.items():
        required = {"scene", "actor", "source", "atlas", "editor_version", "animations"}
        if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*", name) or not isinstance(profile, dict) or not required <= set(profile) or set(profile) - required - {"chapter", "motion_bones"}:
            raise ValueError("Spine profile must declare scene, actor, source, atlas, version and animations.")
        for key in ("scene", "actor"):
            if not isinstance(profile[key], str) or not profile[key]: raise ValueError("Spine scene/actor must be nonempty strings.")
        for key in ("source", "atlas"):
            relative_path(profile[key])
        relative_path(profile.get("chapter", "00"))
        if not isinstance(profile["editor_version"], str) or not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", profile["editor_version"]):
            raise ValueError("Spine profile must pin an exact exported editor version.")
        for key in ("animations", "motion_bones"):
            values = profile.get(key, [])
            if not isinstance(values, list) or len(values) > 64 or any(not isinstance(v, str) or not v for v in values) or len(set(values)) != len(values):
                raise ValueError("Spine profile must use bounded unique name lists.")
        if not profile["animations"]: raise ValueError("Spine profile must select at least one animation.")
    profiles = tests.get("profiles", {})
    if not isinstance(profiles, dict):
        raise ValueError("tests.profiles must be an object.")
    identifiers = set()
    for profile, tasks in profiles.items():
        if profile == "all" or not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]*", profile) or not isinstance(tasks, list):
            raise ValueError("QA profiles must map portable names to task lists; all is reserved.")
        preceding = set()
        for task in tasks:
            if not isinstance(task, dict) or set(task) - {"id", "kind", "value"}:
                raise ValueError("QA tasks accept id, kind and value.")
            identifier, kind, value = task.get("id"), task.get("kind"), task.get("value")
            if not isinstance(identifier, str) or not re.fullmatch(r"[a-zA-Z0-9][a-zA-Z0-9_.-]*", identifier) or identifier in identifiers:
                raise ValueError("QA task IDs must be unique portable identifiers.")
            if kind in ("tests", "formats"):
                if value is not None:
                    raise ValueError("This QA task does not accept a value.")
            elif kind == "cli":
                if value not in ("chapters", "ink-links", "manifests", "inventory"):
                    raise ValueError("QA CLI tasks must select a supported read-only check.")
            elif kind == "spine":
                if not isinstance(value, str) or value not in spine:
                    raise ValueError("QA Spine task must select a declared Spine profile.")
            elif kind == "audio":
                if not isinstance(value, str) or value not in audio:
                    raise ValueError("QA audio task must select a declared audio profile.")
            elif kind == "suite":
                if not isinstance(value, str) or value not in suites:
                    raise ValueError("QA suite task must select a declared suite.")
            elif kind in ("scenario", "matrix", "explore", "reduce"):
                relative_path(value)
            elif kind == "pixels":
                if not isinstance(value, str) or value not in preceding:
                    raise ValueError("Pixel comparison must reference a preceding task in its profile.")
            elif kind == "trace":
                if not isinstance(value, dict) or set(value) != {"reference", "candidate"} or any(not isinstance(v, str) or v not in preceding for v in value.values()):
                    raise ValueError("Trace comparison must reference preceding tasks in its profile.")
            else:
                raise ValueError("Unknown QA task kind.")
            identifiers.add(identifier)
            preceding.add(identifier)
    return data

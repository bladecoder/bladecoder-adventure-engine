"""Version 1 SVG export contract validation. Does not export or pack assets."""

import json
import math
from pathlib import Path
import re
import xml.etree.ElementTree as ET

from art_sources import read_svg, relative_path, roots_for, within


class ContractError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise ContractError(message)


def fields(value, required, optional=()):
    require(isinstance(value, dict), "Expected an object.")
    require(set(required) <= value.keys(), "Missing fields: " + ", ".join(sorted(set(required) - value.keys())))
    require(not value.keys() - set(required) - set(optional), "Unknown fields: " + ", ".join(sorted(value.keys() - set(required) - set(optional))))


def finite(value):
    try:
        return type(value) in (int, float) and math.isfinite(value)
    except OverflowError:
        return False


def name(value):
    return isinstance(value, str) and re.fullmatch(r"[a-z0-9][a-z0-9_-]*", value) is not None


def portable(value):
    try:
        relative_path(value)
    except ValueError as exc:
        raise ContractError(str(exc)) from exc


def strings(values):
    return isinstance(values, list) and all(isinstance(s, str) and s and not any(c.isspace() for c in s) for s in values) and len(set(values)) == len(values)


def json_file(path):
    def pairs(items):
        result = {}
        for key, value in items:
            require(key not in result, "Duplicate JSON member: " + key)
            result[key] = value
        return result

    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=pairs)


def shared_path(root, value):
    portable(value)
    path = (root / value).resolve()
    require(within(path, [(root / "tools/asset-settings").resolve()]), "Shared settings must be under tools/asset-settings/.")
    require(path.suffix == ".json" and path.is_file(), "Shared settings JSON file is missing.")
    return path


def render_settings(value):
    fields(value, ("scale", "background"))
    scale = value["scale"]
    require(isinstance(scale, dict), "scale must be an object.")
    if scale.get("mode") == "dpi":
        fields(scale, ("mode", "value"))
        require(finite(scale["value"]) and scale["value"] > 0, "DPI must be positive and finite.")
    else:
        fields(scale, ("mode", "width", "height"))
        require(scale["mode"] == "pixels", "scale.mode must be dpi or pixels.")
        require(all(type(scale[k]) is int and scale[k] > 0 for k in ("width", "height")), "Pixel dimensions must be positive integers.")
    background = value["background"]
    fields(background, ("color", "opacity"))
    require(isinstance(background["color"], str) and re.fullmatch(r"#[0-9A-Fa-f]{6}", background["color"]), "Use a six-digit background color.")
    require(finite(background["opacity"]) and 0 <= background["opacity"] <= 1, "Background opacity must be between 0 and 1.")


def check_selection(info, selection):
    include, exclude = selection["include"], selection["exclude"]
    identifiers = set(include + exclude)
    require(not identifiers - info["ids"].keys(), "Selection references missing SVG IDs: " + ", ".join(sorted(identifiers - info["ids"].keys())))
    require(not info["duplicate_ids"], "Source has ambiguous duplicate SVG IDs.")
    require(not info["unresolved_ids"], "Source has unresolved local SVG references.")
    require(all(d["status"] == "ok" for d in info["dependencies"]), "Source has missing, unapproved, or remote rendering dependencies; run doctor --profile art.")
    for identifier in include:
        element = info["parents"].get(info["ids"][identifier])
        while element is not None:
            ancestor = element.get("id")
            require(ancestor not in include, "Included IDs overlap through ancestry; select their common export unit explicitly.")
            require(ancestor not in exclude, "An included ID has an excluded ancestor.")
            element = info["parents"].get(element)
    for identifier in include:
        tag = info["ids"][identifier].tag.rsplit("}", 1)[-1]
        require(tag not in {"defs", "metadata", "namedview", "clipPath", "mask", "filter", "linearGradient", "radialGradient", "pattern", "style"},
                "Select a drawable export unit, not a definition or metadata node.")


def validate_one(data, root, config, outputs, manifest_ids, structure_only):
    fields(data, ("schema_version", "id", "source", "exports"), ("settings",))
    require(type(data["schema_version"]) is int and data["schema_version"] == 1, "Unsupported manifest schema_version.")
    require(name(data["id"]) and data["id"] not in manifest_ids, "Manifest ID is invalid or duplicated.")
    manifest_ids.add(data["id"])
    portable(data["source"])
    require(data["source"].lower().endswith(".svg"), "Version 1 sources must be SVG files.")
    require(isinstance(data["exports"], list) and data["exports"], "exports must be a nonempty array.")
    defaults = {}
    if "settings" in data:
        settings = json_file(shared_path(root, data["settings"]))
        fields(settings, ("schema_version", "kind", "render"))
        require(type(settings["schema_version"]) is int and settings["schema_version"] == 1 and settings["kind"] == "svg-export", "Invalid shared SVG settings version/kind.")
        render_settings(settings["render"])
        defaults = settings["render"]
    info = None
    if not structure_only:
        art, linked = roots_for(config, root)
        require(art is not None and art.is_dir(), "Source checking requires a configured, available art_root.")
        source = (art / data["source"]).resolve()
        require(within(source, [art]), "Manifest source escapes art_root.")
        require(source.is_file(), "Manifest SVG source is missing.")
        info = read_svg(source, [art, *linked.values()])
    identifiers = set()
    families = {}
    frames = set()
    variants = set()
    for export in data["exports"]:
        fields(export, ("id", "selection", "crop", "output", "destination"),
               ("render", "reference_point", "family", "frame", "variant", "locale", "nine_patch"))
        require(name(export["id"]) and export["id"] not in identifiers, "Export ID is invalid or duplicated.")
        identifiers.add(export["id"])
        selection = export["selection"]
        fields(selection, ("include", "exclude"))
        require(strings(selection["include"]) and selection["include"], "include must contain unique SVG IDs.")
        require(strings(selection["exclude"]), "exclude must contain unique SVG IDs.")
        require(not set(selection["include"]) & set(selection["exclude"]), "An ID cannot be included and excluded.")
        if info is not None:
            check_selection(info, selection)
        crop = export["crop"]
        require(isinstance(crop, dict), "crop must be an object.")
        if crop.get("mode") == "canvas":
            fields(crop, ("mode", "x", "y", "width", "height"))
            require(all(finite(crop[k]) for k in ("x", "y", "width", "height")) and crop["width"] > 0 and crop["height"] > 0,
                    "Canvas coordinates must be finite and dimensions positive.")
        else:
            fields(crop, ("mode",))
            require(crop["mode"] in ("page", "objects"), "crop.mode must be page, objects, or canvas.")
            if info is not None and crop["mode"] == "page":
                require(info["page_count"] <= 1, "Version 1 page crops require a single-page SVG.")
        overrides = export.get("render", {})
        fields(overrides, (), ("scale", "background"))
        render = {**defaults, **overrides}
        render_settings(render)
        portable(export["output"])
        output = (root / export["output"]).resolve()
        require(output.suffix == ".png", "SVG export outputs must have the .png suffix.")
        destination = export["destination"]
        if isinstance(destination, dict) and destination.get("kind") == "atlas":
            fields(destination, ("kind", "group"), ("pack_settings",))
            require(name(destination["group"]), "Invalid atlas pack group.")
            require(within(output, [(root / "raw/images").resolve()]), "Atlas input outputs must stay under raw/images/.")
            if "pack_settings" in destination:
                json_file(shared_path(root, destination["pack_settings"]))
        elif isinstance(destination, dict) and destination.get("kind") == "spine-piece":
            fields(destination, ("kind", "character"))
            require(name(destination['character']), "Invalid Spine character identifier.")
            require(output.parent == (root/'raw/images'/('spine-'+destination['character'])).resolve(), "Spine pieces must be directly under raw/images/spine-<character>/.")
        else:
            fields(destination, ("kind",))
            require(destination["kind"] == "standalone", "destination.kind must be atlas or standalone.")
            require(within(output, [(root / "assets/images").resolve(), (root / "assets/ui").resolve()]), "Standalone outputs must stay under assets/images/ or assets/ui/.")
        require(within(output, [root.resolve()]), "Output escapes the repository.")
        require(output not in outputs, "Duplicate output destination across manifests or exports.")
        outputs.add(output)
        reference = export.get("reference_point")
        if reference is not None:
            fields(reference, ("x", "y"))
            require(finite(reference["x"]) and finite(reference["y"]), "Reference-point coordinates must be finite.")
        family = export.get("family")
        if family is not None:
            require(name(family) and reference is not None and crop["mode"] in ("page", "canvas"), "Families need an explicit reference_point and a common page/canvas crop.")
            signature = (crop, render["scale"], reference)
            require(family not in families or families[family] == signature, "Family members have inconsistent canvas, scale, or reference point.")
            families[family] = signature
        if "locale" in export:
            require(isinstance(export["locale"], str) and re.fullmatch(r"[a-z]{2,3}(?:-[A-Za-z0-9]+)*", export["locale"]), "Invalid explicit locale.")
        if "frame" in export:
            require(family is not None and type(export["frame"]) is int and export["frame"] >= 0, "frame requires a family and a nonnegative integer.")
            key = (family, export.get("locale"), export["frame"])
            require(key not in frames, "Duplicate frame index within a family/locale.")
            frames.add(key)
        if "variant" in export:
            require(family is not None and name(export["variant"]), "variant requires a family and a logical name.")
            key = (family, export.get("locale"), export["variant"])
            require(key not in variants, "Duplicate variant within a family/locale.")
            variants.add(key)
        if "nine_patch" in export:
            patch = export["nine_patch"]
            fields(patch, ("mode", "split", "padding"))
            require(patch["mode"] == "add-border" and export["output"].endswith(".9.png"), "Nine-patch preparation uses add-border and a .9.png output.")
            require(all(isinstance(patch[k], list) and len(patch[k]) == 4 and all(type(n) is int and n >= 0 for n in patch[k]) for k in ("split", "padding")), "Nine-patch split/padding require four nonnegative integers (left, right, top, bottom).")
            require(render["scale"]["mode"] == "pixels", "Nine-patch geometry needs an explicit pixel size.")
            width, height = render["scale"]["width"], render["scale"]["height"]
            require(all(patch[k][0] + patch[k][1] < width and patch[k][2] + patch[k][3] < height for k in ("split", "padding")), "Nine-patch geometry exceeds the rendered content size.")
        else:
            require(not export["output"].endswith(".9.png"), "A .9.png output requires explicit nine_patch preparation.")
    return len(data["exports"])


def validate_manifests(root, config, filenames=None, structure_only=False):
    paths = [Path(value) if Path(value).is_absolute() else root / value for value in filenames] if filenames else sorted((root / "tools/asset-manifests").glob("*.json"))
    checks = []
    outputs, identifiers = set(), set()
    for path in paths:
        try:
            next_outputs, next_identifiers = outputs.copy(), identifiers.copy()
            count = validate_one(json_file(path), root, config, next_outputs, next_identifiers, structure_only)
            outputs, identifiers = next_outputs, next_identifiers
            checks.append({"id": "manifest.valid", "status": "ok", "message": f"Validated {count} SVG export recipe(s); no images rendered.", "path": str(path)})
        except (OSError, ValueError, ET.ParseError, RuntimeError) as exc:
            checks.append({"id": "manifest.invalid", "status": "error", "message": str(exc) if isinstance(exc, ContractError) else "Cannot read manifest, settings, or SVG source (" + type(exc).__name__ + ").", "path": str(path)})
    if not paths:
        checks.append({"id": "manifest.inventory", "status": "warning", "message": "No SVG export manifests are configured; no source IDs or image outputs were validated."})
    elif structure_only:
        checks.append({"id": "manifest.source-unchecked", "status": "warning", "message": "Only recipe structure, shared settings, and output paths checked; source IDs, linked inputs, and rendering are unverified."})
    return checks

"""Read-only external art inspection. Never renders, rewrites, or fetches resources."""

import os
from pathlib import Path, PurePosixPath, PureWindowsPath
import re
from urllib.parse import unquote, urljoin, urlsplit
import xml.etree.ElementTree as ET

SVG = "http://www.w3.org/2000/svg"
XLINK = "{http://www.w3.org/1999/xlink}href"
XML_BASE = "{http://www.w3.org/XML/1998/namespace}base"
INKSCAPE = "{http://www.inkscape.org/namespaces/inkscape}"


def local_path(value, root):
    path = Path(value).expanduser()
    return (path if path.is_absolute() else root / path).resolve()


def within(path, roots):
    return any(path.is_relative_to(root) for root in roots)


def relative_path(value):
    """Portable recipe paths, never personal paths, URIs, or traversals."""
    if not isinstance(value, str) or not value or "\\" in value or value.startswith("~"):
        raise ValueError("Use a nonempty root-relative POSIX path.")
    path = PurePosixPath(value)
    if path.is_absolute() or PureWindowsPath(value).drive or ".." in path.parts or ":" in value:
        raise ValueError("Absolute paths, URIs, and parent traversal are not allowed in recipes.")
    if path.as_posix() != value or value == ".":
        raise ValueError("Use a normalized root-relative path.")
    return value


def roots_for(config, root):
    art = local_path(config["art_root"], root) if config.get("art_root") else None
    linked = {name: local_path(value, root) for name, value in config.get("linked_roots", {}).items()}
    return art, linked


def read_svg(path, allowed):
    """Retain the original tree; report rendering dependencies without pruning defs."""
    raw = path.read_bytes()
    if b"<!ENTITY" in raw.upper():
        raise ValueError("SVG entity declarations are not supported by inspection.")
    tree = ET.fromstring(raw)
    if tree.tag != "{" + SVG + "}svg":
        raise ValueError("Expected an SVG document.")
    ids = {}
    parents = {}
    duplicates = set()
    for element in tree.iter():
        identifier = element.get("id")
        if identifier:
            if identifier in ids:
                duplicates.add(identifier)
            ids[identifier] = element
        for child in element:
            parents[child] = element
    dependencies = []
    internal = set()
    layers = []
    embedded = 0
    hints = 0
    seen = set()

    def visit(element, base):
        nonlocal embedded, hints
        base = urljoin(base, element.get(XML_BASE, ""))
        if element.get(INKSCAPE + "groupmode") == "layer":
            layers.append({"id": element.get("id"), "label": element.get(INKSCAPE + "label")})
        hints += int(INKSCAPE + "export-filename" in element.attrib)
        values = []
        if element.tag.rsplit("}", 1)[-1] in {"image", "use", "feImage", "linearGradient", "radialGradient", "pattern", "textPath", "mpath"}:
            href = element.get("href", element.get(XLINK))
            if href:
                values.append(href)
        for value in [*element.attrib.values(), element.text if element.tag == "{" + SVG + "}style" else ""]:
            values.extend(re.findall(r"url\(\s*['\"]?([^)'\"]+)['\"]?\s*\)", value or ""))
        for value in values:
            value = value.strip()
            if value.startswith("data:"):
                embedded += 1
                continue
            uri = urlsplit(urljoin(base, value))
            if uri.scheme != "file" or uri.netloc not in ("", "localhost"):
                resolved = value
                status = "unsupported-uri"
            else:
                target = Path(unquote(uri.path)).resolve()
                if target == path.resolve() and uri.fragment:
                    internal.add(unquote(uri.fragment))
                    continue
                resolved = str(target)
                if not within(target, allowed):
                    status = "outside-roots"
                else:
                    status = "ok" if target.is_file() else "missing"
            key = (value, resolved)
            if key not in seen:
                seen.add(key)
                dependencies.append({"href": value, "path": resolved, "status": status})
        for child in element:
            visit(child, base)

    visit(tree, path.resolve().as_uri())
    return {"tree": tree, "ids": ids, "parents": parents, "duplicate_ids": sorted(duplicates),
            "unresolved_ids": sorted(internal - ids.keys()), "dependencies": dependencies,
            "layers": layers, "embedded_images": embedded, "export_hint_count": hints,
            "page_count": sum(element.tag == INKSCAPE + "page" for element in tree.iter()),
            "width": tree.get("width"), "height": tree.get("height"), "viewBox": tree.get("viewBox")}


def inspect_art(config, root, required=False):
    try:
        art, linked = roots_for(config, root)
    except (OSError, RuntimeError, ValueError):
        return {"status": "unavailable", "root": config.get("art_root"), "files": [], "groups": {}, "svgs": [], "linked_roots": {}}, [
            {"id": "art.root", "status": "error" if required else "warning",
             "message": "Cannot resolve a configured source root; check path spelling, permissions, and symlink loops.",
             "remedy": "Correct art_root/linked_roots; packaged-content checks do not require them."}]
    checks = []
    data = {"root": str(art) if art else None, "linked_roots": {k: str(v) for k, v in linked.items()},
            "files": [], "groups": {}, "svgs": []}
    severity = "error" if required else "warning"

    def issue(identifier, message, **details):
        checks.append({"id": identifier, "status": severity, "message": message, **details})

    if art is None or not art.is_dir():
        data["status"] = "not-configured" if art is None else "missing"
        issue("art.root", "External art root is not configured." if art is None else "Configured external art root is unavailable.",
              remedy="Set paths.art_root in ai-toolkit/tools/local.json or ADVENTURE_ART_ROOT. Packaged-content validation does not require it.")
        return data, checks
    data["status"] = "available"
    allowed = [art, *linked.values()]

    def walk_error(error):
        issue("art.read", "Cannot enumerate part of the art tree.", path=str(error.filename))

    for directory, dirs, files in os.walk(art, followlinks=False, onerror=walk_error):
        dirs.sort()
        for name in sorted(dirs[:]):
            path = Path(directory) / name
            if path.is_symlink():
                dirs.remove(name)
                issue("art.symlink", "Directory symlink is not traversed; configure its actual source location explicitly.", path=str(path))
        for name in sorted(files):
            path = Path(directory) / name
            relative = path.relative_to(art).as_posix()
            if not within(path.resolve(), allowed):
                issue("art.path", "Source file resolves outside the configured input roots.", path=relative)
                continue
            try:
                if not path.is_file():
                    issue("art.read", "Source file or symlink target is unavailable.", path=relative)
                    continue
                data["files"].append(relative)
                group = Path(relative).parts[0]
                data["groups"][group] = data["groups"].get(group, 0) + 1
                if path.suffix.lower() != ".svg":
                    continue
                info = read_svg(path, allowed)
                row = {key: value for key, value in info.items() if key not in ("tree", "ids", "parents")}
                row["file"] = relative
                row["id_count"] = len(info["ids"])
                data["svgs"].append(row)
                if info["duplicate_ids"] or info["unresolved_ids"]:
                    issue("art.svg-ids", "SVG has duplicate IDs or unresolved local rendering references: " + ", ".join(info["duplicate_ids"] + info["unresolved_ids"]) + ".", path=relative,
                          duplicate_ids=info["duplicate_ids"], unresolved_ids=info["unresolved_ids"])
                for dependency in info["dependencies"]:
                    if dependency["status"] != "ok":
                        issue("art.link", "SVG rendering dependency (" + dependency["status"] + "): " + dependency["href"] + ".", path=relative,
                              dependency=dependency,
                              remedy="Restore the linked file or configure a named linked_roots location; no remote resources are fetched.")
            except (OSError, ET.ParseError, ValueError, RecursionError) as exc:
                issue("art.svg-read", "Cannot inspect source: " + type(exc).__name__ + ".", path=relative)
    data["files"].sort()
    data["groups"] = dict(sorted(data["groups"].items()))
    data["svgs"].sort(key=lambda row: row["file"])
    data["file_count"] = len(data["files"])
    data["svg_count"] = len(data["svgs"])
    data["spine_projects"] = [p for p in data["files"] if p.lower().endswith(".spine")]
    checks.append({"id": "art.inventory", "status": "ok", "message": f"Inspected {data['file_count']} external files, {data['svg_count']} SVGs, and {len(data['spine_projects'])} Spine projects; rendering and font availability are not verified."})
    return data, checks

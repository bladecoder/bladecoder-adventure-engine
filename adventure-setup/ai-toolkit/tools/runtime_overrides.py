"""Prepare explicit external model/compiled-Ink bundles without changing source or packaged assets."""
import hashlib
import json
from pathlib import Path
import re
from runtime_client import RuntimeFailure, save_json

def resource_path(value):
    if not isinstance(value, str) or '..' in value or (not re.fullmatch('model/[A-Za-z0-9_.-]+\\.(chapter|ink)\\.json', value)):
        raise RuntimeFailure('Only existing chapter/compiled-Ink model resources are supported.')
    return value

def prepare_bundle(root, spec, output):
    root = Path(root).resolve()
    out = Path(output).expanduser().resolve()
    if not isinstance(spec, dict) or set(spec) - {'schema_version', 'id', 'chapter_changes', 'resources'} or type(spec.get('schema_version')) is not int or (spec['schema_version'] != 1):
        raise RuntimeFailure('Invalid version-1 override preparation specification.')
    if not isinstance(spec.get('id'), str) or not re.fullmatch('[a-z0-9][a-z0-9-]{0,63}', spec['id']):
        raise RuntimeFailure('Invalid override id.')
    if out.exists() or any((out.is_relative_to(root / v) for v in ('assets', 'raw', 'tools', 'skills', 'ai-toolkit', 'automation', 'docs'))):
        raise RuntimeFailure('Override output must be new and outside packaged/source/tool directories.')
    changes = spec.get('chapter_changes', [])
    resources = spec.get('resources', [])
    if not isinstance(changes, list) or not isinstance(resources, list) or (not 1 <= len(changes) + len(resources) <= 64):
        raise RuntimeFailure('Declare 1–64 changes/resources.')
    files = {}
    provenance = []

    def original(name):
        path = root / 'assets' / resource_path(name)
        if not path.resolve().is_relative_to(root / 'assets') or not path.is_file() or path.stat().st_size > 16 * 1024 * 1024:
            raise RuntimeFailure('Missing/oversized packaged base: ' + name)
        return path.read_bytes()
    models = {}
    for change in changes:
        if not isinstance(change, dict) or set(change) != {'resource', 'path', 'value'}:
            raise RuntimeFailure('Invalid chapter scalar change.')
        name = resource_path(change['resource'])
        path = change['path']
        if not name.endswith('.chapter.json') or not isinstance(path, str) or (not re.fullmatch('/scenes/[^/]+/actors/[^/]+/(pos/(x|y)|visible|state|desc|scaleX|scaleY|rot|zIndex|walkingSpeed)', path)):
            raise RuntimeFailure('Unsupported chapter override path.')
        if name not in models:
            models[name] = json.loads(original(name))
        target = models[name]
        parts = [v.replace('~1', '/').replace('~0', '~') for v in path[1:].split('/')]
        try:
            for part in parts[:-1]:
                target = target[part]
            before = target[parts[-1]]
        except (KeyError, TypeError):
            raise RuntimeFailure('Chapter scalar path does not exist: ' + path)
        after = change['value']
        if not isinstance(after, (str, int, float, bool, type(None))) or (type(before) is not type(after) and (not (type(before) in (int, float) and type(after) in (int, float)))):
            raise RuntimeFailure('Chapter override must preserve the scalar type.')
        target[parts[-1]] = after
        provenance.append({'resource': name, 'path': path, 'before': before, 'after': after})
    for name, model in models.items():
        files[name] = json.dumps(model, ensure_ascii=False, indent=2, allow_nan=False).encode() + b'\n'
    for entry in resources:
        if not isinstance(entry, dict) or set(entry) != {'path', 'source'}:
            raise RuntimeFailure('Resource replacement requires path/source.')
        if not isinstance(entry['source'], str) or not entry['source']:
            raise RuntimeFailure('Resource source must be a local filename.')
        name = resource_path(entry['path'])
        if not name.endswith('.ink.json') or name in files:
            raise RuntimeFailure('Only unique compiled Ink replacements are supported.')
        original(name)
        source = Path(entry['source']).expanduser()
        source = (source if source.is_absolute() else root/source).resolve()
        if not source.is_file() or source.stat().st_size > 16 * 1024 * 1024:
            raise RuntimeFailure('Missing/oversized compiled Ink replacement.')
        data = source.read_bytes()
        story = json.loads(data)
        if not isinstance(story, dict) or 'inkVersion' not in story:
            raise RuntimeFailure('Replacement must be compiled Ink JSON.')
        files[name] = data
        provenance.append({'resource': name, 'source': str(source), 'source_sha256': hashlib.sha256(data).hexdigest()})
    if sum(map(len, files.values())) > 64 * 1024 * 1024:
        raise RuntimeFailure('Override bundle exceeds 64 MiB.')
    manifest = {'schema_version': 1, 'id': spec['id'], 'files': [{'path': name, 'base_sha256': hashlib.sha256(original(name)).hexdigest(), 'sha256': hashlib.sha256(data).hexdigest()} for name, data in files.items()]}
    out.mkdir(parents=True, exist_ok=False)
    for name, data in files.items():
        target = out / 'files' / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    save_json(out / 'provenance.json', {'changes': provenance, 'scope': 'Prepared bundle only; engine preview/activation and restart verification still required.'})
    save_json(out / 'manifest.json', manifest)
    return {'directory': str(out), 'manifest': manifest, 'boundary': 'Engine preview/activation followed by restart; source assets unchanged.'}

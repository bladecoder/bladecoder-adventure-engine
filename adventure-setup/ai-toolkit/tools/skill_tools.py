"""Canonical project skills, symlink discovery, and legacy-copy synchronization."""
import hashlib
import json
import os
import re
import shutil
import tempfile
from pathlib import Path
from urllib.parse import unquote, urlsplit

from art_sources import relative_path
from project_config import skills_dir, toolkit_path


def digest(data): return hashlib.sha256(data).hexdigest()


def files(folder, root):
    folder = Path(folder)
    if not folder.resolve().is_relative_to(root): raise ValueError('Skill path escapes the project.')
    if folder.is_symlink(): raise ValueError('Nested or external skill symlinks are not supported.')
    result = {}
    if not folder.exists(): return result
    if not folder.is_dir(): raise ValueError('Skill root must be a directory.')
    for path in sorted(folder.rglob('*')):
        if path.is_symlink(): raise ValueError('Skill resources must be regular project files.')
        if path.is_file(): result[path.relative_to(folder).as_posix()] = path.read_bytes()
    return result


def discovery_files(source, canonical='skills'):
    result = {}
    for name, data in source.items():
        parts = name.split('/')
        if len(parts) == 2 and parts[1] == 'SKILL.md':
            text = data.decode('utf-8').replace('\r\n', '\n').replace('\r', '\n')
            end = text.find('\n---', 4)
            if not text.startswith('---\n') or end < 0: raise ValueError('Invalid canonical skill frontmatter: ' + name)
            reference = '../../../' + canonical + '/' + parts[0] + '/SKILL.md'
            body = text[:end + 4] + '\n\nSee the [canonical skill](' + reference + ') for its source and any referenced resources; references resolve relative to that file.\n'
            result[name] = body.encode('utf-8')
        elif len(parts) > 2 and parts[1] in ('agents', 'assets'):
            result[name] = data
    return result


def content_checks(root, source):
    checks = []
    canonical = skills_dir(root)
    canonical_rel = canonical.relative_to(root).as_posix()
    manifests = sorted(name for name in source if name.count('/') == 1 and name.endswith('/SKILL.md'))
    if not manifests: return [{'id': 'skills.sources', 'status': 'error', 'message': 'No skill manifests found under ' + canonical_rel + '/.'}]
    for name in manifests:
        text = source[name].decode('utf-8').replace('\r\n', '\n').replace('\r', '\n')
        folder = name.split('/')[0]
        front = re.match(r'\A---\s*\n(.*?)\n---(?:\n|$)', text, re.S)
        valid = front and re.search(r'(?m)^name:\s*' + '(?:' + re.escape(folder) + '|"' + re.escape(folder) + '"|\'' + re.escape(folder) + '\')' + r'\s*$', front[1]) and re.search(r'(?m)^description:\s*\S', front[1])
        if not re.fullmatch(r'[a-z0-9][a-z0-9-]{0,63}', folder) or not valid:
            checks.append({'id': 'skills.metadata', 'status': 'error', 'message': 'Skill name/folder and required frontmatter must agree.', 'path': canonical_rel + '/' + name})
    for name, data in source.items():
        if not name.endswith('.md'): continue
        text = data.decode('utf-8')
        text = re.sub(r'(?ms)^(```|~~~).*?^\1[^\n]*(?:\n|$)', '', text)
        text = re.sub(r'`[^`\n]+`', '', text)
        for target in re.findall(r'!?\[[^]\n]*\]\(([^)\s]+)\)', text):
            target = target.strip('<>')
            if urlsplit(target).scheme or target.startswith('#'): continue
            selected = unquote(target.split('#', 1)[0])
            path = (canonical / name).parent / selected
            if not path.resolve().is_relative_to(root) or not path.exists():
                checks.append({'id': 'skills.reference', 'status': 'error', 'message': 'Missing or nonportable local reference: ' + target, 'path': canonical_rel + '/' + name})
    if not checks: checks.append({'id': 'skills.content', 'status': 'ok', 'message': str(len(manifests)) + ' skill manifests and local Markdown references checked.'})
    return checks


def check_skills(root):
    root = Path(root).resolve(); canonical = skills_dir(root); source = files(canonical, root)
    checks = content_checks(root, source); target = root / '.agents/skills'
    if target.is_symlink() and target.resolve() == canonical.resolve():
        checks.append({'id': 'skills.discovery', 'status': 'ok', 'message': 'Discovery links directly to canonical project skills.'})
        return checks
    discovered = files(target, root)
    if discovered != discovery_files(source, canonical.relative_to(root).as_posix()) or not source:
        checks.append({'id': 'skills.discovery', 'status': 'error', 'message': 'Discovery copies differ from canonical skills; inspect skills-sync --dry-run.'})
    else:
        checks.append({'id': 'skills.discovery', 'status': 'ok', 'message': 'Portable discovery entries route to canonical project skills.'})
    return checks


def sync_skills(root, dry_run=False):
    root = Path(root).resolve(); canonical_dir = skills_dir(root); canonical = files(canonical_dir, root)
    checks = content_checks(root, canonical)
    if any(row['status'] == 'error' for row in checks): raise ValueError('Correct canonical skill metadata/references before synchronization.')
    target = root / '.agents/skills'; manifest = toolkit_path(root, 'skills-discovery.json')
    if target.is_symlink() and target.resolve() == canonical_dir.resolve():
        return {'schema_version': 1, 'dry_run': dry_run, 'changed': [], 'conflicts': [], 'linked': True}
    if manifest.is_symlink() or not manifest.parent.resolve().is_relative_to(root): raise ValueError('Discovery manifest must stay inside the toolkit tools directory.')
    source = discovery_files(canonical, canonical_dir.relative_to(root).as_posix())
    previous = files(target, root)
    data = json.loads(manifest.read_text()) if manifest.exists() else {'schema_version': 1, 'files': {}}
    if not isinstance(data, dict) or type(data.get('schema_version')) is not int or data['schema_version'] != 1 or not isinstance(data.get('files'), dict): raise ValueError('Invalid skill discovery manifest.')
    baseline = data['files']
    for name, value in baseline.items():
        relative_path(name)
        if not isinstance(value, str) or not re.fullmatch('[0-9a-f]{64}', value): raise ValueError('Invalid skill discovery hash.')
    conflicts = sorted(name for name in previous if previous[name] != source.get(name) and digest(previous[name]) != baseline.get(name))
    changed = sorted(name for name in source.keys() | previous.keys() if source.get(name) != previous.get(name))
    result = {'schema_version': 1, 'dry_run': dry_run, 'changed': changed, 'conflicts': conflicts, 'linked': False}
    if dry_run or conflicts: return result
    manifest.parent.mkdir(parents=True, exist_ok=True)
    parent = target.parent
    if not parent.resolve().is_relative_to(root): raise ValueError('Discovery directory escapes the project.')
    parent.mkdir(parents=True, exist_ok=True)
    staging = Path(tempfile.mkdtemp(prefix='.skills-sync-', dir=parent)); payload = staging / 'payload'; payload.mkdir()
    backup = staging / 'previous'; moved = False; installed = False; completed = False
    try:
        for name, value in source.items():
            path = payload / name; path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(value)
        if files(canonical_dir, root) != canonical or files(target, root) != previous: raise ValueError('Skills changed during synchronization; inspect a fresh preview.')
        if target.exists(): target.rename(backup); moved = True
        payload.rename(target); installed = True
        updated = {'schema_version': 1, 'source': canonical_dir.relative_to(root).as_posix(), 'discovery': '.agents/skills', 'files': {name: digest(value) for name, value in source.items()}}
        with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', dir=manifest.parent, prefix='.skills-', delete=False) as output:
            temporary = Path(output.name); json.dump(updated, output, indent=2); output.write('\n')
        try: temporary.replace(manifest)
        finally: temporary.unlink(missing_ok=True)
        completed = True
    except Exception:
        if installed: shutil.rmtree(target)
        if moved: backup.rename(target)
        raise
    finally:
        if completed or not backup.exists(): shutil.rmtree(staging)
    return result

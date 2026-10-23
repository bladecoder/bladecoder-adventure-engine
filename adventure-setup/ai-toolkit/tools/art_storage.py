"""Explicit local art-root selection; portable recipes never contain machine paths."""
import json
import os
from pathlib import Path
import shutil
import tempfile


def configure_art_root(root, selected, seed=False, dry_run=False):
    root = Path(root).resolve()
    requested = Path(selected).expanduser()
    selected_path = requested if requested.is_absolute() else root / requested
    if seed and selected_path.is_symlink():
        raise ValueError('Seeding cannot replace a symlink art root.')
    destination = selected_path.resolve()
    source = root / 'raw/art'
    local = root / 'tools/local.json'
    if local.is_symlink():
        raise ValueError('Local configuration must be a regular file, not a symlink.')
    data = json.loads(local.read_text(encoding='utf-8')) if local.exists() else {}
    if not isinstance(data, dict) or set(data) - {'paths', 'linked_roots'}:
        raise ValueError('Local configuration accepts only paths and linked_roots.')
    if not isinstance(data.get('paths', {}), dict) or not isinstance(data.get('linked_roots', {}), dict):
        raise ValueError('Local paths and linked_roots must be objects.')
    if destination == root or any(destination.is_relative_to(root / name) for name in ('assets', 'tools', 'skills', '.git', '.agents', 'build')):
        raise ValueError('Art root must not overlap project runtime, tooling or build directories.')
    files = []
    if seed:
        if destination.exists():
            raise ValueError('Seeding requires a new art directory; use an existing root without --seed-starter.')
        if destination.is_relative_to(source) or source.is_relative_to(destination):
            raise ValueError('Starter source and new art root must not overlap.')
        if source.is_symlink() or not source.is_dir():
            raise ValueError('No staged raw/art starter sources are available.')
        for path in sorted(source.rglob('*')):
            if path.is_symlink():
                raise ValueError('Starter art sources must not contain symlinks.')
            if path.is_file():
                files.append(path.relative_to(source))
        if not files:
            raise ValueError('No editable starter art files are available.')
    elif not destination.is_dir():
        raise ValueError('Art directory does not exist; use --seed-starter to initialize a new root.')
    result = {'schema_version': 1, 'dry_run': dry_run, 'root': str(destination),
              'seeded_files': [p.as_posix() for p in files], 'configuration': 'tools/local.json',
              'source_policy': 'No release source archives; raw/art starter sources are retained until explicitly removed.'}
    if dry_run:
        return result
    if seed:
        destination.parent.mkdir(parents=True, exist_ok=True)
        staging = Path(tempfile.mkdtemp(prefix='.adventure-art-', dir=destination.parent))
        try:
            for path in files:
                target = staging / path
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source / path, target)
            # Claim a new destination without replacing even an empty existing directory.
            destination.mkdir()
            for path in files:
                target = destination / path
                target.parent.mkdir(parents=True, exist_ok=True)
                with (staging / path).open('rb') as incoming, target.open('xb') as outgoing:
                    shutil.copyfileobj(incoming, outgoing)
        finally:
            shutil.rmtree(staging)
    data.setdefault('paths', {})['art_root'] = str(destination)
    # Keep personal paths only in ignored local configuration. Do not edit manifests.
    fd, temporary = tempfile.mkstemp(prefix='.local-art-', dir=local.parent)
    try:
        with os.fdopen(fd, 'w', encoding='utf-8') as output:
            json.dump(data, output, indent=2); output.write('\n')
        os.replace(temporary, local)
    except Exception as error:
        raise ValueError('Art sources were preserved, but local configuration could not be saved; configure tools/local.json manually.') from error
    finally:
        if os.path.exists(temporary): os.unlink(temporary)
    return result

"""Reproducible, hash-pinned TexturePacker previews; runtime parsing before application."""
from pathlib import Path
import json
import os
import re
import shutil
import subprocess
import tempfile
from art_pipeline import sha, run, create_output, fingerprints, image_info, program, local_config
from asset_manifest import json_file
from ink_tools import java_program
from project_config import toolkit_path, automation_path


def packer_classpath(root, config):
    lock_path = toolkit_path(root, "asset-settings", "packer-lock.json")
    lock = json_file(lock_path)
    cache = Path(config.get('gradle_cache', os.environ.get('GRADLE_USER_HOME', str(Path.home()/'.gradle')) + '/caches/modules-2/files-2.1')).expanduser()
    if not cache.is_absolute():
        cache = root / cache
    jars = []
    for artifact in lock['jars']:
        configured = config.get('gdx_jar' if artifact['artifact'] == 'gdx' else 'gdx_tools_jar')
        choices = [(root / Path(configured).expanduser()).resolve()] if configured else list(
            (cache / lock['group'] / artifact['artifact'] / artifact['version']).glob('*/*.jar'))
        matching = [p for p in choices if p.is_file() and sha(p) == artifact['sha256']]
        if not matching:
            raise ValueError(f"Pinned {artifact['artifact']} {artifact['version']} unavailable; resolve :desktop:automationPackerClasspath with ai-toolkit/tools/automation.gradle or configure the exact JAR.")
        jars.append(matching[0])
    return jars, lock_path


def pack_preview(root, config, group, output):
    try:
        if not re.fullmatch('[a-zA-Z0-9][a-zA-Z0-9_-]*', group):
            raise ValueError('Invalid atlas group.')
        source = root / 'raw/images' / group
        if not source.is_dir() or source.is_symlink():
            raise ValueError('Pack group must be an existing raw/images subdirectory.')
        out = create_output(output, root)
        settings = toolkit_path(root, "asset-settings", "atlas-ui.json" if group == "ui" else "atlas-scene.json")
        # Production recipe settings are authoritative. A group may span several source SVGs.
        presets = set()
        for path in automation_path(root, "asset-manifests").glob("*.json"):
            for export in json_file(path)['exports']:
                destination = export['destination']
                if destination.get('kind') == 'atlas' and destination['group'] == group and destination.get('pack_settings'):
                    presets.add(destination['pack_settings'])
        if len(presets) > 1:
            raise ValueError('Group has conflicting pack settings across manifests.')
        if presets:
            settings = root / presets.pop()
        jars, lock = packer_classpath(root, config)
        java = java_program(config, root)
        if not java:
            raise ValueError('A JDK 11+ is required for the packer verifier.')
        inputs = sorted(source.glob('*.png'))
        if not inputs or any(p.is_symlink() for p in inputs):
            raise ValueError('Packing needs flat, regular PNG inputs; linked inputs are not accepted.')
        verifier = toolkit_path(root, "java", "AtlasVerifier.java")
        captured = fingerprints([*inputs, settings, lock, verifier, *jars])
        # Copy inputs to avoid legacy pack.json inheritance, nested settings and source mutation.
        with tempfile.TemporaryDirectory(prefix='adventure-pack-') as temporary:
            staging = Path(temporary)
            for path in inputs:
                shutil.copyfile(path, staging / path.name)
            shutil.copyfile(settings, staging / 'pack.json')
            classpath = os.pathsep.join(str(p) for p in jars)
            run([java, '-Djava.awt.headless=true', '-cp', classpath,
                 'com.badlogic.gdx.tools.texturepacker.TexturePacker', staging, out, group])
        atlas = out / (group + '.atlas')
        summary = run([java, '-cp', classpath, verifier, atlas])
        metadata = json.loads(summary.split('ATLAS_DATA=', 1)[1])
        expected = []
        for path in inputs:
            name = path.name[:-4]
            name = name[:-2] if name.endswith('.9') else name
            match = re.fullmatch(r'(.+)_([0-9]+)', name)
            expected.append((match[1], int(match[2])) if match else (name, -1))
        if len(set(expected)) != len(expected):
            raise ValueError('PNG names collapse to duplicate atlas region/index identities.')
        actual = [(r['name'], r['index']) for r in metadata['regions']]
        if sorted(actual) != sorted(expected):
            raise ValueError('Atlas regions/indices differ from the staged PNG set.')
        magick = program(local_config(config, root), 'magick')
        rows = []
        destination = 'assets/ui/1' if group == 'ui' else 'assets/atlases/1'
        for candidate in [atlas, *[out / name for name in metadata['pages']]]:
            target = root / destination / candidate.name
            row = {'output': target.relative_to(root).as_posix(), 'preview': candidate.name,
                   'sha256': sha(candidate), 'baseline': sha(target) if target.exists() else None}
            if candidate.suffix == '.png':
                row['image'] = image_info(magick, candidate)
            rows.append(row)
        if sorted(source.glob('*.png')) != inputs or fingerprints([*inputs, settings, lock, verifier, *jars]) != captured:
            raise ValueError('Pack inputs changed during generation; create a fresh preview.')
        report = {'schema_version': 1, 'kind': 'atlas-preview', 'group': group,
                  'inputs': captured, 'directory_inputs': {str(source.resolve()): [p.name for p in inputs]}, 'files': rows,
                  'atlas': metadata, 'stale_pages': []}
        # Do not silently leave extra runtime pages from a previous multipage pack.
        current = root / destination / (group + '.atlas')
        if current.exists():
            old = json.loads(run([java, '-cp', classpath, verifier, current]).split('ATLAS_DATA=', 1)[1])
            report['stale_pages'] = sorted(set(old['pages']) - set(metadata['pages']))
            if report['stale_pages']:
                raise ValueError('Old atlas pages need explicit cleanup after review: ' + ', '.join(report['stale_pages']))
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
        return [{'id': 'atlas.preview', 'status': 'ok', 'message': f'Packed and parsed {len(actual)} regions using pinned LibGDX 1.10.0; review before art-apply.', 'path': str(out / 'report.json')}]
    except (OSError, ValueError, KeyError, subprocess.SubprocessError) as exc:
        return [{'id': 'atlas.preview', 'status': 'error', 'message': str(exc)}]

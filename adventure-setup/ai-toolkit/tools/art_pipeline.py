"""Explicit SVG rendering, review artifacts and stale-safe application of previews."""
import base64
import hashlib
import json
import mimetypes
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
from urllib.parse import urljoin, urlsplit, unquote
import xml.etree.ElementTree as ET

from art_sources import SVG, XLINK, XML_BASE, read_svg, roots_for, within
from asset_manifest import json_file, validate_one, shared_path

RESOURCE = {'defs', 'metadata', 'namedview', 'style', 'title', 'desc', 'linearGradient',
            'radialGradient', 'clipPath', 'mask', 'filter', 'pattern', 'symbol', 'marker'}


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def run(args, timeout=60, strict_images=False):
    result = subprocess.run([str(a) for a in args], stdin=subprocess.DEVNULL,
                            capture_output=True, text=True, timeout=timeout)
    if strict_images and any(message in result.stderr for message in ('Glycin running without sandbox', 'Failed to load image', 'Could not load image')):
        raise ValueError('Inkscape image decoding is unavailable in this execution environment; use an environment with access to its image loaders.')
    if result.returncode:
        raise ValueError(f'{Path(str(args[0])).name} failed: {(result.stderr or result.stdout)[-3000:]}')
    return result.stdout.strip()


def program(config, key):
    value = config.get(key, key)
    found = shutil.which(value) if '/' not in value else str(Path(value).expanduser())
    if not found or not Path(found).is_file():
        raise ValueError(f'{key} is unavailable; configure paths.{key}.')
    return str(Path(found).absolute())


def local_config(config, root):
    return {key: str((root / Path(value).expanduser()).resolve()) if '/' in value else value
            for key, value in config.items() if key in ('inkscape', 'magick')}


def force_display(element):
    element.set('display', 'inline')
    style = re.sub(r'(?:^|;)\s*display\s*:[^;]*', '', element.get('style', ''), flags=re.I)
    element.set('style', style.strip(';') + ';display:inline')


def compose(source, selection, allowed):
    """Keep ancestors and definitions. Park other drawings in defs, retaining clone IDs."""
    info = read_svg(source, allowed)
    tree = info['tree']
    includes = {info['ids'][i] for i in selection['include']}
    excludes = {info['ids'][i] for i in selection['exclude']}
    ancestors = {tree}
    for node in includes:
        parent = info['parents'].get(node)
        while parent is not None:
            ancestors.add(parent)
            parent = info['parents'].get(parent)
    # Resolve every href before changing the hierarchy or removing xml:base.
    def rebase(node, base):
        base = urljoin(base, node.get(XML_BASE, ''))
        for key in ('href', XLINK):
            href = node.get(key)
            if href and not href.startswith(('#', 'data:')):
                uri = urlsplit(urljoin(base, href))
                if uri.scheme != 'file' or uri.netloc not in ('', 'localhost'):
                    raise ValueError('Only approved local linked inputs can be rendered.')
                path = Path(unquote(uri.path)).resolve()
                if not within(path, allowed) or not path.is_file():
                    raise ValueError('Linked input escapes approved roots or is unavailable.')
                if path.suffix.lower() == '.svg':
                    raise ValueError('External SVG references require an explicitly reviewed self-contained source.')
                if node.tag.rsplit('}', 1)[-1] in ('image', 'feImage'):
                    mime = mimetypes.guess_type(path.name)[0] or 'application/octet-stream'
                    node.set(key, 'data:' + mime + ';base64,' + base64.b64encode(path.read_bytes()).decode('ascii'))
                else:
                    node.set(key, path.as_uri() + ('#' + uri.fragment if uri.fragment else ''))
        # Relative CSS url resources must retain their original base URI too.
        def replace(match):
            href = match[1].strip(' \"\'')
            if href.startswith(('#', 'data:')):
                return match[0]
            uri = urlsplit(urljoin(base, href))
            path = Path(unquote(uri.path)).resolve()
            if uri.scheme != 'file' or not within(path, allowed) or not path.is_file():
                raise ValueError('Unapproved CSS resource.')
            raise ValueError('External CSS url resources require embedding in the editable SVG first.')
        for key, value in list(node.attrib.items()):
            if 'url(' in value:
                node.set(key, re.sub(r'url\(([^)]+)\)', replace, value))
        if node.tag == '{' + SVG + '}style' and node.text:
            if '@import' in node.text:
                raise ValueError('External CSS imports are not supported.')
            node.text = re.sub(r'url\(([^)]+)\)', replace, node.text)
        # Inkscape's SVG 1.1 loader needs xlink even for SVG 2 href inputs.
        if node.get('href') and node.tag.rsplit('}', 1)[-1] in ('image', 'use', 'feImage', 'linearGradient', 'radialGradient', 'pattern', 'textPath', 'mpath'):
            node.set(XLINK, node.get('href'))
        node.attrib.pop(XML_BASE, None)
        for child in node:
            rebase(child, base)
    rebase(tree, source.resolve().as_uri())

    def prune(node, selected=False):
        selected = selected or node in includes
        if node in includes or node in ancestors:
            force_display(node)
        for child in list(node):
            tag = child.tag.rsplit('}', 1)[-1]
            if child in excludes or (not selected and child not in includes and child not in ancestors and tag not in RESOURCE):
                # Definitions do not draw, but remain addressable by <use>, masks, etc.
                wrapper = ET.Element('{' + SVG + '}defs')
                node.insert(list(node).index(child), wrapper)
                node.remove(child)
                wrapper.append(child)
            elif tag not in RESOURCE:
                prune(child, selected)
    prune(tree)
    return tree


def image_info(magick, path):
    values = run([magick, 'identify', '-format', '%w %h %[opaque] %[fx:minima.a] %[fx:maxima.a]', path]).split()
    return dict(width=int(values[0]), height=int(values[1]), opaque=values[2].lower() == 'true',
                alpha_min=float(values[3]), alpha_max=float(values[4]))


def add_nine_patch(magick, path, spec, width, height):
    left, right, top, bottom = spec['split']
    pl, pr, pt, pb = spec['padding']
    draw = (f'line {left+1},0 {width-right},0 line 0,{top+1} 0,{height-bottom} '
            f'line {pl+1},{height+1} {width-pr},{height+1} '
            f'line {width+1},{pt+1} {width+1},{height-pb}')
    run([magick, path, '-bordercolor', 'none', '-border', '1', '-fill', 'black',
         '-stroke', 'black', '-strokewidth', '1', '+antialias', '-draw', draw, 'PNG32:' + str(path)])


def compare(magick, old, new, out):
    if not old.is_file():
        return {'status': 'new'}
    before, after = image_info(magick, old), image_info(magick, new)
    run([magick, old, new, '-background', '#888888', '-alpha', 'remove', '+append', out / 'before-after.png'])
    if (before['width'], before['height']) != (after['width'], after['height']):
        return {'status': 'size-changed', 'before': before}
    result = subprocess.run([magick, 'compare', '-metric', 'AE', str(old), str(new), str(out / 'diff.png')],
                            capture_output=True, text=True, timeout=60)
    if result.returncode not in (0, 1):
        raise ValueError('Image comparison failed: ' + result.stderr[-1000:])
    pixels = int(float(result.stderr.strip().split()[0]))
    return {'status': 'identical' if pixels == 0 else 'changed',
            'different_pixels': pixels, 'before': before}


def fingerprints(paths):
    return {str(Path(p).resolve()): sha(p) for p in paths}


def create_output(out, root):
    out = Path(out).expanduser().resolve()
    if within(out, [(root / 'assets').resolve(), (root / 'raw').resolve()]):
        raise ValueError('Preview directories must be outside raw/ and assets/.')
    out.mkdir(parents=True, exist_ok=False)
    return out


def render_one(inkscape, magick, source, export, render, allowed, output):
    tree = compose(source, export['selection'], allowed)
    crop = export['crop']
    if crop['mode'] == 'canvas':
        # Change only the temporary page, keeping root user coordinates intact.
        tree.set('viewBox', ' '.join(str(crop[k]) for k in ('x', 'y', 'width', 'height')))
        tree.set('width', str(crop['width']))
        tree.set('height', str(crop['height']))
    with tempfile.TemporaryDirectory(prefix='adventure-svg-') as temporary:
        svg = Path(temporary) / 'render.svg'
        ET.ElementTree(tree).write(svg, encoding='utf-8', xml_declaration=True)
        scale, background = render['scale'], render['background']
        args = [inkscape, svg, '--export-type=png', '--export-filename=' + str(output),
                '--export-area-drawing' if crop['mode'] == 'objects' else '--export-area-page',
                '--export-background=' + background['color'], '--export-background-opacity=' + str(background['opacity'])]
        if scale['mode'] == 'dpi':
            args.append('--export-dpi=' + str(scale['value']))
        else:
            # Let the renderer preserve aspect; reject a mismatching requested height.
            args.append('--export-width=' + str(scale['width']))
        run(args, strict_images=True)
    info = image_info(magick, output)
    if render['scale']['mode'] == 'pixels':
        scale = render['scale']
        if (info['width'], info['height']) != (scale['width'], scale['height']):
            raise ValueError(f"Requested pixel aspect does not match renderer: {info}")
    if 'nine_patch' in export:
        add_nine_patch(magick, output, export['nine_patch'], info['width'], info['height'])
        info = image_info(magick, output)
    return info


def export_preview(root, config, files, output):
    try:
        out = create_output(output, root)
        config_tools = local_config(config, root)
        inkscape, magick = program(config_tools, 'inkscape'), program(config_tools, 'magick')
        art, linked = roots_for(config, root)
        allowed = [art, *linked.values()]
        paths, outputs, ids, rows, families = [], set(), set(), [], {}
        captured = {}
        for filename in files:
            path = (root / filename).resolve()
            manifest = json_file(path)
            validate_one(manifest, root, config, outputs, ids, False)
            source = (art / manifest['source']).resolve()
            info = read_svg(source, allowed)
            paths += [path, source] + [Path(d['path']) for d in info['dependencies']]
            defaults = {}
            if 'settings' in manifest:
                settings = shared_path(root, manifest['settings'])
                paths.append(settings)
                defaults = json_file(settings)['render']
            for export in manifest['exports']:
                if 'pack_settings' in export['destination']:
                    paths.append(shared_path(root, export['destination']['pack_settings']))
            for input_path, digest in fingerprints(paths).items():
                if input_path in captured and captured[input_path] != digest:
                    raise ValueError('A shared input changed during rendering; create a fresh preview.')
                captured[input_path] = digest
            for export in manifest['exports']:
                destination = export['destination']
                if destination['kind'] == 'atlas' and Path(export['output']).parent != Path('raw/images') / destination['group']:
                    raise ValueError('Atlas PNGs must be directly under raw/images/<destination.group>/.')
                if 'frame' in export and destination['kind'] == 'atlas' and Path(export['output']).stem != export['family'] + '_' + str(export['frame']):
                    raise ValueError('Atlas frame filenames must be <family>_<zero-based-index>.png.')
                directory = out / manifest['id'] / export['id']
                directory.mkdir(parents=True)
                candidate = directory / Path(export['output']).name
                render = {**defaults, **export.get('render', {})}
                dimensions = render_one(inkscape, magick, source, export, render, allowed, candidate)
                target = root / export['output']
                row = {'output': export['output'], 'preview': candidate.relative_to(out).as_posix(),
                       'sha256': sha(candidate), 'baseline': sha(target) if target.exists() else None,
                       'image': dimensions, 'comparison': compare(magick, target, candidate, directory)}
                rows.append(row)
                if 'frame' in export:
                    key = manifest['id'] + '-' + export['family'] + '-' + export.get('locale', 'all')
                    families.setdefault(key, []).append((export['frame'], candidate))
                if 'pack_settings' in export['destination']:
                    paths.append(shared_path(root, export['destination']['pack_settings']))
        for family, frames in families.items():
            frames.sort()
            if [i for i, _ in frames] != list(range(len(frames))):
                raise ValueError('Animation frames must be contiguous from zero: ' + family)
            run([magick, 'montage', *[p for _, p in frames], '-background', '#888888', '-geometry', '+8+8', '-tile', '4x', out / (family + '.png')])
            run([magick, '-delay', '45', *[p for _, p in frames], '-background', '#888888', '-alpha', 'remove', '-loop', '0', out / (family + '.gif')])
        if fingerprints(paths) != captured:
            raise ValueError('Inputs changed during rendering; create a fresh preview.')
        report = {'schema_version': 1, 'kind': 'svg-preview', 'inputs': captured, 'files': rows,
                  'tools': {'inkscape': run([inkscape, '--version']), 'magick': run([magick, '--version']).splitlines()[0]},
                  'animation_preview': '450 ms/frame, looping; actual chapter timing is checked separately'}
        (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
        return [{'id': 'art.preview', 'status': 'ok', 'message': f'{len(rows)} PNG previews created; inspect comparisons before art-apply.', 'path': str(out / 'report.json')}]
    except (ValueError, OSError, subprocess.SubprocessError, ET.ParseError) as exc:
        return [{'id': 'art.preview', 'status': 'error', 'message': str(exc), 'path': str(output)}]


def apply_preview(root, filename):
    """Review is a caller decision. Hashes prevent stale sources, candidates or targets."""
    report_path = Path(filename).expanduser().resolve()
    try:
        report = json_file(report_path)
        if report.get('schema_version') != 1 or report.get('kind') not in ('svg-preview', 'atlas-preview'):
            raise ValueError('Expected a version 1 art/atlas preview report.')
        for path, digest in report['inputs'].items():
            if not Path(path).is_file() or sha(path) != digest:
                raise ValueError('A preview input changed; render a fresh preview: ' + path)
        for directory, names in report.get('directory_inputs', {}).items():
            if sorted(p.name for p in Path(directory).glob('*.png')) != names:
                raise ValueError('Pack directory members changed; create a fresh preview.')
        replacements = []
        seen = set()
        for row in report['files']:
            target = (root / row['output']).resolve()
            allowed = [root / 'raw/images', root / 'assets/images', root / 'assets/ui'] if report['kind'] == 'svg-preview' else [root / 'assets/atlases/1', root / 'assets/ui/1']
            candidate = (report_path.parent / row['preview']).resolve()
            if not within(target, [p.resolve() for p in allowed]) or target in seen or not within(candidate, [report_path.parent]):
                raise ValueError('Preview output escapes its allowed destination or is duplicated.')
            extensions = ('.png',) if report['kind'] == 'svg-preview' else ('.png', '.atlas')
            if target.suffix not in extensions:
                raise ValueError('Unexpected preview output extension.')
            seen.add(target)
            if sha(candidate) != row['sha256'] or (sha(target) if target.exists() else None) != row['baseline']:
                raise ValueError('Candidate or destination changed since preview: ' + row['output'])
            replacements.append((candidate, target))
        # Validate every input before any writes. Roll back a failed replacement batch.
        with tempfile.TemporaryDirectory(prefix='adventure-apply-') as temporary:
            backup = Path(temporary)
            applied = []
            try:
                for i, (candidate, target) in enumerate(replacements):
                    existed = target.exists()
                    if existed:
                        shutil.copy2(target, backup / str(i))
                    target.parent.mkdir(parents=True, exist_ok=True)
                    applied.append((i, target, existed))
                    with tempfile.NamedTemporaryFile(dir=target.parent, prefix='.' + target.name + '.', suffix='.tmp', delete=False) as staging:
                        sibling = Path(staging.name)
                    try:
                        shutil.copyfile(candidate, sibling)
                        sibling.replace(target)
                    finally:
                        sibling.unlink(missing_ok=True)
            except OSError:
                for i, target, existed in reversed(applied):
                    if existed:
                        shutil.copy2(backup / str(i), target)
                    else:
                        target.unlink(missing_ok=True)
                raise
        return [{'id': 'art.apply', 'status': 'ok', 'message': f'Applied {len(replacements)} reviewed outputs; source art was not modified.'}]
    except (ValueError, KeyError, TypeError, OSError) as exc:
        return [{'id': 'art.apply', 'status': 'error', 'message': str(exc)}]

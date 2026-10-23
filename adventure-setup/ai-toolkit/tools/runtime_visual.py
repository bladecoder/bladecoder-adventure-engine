"""Reviewed visual baselines on the shared runtime; no implicit reference updates."""
from copy import deepcopy
import hashlib
import html
import json
import math
from pathlib import Path
import shutil
import zipfile

from runtime_client import RuntimeFailure, save_json
from runtime_scenario import identifier, run_scenario, validate


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def image_library():
    try:
        from PIL import Image, ImageChops, ImageDraw
    except ImportError as exc:
        raise RuntimeFailure('Visual pixel checks unavailable: install ai-toolkit/tools/requirements-visual.txt in the Python environment running this command.') from exc
    return Image, ImageChops, ImageDraw


def thresholds(value):
    if not isinstance(value, dict) or set(value) != {'channel_delta', 'max_changed_ratio', 'max_mean_error', 'masks'}:
        raise RuntimeFailure('Visual thresholds require channel_delta, max_changed_ratio, max_mean_error and masks.')
    if type(value['channel_delta']) is not int or not 0 <= value['channel_delta'] <= 255:
        raise RuntimeFailure('channel_delta must be an integer in 0–255.')
    for name, limit in [('max_changed_ratio', 1), ('max_mean_error', 255)]:
        v = value[name]
        if type(v) not in (int, float) or not math.isfinite(v) or not 0 <= v <= limit:
            raise RuntimeFailure('Invalid visual threshold: ' + name)
    if not isinstance(value['masks'], list) or len(value['masks']) > 32:
        raise RuntimeFailure('Use at most 32 explicit rectangular masks.')
    for mask in value['masks']:
        if not isinstance(mask, dict) or set(mask) != {'x', 'y', 'width', 'height', 'reason'}:
            raise RuntimeFailure('Each mask needs a rectangle and review reason.')
        if any(type(mask[k]) is not int or mask[k] < (1 if k in ('width', 'height') else 0) for k in ('x', 'y', 'width', 'height')):
            raise RuntimeFailure('Masks use nonnegative pixel origins and positive integer dimensions.')
        if not isinstance(mask['reason'], str) or not mask['reason'].strip():
            raise RuntimeFailure('A mask requires a review reason.')
    return value


def compare_pixels(reference, candidate, limits, diff_path):
    Image, ImageChops, ImageDraw = image_library()
    thresholds(limits)
    with Image.open(reference) as a, Image.open(candidate) as b:
        if a.format != 'PNG' or b.format != 'PNG' or a.size != b.size or a.width * a.height > 8294400:
            raise RuntimeFailure('Compare equally sized PNG images up to 3840×2160 pixels.')
        include = Image.new('L', a.size, 255); draw = ImageDraw.Draw(include)
        for mask in limits['masks']:
            x, y, w, h = (mask[k] for k in ('x', 'y', 'width', 'height'))
            if x + w > a.width or y + h > a.height:
                raise RuntimeFailure('A visual mask extends outside the capture.')
            draw.rectangle((x, y, x + w - 1, y + h - 1), fill=0)
        pixels = include.histogram()[255]
        if pixels < a.width * a.height * .8:
            raise RuntimeFailure('Masks may hide at most 20% of capture pixels.')
        delta = ImageChops.difference(a.convert('RGBA'), b.convert('RGBA'))
        maximum = Image.new('L', a.size, 0)
        error = 0; max_delta = 0
        for channel in delta.split():
            histogram = channel.histogram(mask=include)
            error += sum(i * n for i, n in enumerate(histogram))
            max_delta = max(max_delta, max(i for i, n in enumerate(histogram) if n))
            maximum = ImageChops.lighter(maximum, channel)
        changed = maximum.point([255 if i > limits['channel_delta'] else 0 for i in range(256)])
        changed_pixels = changed.histogram(mask=include)[255]
        mean = error / (pixels * 4)
        ratio = changed_pixels / pixels
        heat = ImageChops.multiply(maximum, include)
        Image.merge('RGB', (heat, Image.new('L', a.size), Image.new('L', a.size))).save(diff_path)
    return {'passed': ratio <= limits['max_changed_ratio'] and mean <= limits['max_mean_error'],
            'compared_pixels': pixels, 'masked_pixels': a.width * a.height - pixels,
            'changed_pixels': changed_pixels, 'changed_ratio': ratio, 'mean_channel_error': mean,
            'max_channel_delta': max_delta, 'thresholds': limits}


def visual_metadata(receipt, font_hashes):
    ui = receipt['state']['ui']
    if not ui.get('render_environment') or not ui.get('fonts'):
        raise RuntimeFailure('Engine capture lacks renderer/font metadata; rebuild with the maintained engine.')
    return {'locale': ui['locale'], 'width': receipt['width'], 'height': receipt['height'],
            'logical_width': ui['width'], 'logical_height': ui['height'], 'kind': receipt['kind'],
            'render_environment': ui['render_environment'], 'fonts': ui['fonts'], 'font_resource_sha256': font_hashes}


def contained(base, name):
    target = (Path(base) / name).resolve()
    if not target.is_relative_to(Path(base).resolve()):
        raise RuntimeFailure('Visual artifact path escapes its evidence directory.')
    return target


def new_output(root, output):
    out = Path(output).expanduser().resolve(); root = Path(root).resolve()
    if out.exists() or any(out.is_relative_to(root / v) for v in ('assets', 'raw', '.git', 'skills', 'ai-toolkit', 'automation', 'docs')):
        raise RuntimeFailure('Visual output must be new and outside source/assets directories.')
    out.mkdir(parents=True)
    return out


def run_matrix(root, config, filename, output, jar):
    root = Path(root).resolve(); filename = Path(filename).resolve()
    spec = json.loads(filename.read_text())
    if not isinstance(spec, dict) or set(spec) != {'schema_version', 'id', 'cases', 'thresholds'} or type(spec['schema_version']) is not int or spec['schema_version'] != 1 or not identifier(spec['id']):
        raise RuntimeFailure('Visual matrix needs schema_version 1, id, cases and thresholds.')
    limits = thresholds(spec['thresholds'])
    if not isinstance(spec['cases'], list) or not 1 <= len(spec['cases']) <= 16:
        raise RuntimeFailure('Use 1–16 explicit scenario files per visual matrix.')
    cases = []; seen = set()
    for name in spec['cases']:
        if not isinstance(name, str): raise RuntimeFailure('Visual cases must be scenario filenames.')
        path = Path(name).expanduser(); path = path if path.is_absolute() else root / path
        scenario = validate(json.loads(path.read_text()))
        if scenario['id'] in seen: raise RuntimeFailure('Visual scenario IDs must be unique.')
        seen.add(scenario['id']); cases.append((path, scenario))
    if not jar: raise RuntimeFailure('Visual matrix requires --jar with one concrete packaged game.')
    jar = Path(jar).resolve(); digest = sha(jar)
    with zipfile.ZipFile(jar) as archive:
        font_hashes = {name: hashlib.sha256(archive.read(name)).hexdigest() for name in sorted(archive.namelist()) if name.startswith('ui/fonts/') and name.endswith(('.ttf', '.fnt', '.otf'))}
    if not font_hashes: raise RuntimeFailure('Packaged font identities unavailable.')
    out = new_output(root, output); save_json(out / 'matrix.json', spec)
    report = {'schema_version': 1, 'id': spec['id'], 'passed': True, 'jar_sha256': digest,
              'thresholds': limits, 'cases': [], 'captures': [], 'errors': []}
    for path, scenario in cases:
        case = scenario['id']
        try:
            if sha(jar) != digest: raise RuntimeFailure('Packaged game changed during matrix execution.')
            _, result = run_scenario(root, config, path, out / case, jar, engine_replay=True)
            report['cases'].append({'id': case, 'passed': result['passed'], 'error': result['error'], 'setup': scenario['setup']})
            if not result['passed']: report['passed'] = False
            for step in scenario['steps']:
                if step['type'] != 'capture': continue
                receipt_path = out / case / (step['id'] + '.json')
                png = out / case / (step['id'] + '.png')
                if not receipt_path.is_file() or not png.is_file():
                    raise RuntimeFailure('Missing declared capture: ' + case + '/' + step['id'])
                receipt = json.loads(receipt_path.read_text())
                if sha(png) != receipt['sha256']: raise RuntimeFailure('Capture receipt hash mismatch.')
                report['captures'].append({'id': case + '/' + step['id'], 'file': png.relative_to(out).as_posix(),
                    'sha256': receipt['sha256'], 'metadata': visual_metadata(receipt, font_hashes)})
        except (RuntimeFailure, OSError, ValueError, KeyError) as exc:
            report['passed'] = False; report['errors'].append(case + ': ' + str(exc))
        save_json(out / 'result.json', report)
    gallery = ['<!doctype html><meta charset="utf-8"><title>Visual matrix</title><style>body{font:16px sans-serif;background:#222;color:#eee}img{max-width:100%}section{margin:2em 0}</style><h1>' + html.escape(spec['id']) + '</h1>']
    for capture in report['captures']:
        gallery.append('<section><h2>' + html.escape(capture['id']) + '</h2><img src="' + html.escape(capture['file'], quote=True) + '"></section>')
    (out / 'gallery.html').write_text('\n'.join(gallery))
    (out / 'report.md').write_text('# Visual matrix\n\n' + ('PASS' if report['passed'] else 'FAIL') + '\n\nSee gallery.html for review and result.json for exact renderer/font metadata.\n')
    return [{'id': 'visual.matrix', 'status': 'ok' if report['passed'] else 'error', 'message': 'Visual capture matrix completed.' if report['passed'] else 'Matrix incomplete; inspect case evidence.', 'path': str(out / 'result.json')}], report


def accept_baseline(root, filename, output, reviewer, note):
    filename = Path(filename).resolve(); source = filename.parent
    report = json.loads(filename.read_text())
    if report.get('passed') is not True or report.get('errors') or not report.get('captures'):
        raise RuntimeFailure('Only a complete passing capture matrix can become a baseline.')
    if not isinstance(reviewer, str) or not reviewer.strip() or not isinstance(note, str) or not note.strip():
        raise RuntimeFailure('Baseline acceptance requires --reviewed-by and --review-note after actual visual review.')
    thresholds(report['thresholds'])
    for capture in report['captures']:
        if sha(contained(source, capture['file'])) != capture['sha256']:
            raise RuntimeFailure('Reviewed capture changed before baseline acceptance.')
    out = new_output(root, output)
    baseline = deepcopy(report)
    baseline.update(review={'reviewed_by': reviewer, 'note': note}, source_matrix_sha256=sha(filename))
    for capture in baseline['captures']:
        target = contained(out, capture['file']); target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(contained(source, capture['file']), target)
    save_json(out / 'baseline.json', baseline)
    return [{'id': 'visual.accept', 'status': 'ok', 'message': 'Explicit reviewed baseline saved.', 'path': str(out / 'baseline.json')}], baseline


def compare_matrix(root, filename, baseline_path, output):
    image_library()  # Fail before creating partial outputs if the optional dependency is absent.
    filename = Path(filename).resolve(); source = filename.parent
    base = Path(baseline_path).resolve(); baseline = json.loads((base / 'baseline.json').read_text())
    report = json.loads(filename.read_text())
    if not baseline.get('review') or not baseline['review'].get('reviewed_by') or not baseline['review'].get('note'):
        raise RuntimeFailure('An explicitly reviewed baseline is required.')
    limits = thresholds(baseline['thresholds'])  # Candidates cannot loosen reviewed thresholds.
    if report.get('passed') is not True or report.get('errors'):
        raise RuntimeFailure('Visual matrix failed; diagnose its original runtime errors first.')
    originals = {v['id']: v for v in baseline['captures']}
    candidates = {v['id']: v for v in report['captures']}
    if len(originals) != len(baseline['captures']) or len(candidates) != len(report['captures']) or originals.keys() != candidates.keys():
        raise RuntimeFailure('Baseline and candidate must have the same unique capture IDs.')
    out = new_output(root, output); rows = []; checks = []
    for index, name in enumerate(sorted(originals)):
        a, b = originals[name], candidates[name]
        reference, candidate = contained(base, a['file']), contained(source, b['file'])
        if sha(reference) != a['sha256'] or sha(candidate) != b['sha256']:
            raise RuntimeFailure('Visual capture hash mismatch: ' + name)
        different = [key for key in a['metadata'] if a['metadata'][key] != b['metadata'].get(key)]
        if different:
            row = {'id': name, 'status': 'incompatible_environment', 'metadata_differences': different}
            status = 'warning'
        else:
            diff_name = 'diff-%02d.png' % index
            metrics = compare_pixels(reference, candidate, limits, out / diff_name)
            row = {'id': name, 'status': 'passed' if metrics['passed'] else 'regression', 'metrics': metrics, 'diff': diff_name}
            status = 'ok' if metrics['passed'] else 'error'
        rows.append(row)
        checks.append({'id': 'visual.compare', 'status': status, 'message': name + ': ' + row['status'], 'path': str(out / 'result.json')})
    result = {'schema_version': 1, 'passed': all(r['status'] == 'passed' for r in rows), 'captures': rows,
              'baseline_review': baseline['review'], 'baseline_sha256': sha(base / 'baseline.json'), 'candidate_sha256': sha(filename),
              'limits': ['Compatible desktop renderer/fonts/locale/size only; no cross-platform visual claim.', 'Masks and thresholds come from the reviewed reference, not the candidate.']}
    save_json(out / 'result.json', result)
    (out / 'report.md').write_text('# Visual comparison\n\n' + '\n'.join('- ' + row['id'] + ': ' + row['status'] for row in rows) + '\n')
    return checks, result


def execute(root, config, args):
    try:
        def path(value):
            p = Path(value).expanduser(); return p if p.is_absolute() else Path(root) / p
        if args.action == 'run': return run_matrix(root, config, path(args.spec), path(args.out), path(args.jar) if args.jar else None)
        if args.action == 'accept': return accept_baseline(root, path(args.spec), path(args.out), args.reviewed_by, args.review_note)
        if not args.baseline: raise RuntimeFailure('visual compare requires --baseline DIRECTORY.')
        return compare_matrix(root, path(args.spec), path(args.baseline), path(args.out))
    except (RuntimeFailure, OSError, ValueError, KeyError) as exc:
        return [{'id': 'visual.' + args.action, 'status': 'error', 'message': str(exc)}], {}

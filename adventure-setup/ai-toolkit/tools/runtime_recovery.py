"""Recover only authenticated-by-hash receipts from this runner's isolated profile."""
import hashlib
import json
from pathlib import Path
import re

from runtime_client import RuntimeFailure, save_json
from runtime_scenario import identifier, RESERVED_IDS

UUID = re.compile(r'[a-f0-9]{8}-(?:[a-f0-9]{4}-){3}[a-f0-9]{12}')


def recover_captures(output, session, scenario):
    """Use exact owned profile/session paths, never search personal saves or elsewhere."""
    out = Path(output).resolve(); report = {'recovered': [], 'errors': []}
    if not session or not (out / 'process.json').is_file(): return report
    process = json.loads((out / 'process.json').read_text())
    profile = (out / 'profile').resolve()
    if profile != out / 'profile' or process.get('owned') is not True or Path(process.get('profile', '/')).resolve() != profile:
        raise RuntimeFailure('Capture recovery requires this runner\'s owned isolated profile.')
    session_id = session.get('session_id', '')
    if not isinstance(session_id, str) or not UUID.fullmatch(session_id):
        raise RuntimeFailure('Capture recovery requires a valid exact session identity.')
    save_root = Path(session.get('save_directory', '/')).resolve()
    if not save_root.is_relative_to(profile):
        raise RuntimeFailure('Recovery cannot inspect a save directory outside the isolated profile.')
    directory = save_root / 'automation' / session_id
    if not directory.resolve().is_relative_to(profile): raise RuntimeFailure('Recovery session path escapes its profile.')
    if not directory.is_dir(): return report
    allowed_steps = {s['id'] for s in scenario['steps'] if s['type'] == 'capture'}
    files = sorted(p for p in directory.iterdir() if p.suffix == '.json' and UUID.fullmatch(p.stem))
    if len(files) > 128: raise RuntimeFailure('Capture recovery receipt limit exceeded.')
    existing_hashes = set()
    for existing in out.glob('*.png'):
        if existing.is_file() and not existing.is_symlink() and existing.stat().st_size <= 64 * 1024 * 1024:
            existing_hashes.add(hashlib.sha256(existing.read_bytes()).hexdigest())
    for receipt_path in files:
        try:
            if receipt_path.is_symlink() or receipt_path.stat().st_size > 16 * 1024 * 1024:
                raise RuntimeFailure('Unsafe/oversized capture receipt.')
            receipt = json.loads(receipt_path.read_text())
            artifact = receipt_path.stem
            png = directory / (artifact + '.png')
            if receipt.get('artifact_id') != artifact or receipt.get('media_type') != 'image/png':
                raise RuntimeFailure('Capture receipt identity/type mismatch.')
            if png.is_symlink() or not png.is_file() or png.stat().st_size > 64 * 1024 * 1024:
                raise RuntimeFailure('Missing/unsafe/oversized capture file.')
            data = png.read_bytes()
            if len(data) != receipt.get('length') or hashlib.sha256(data).hexdigest() != receipt.get('sha256'):
                raise RuntimeFailure('Recovered capture hash/length mismatch.')
            if receipt['sha256'] in existing_hashes:
                continue
            # The capture's own state identifies its active parent/step, not a
            # guessed filename or the last step observed by a slower HTTP poll.
            names = {(row.get('progress') or {}).get('step_id') for row in receipt.get('state', {}).get('scenario_runs', [])
                     if row.get('status') == 'running' and (row.get('progress') or {}).get('id') == scenario['id']}
            names &= allowed_steps
            name = next(iter(names)) if len(names) == 1 else 'recovered-' + artifact
            if not identifier(name) or name in RESERVED_IDS: raise RuntimeFailure('Unsafe recovered capture name.')
            target = out / (name + '.png')
            if target.exists():
                if hashlib.sha256(target.read_bytes()).hexdigest() != receipt['sha256']:
                    raise RuntimeFailure('Recovery would overwrite different existing evidence.')
                continue
            target.write_bytes(data); save_json(out / (name + '.json'), receipt)
            save_json(out / (name + '-state.json'), receipt['state'])
            existing_hashes.add(receipt['sha256'])
            report['recovered'].append({'artifact_id': artifact, 'file': target.name, 'sha256': receipt['sha256'],
                                        'frame': receipt.get('frame'), 'event_cursor': receipt.get('event_cursor')})
        except (RuntimeFailure, OSError, ValueError, KeyError, TypeError, AttributeError) as exc:
            report['errors'].append(receipt_path.name + ': ' + str(exc))
    save_json(out / 'capture-recovery.json', report)
    return report

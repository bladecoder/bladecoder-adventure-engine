"""Bounded diagnostic export, excluding discovery records, profiles and executable fixtures."""
import hashlib
import json
from pathlib import Path

from runtime_client import RuntimeFailure, save_json

TEXT = {'.json', '.jsonl', '.md', '.txt', '.log', '.html'}
MEDIA = {'.png', '.wav', '.mp3'}
EXCLUDED_PARTS = {'profile', '.git', 'store', 'asset-fixture', '__pycache__'}


def export_evidence(source, output, max_bytes=256 * 1024 * 1024, max_files=5000):
    source = Path(source).resolve(); out = Path(output).resolve()
    if not source.is_dir(): raise RuntimeFailure('Artifact source directory is missing.')
    if out.exists() or out.is_relative_to(source) or source.is_relative_to(out):
        raise RuntimeFailure('Artifact export requires separate nonoverlapping source/output directories.')
    if type(max_bytes) is not int or max_bytes < 1 or type(max_files) is not int or max_files < 1:
        raise RuntimeFailure('Artifact budgets must be positive integers.')
    secrets = set()
    # Read only run-created discovery records; never inspect user configuration.
    for path in source.rglob('connection.json'):
        if path.is_symlink() or not path.resolve().is_relative_to(source) or path.stat().st_size > 65536: continue
        try:
            token = json.loads(path.read_text()).get('token')
            if isinstance(token, str) and len(token) >= 8: secrets.add(token.encode())
        except (OSError, ValueError): pass
    out.mkdir(parents=True); entries = []; total = 0; count = 0; incomplete = False
    for path in sorted(source.rglob('*')):
        relative = path.relative_to(source)
        if path.is_dir(): continue
        reason = None
        if path.is_symlink() or not path.resolve().is_relative_to(source): reason = 'symlink_or_escape'
        elif any(part in EXCLUDED_PARTS or part.startswith('.') for part in relative.parts): reason = 'private_profile_or_store'
        elif path.name in ('connection.json', 'process.json', 'session.json'): reason = 'private_connection_or_process'
        elif path.suffix.lower() not in TEXT | MEDIA: reason = 'not_a_diagnostic_format'
        if reason:
            entries.append({'file': relative.as_posix(), 'retained': False, 'reason': reason}); continue
        size = path.stat().st_size
        if total + size > max_bytes or count >= max_files:
            incomplete = True; entries.append({'file': relative.as_posix(), 'retained': False, 'reason': 'budget', 'length': size}); continue
        data = path.read_bytes(); redacted = False
        if path.suffix.lower() in TEXT:
            for token in secrets:
                if token in data: data = data.replace(token, b'[REDACTED_CONTROLLER_TOKEN]'); redacted = True
        if total + len(data) > max_bytes:
            incomplete = True; entries.append({'file': relative.as_posix(), 'retained': False, 'reason': 'budget_after_redaction', 'length': len(data)}); continue
        target = out / relative; target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(data)
        total += len(data); count += 1
        entries.append({'file': relative.as_posix(), 'retained': True, 'length': len(data),
                        'sha256': hashlib.sha256(data).hexdigest(), 'controller_token_redacted': redacted})
    result = {'schema_version': 1, 'complete': not incomplete, 'files': count, 'bytes': total,
              'max_bytes': max_bytes, 'max_files': max_files, 'entries': entries,
              'scope': 'Diagnostic files only. Discovery records, process/session records, profiles, stores, executables and uninspected ZIPs are excluded. Failure fixture manifests/scenarios retain reconstruction details.'}
    save_json(out / 'artifact-index.json', result)
    return result

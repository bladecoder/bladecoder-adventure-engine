"""Authoring client: canonical model writes belong to the engine edit session."""
import json
from pathlib import Path
import uuid
from runtime_client import RuntimeClient, RuntimeFailure, start_runtime, stop_runtime
from runtime_tools import connection


def execute(root, config, args):
    try:
        if args.action == 'start':
            if not args.out:
                raise RuntimeFailure('edit start requires --out NEW_DIRECTORY')
            _, out = start_runtime(root, config, args.out, mode='edit', chapter=args.chapter)
            value = {'connection': str(out / 'connection.json')}
        elif args.action == 'stop':
            value = stop_runtime(connection(args.session).parent)
        else:
            client = RuntimeClient.connect(connection(args.session))
            if client.mode != 'edit':
                raise RuntimeFailure('Requires an explicit edit session; no file-writing fallback is available.')
            session = client.request('/edit/v1/session')
            arguments = json.loads(Path(args.spec).read_text()) if args.spec else json.loads(args.arguments)
            if args.action == 'status':
                value = session
            elif args.action == 'query':
                value = client.request('/edit/v1/query', arguments)
            else:
                if args.action == 'capture' and not args.out:
                    raise RuntimeFailure('capture requires --out FILE.png')
                operation = {'save': 'model.save', 'select': 'view.select', 'capture': 'capture'}.get(args.action, args.operation)
                if not operation:
                    raise RuntimeFailure('edit command requires --operation')
                payload = {'request_id': args.request_id or str(uuid.uuid4()),
                           'expected_revision': args.revision if args.revision is not None else session['model']['revision'],
                           'operation': operation, 'arguments': arguments}
                value = client.request('/edit/v1/commands', payload, timeout=35)
                if args.action == 'capture':
                    if not args.out:
                        raise RuntimeFailure('capture requires --out FILE.png')
                    Path(args.out).write_bytes(client.request('/edit/v1/artifacts/' + value['result']['artifact'], binary=True))
        return [{'id': 'edit.' + args.action, 'status': 'ok', 'message': 'Edit operation completed.'}], value
    except (RuntimeFailure, OSError, ValueError) as exc:
        return [{'id': 'edit.' + args.action, 'status': 'error', 'message': str(exc)}], {}

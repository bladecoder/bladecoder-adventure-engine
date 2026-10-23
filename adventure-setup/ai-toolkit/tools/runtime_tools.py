"""Shared CLI operations; explicit execution only, never part of read-only validation."""
import json
from pathlib import Path
from runtime_client import RuntimeClient, RuntimeFailure, start_runtime, stop_runtime


def connection(value):
    if not value:
        raise RuntimeFailure('--session must name a session directory or discovery file.')
    path=Path(value).expanduser().resolve()
    return path/'connection.json' if path.is_dir() else path


def execute(root, config, args):
    payload={}
    for key in ('out','jar','spec','scenario','session','overrides','reference','candidate'):
        value=getattr(args,key,None)
        if value and not Path(value).expanduser().is_absolute():setattr(args,key,str(Path(root)/Path(value).expanduser()))
    try:
        if args.action=='trace-compare':
            from runtime_trace import compare_traces
            if not args.reference or not args.candidate or not args.out:raise RuntimeFailure('trace-compare requires --reference RUN_DIRECTORY --candidate RUN_DIRECTORY --out NEW_DIRECTORY.')
            return compare_traces(root,args.reference,args.candidate,args.out)
        elif args.action=='explore':
            from runtime_explore import run_exploration
            if not args.spec or not args.out:raise RuntimeFailure('explore requires --spec FILE and --out NEW_DIRECTORY.')
            return run_exploration(root,config,args.spec,args.out,args.jar)
        elif args.action=='reduce':
            from runtime_reduce import reduce_scenario
            if not args.scenario or not args.out:raise RuntimeFailure('reduce requires --scenario FILE and --out NEW_DIRECTORY.')
            return reduce_scenario(root,config,args.scenario,args.out,args.jar,args.max_trials,args.budget_seconds)
        elif args.action=='audit-recordings':
            from runtime_recordings import audit_recordings
            return audit_recordings(root,config)
        elif args.action=='migrate-recording':
            from runtime_recordings import migrate_recording
            if not args.spec or not args.out:raise RuntimeFailure('migrate-recording requires --spec FILE and --out NEW_DIRECTORY.')
            payload=migrate_recording(root,config,json.loads(Path(args.spec).read_text()),args.out)
        elif args.action=='prepare-overrides':
            from runtime_overrides import prepare_bundle
            if not args.spec or not args.out:raise RuntimeFailure('prepare-overrides requires --spec FILE and --out NEW_DIRECTORY.')
            payload=prepare_bundle(root,json.loads(Path(args.spec).expanduser().read_text()),args.out)
        elif args.action=='start':
            if not args.out:raise RuntimeFailure('start requires --out NEW_DIRECTORY.')
            client,out=start_runtime(root,config,args.out,args.mode,args.scene,args.jar,overrides=args.overrides,hidden=args.hidden_window,chapter=args.chapter)
            payload={'health':client.health(),'connection':str(out/'connection.json')}
        elif args.action=='stop':
            payload=stop_runtime(connection(args.session).parent)
        elif args.action=='run':
            from runtime_scenarios import run_suite
            if not args.out:raise RuntimeFailure('run requires --out NEW_DIRECTORY.')
            if args.scenario:
                from runtime_scenario import run_scenario
                return run_scenario(root,config,args.scenario,args.out,args.jar,engine_replay=args.engine_replay)
            if args.engine_replay:raise RuntimeFailure('--engine-replay requires --scenario FILE.')
            return run_suite(root,config,args.suite,args.out,args.jar)
        else:
            client=RuntimeClient.connect(connection(args.session))
            if args.action=='status':payload=client.health()
            elif args.action=='state':payload=client.state(args.path)
            elif args.action=='events':payload=client.events(args.after,args.wait_ms)
            elif args.action=='command':
                if not args.operation:raise RuntimeFailure('command requires --operation.')
                payload=client.command(args.operation,json.loads(args.arguments),args.timeout_ms,wait=not args.no_wait)
            elif args.action in ('job','cancel'):
                if not args.command_id:raise RuntimeFailure('job/cancel requires --command-id.')
                path='/commands/'+args.command_id+('/cancel' if args.action=='cancel' else '')
                payload=client.debug(path,{} if args.action=='cancel' else None)
            elif args.action=='play':payload=client.request('/command',json.loads(args.arguments))
            elif args.action in ('capture','evidence'):
                if not args.out:raise RuntimeFailure('capture/evidence requires --out NEW_DIRECTORY.')
                out=Path(args.out).expanduser().resolve();out.mkdir(parents=True,exist_ok=False)
                payload=client.capture(out,kind=args.kind) if args.action=='capture' else client.evidence(out)
        return [{'id':'runtime.'+args.action,'status':'ok','message':'Runtime '+args.action+' completed.','path':str(args.out) if args.out else str(args.session or '')}],payload
    except (RuntimeFailure,OSError,ValueError) as exc:
        return [{'id':'runtime.'+args.action,'status':'error','message':str(exc)}],payload

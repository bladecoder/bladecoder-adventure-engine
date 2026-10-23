"""Real HTTP integration scenarios, shared state/transcript/capture evidence."""
import json
from pathlib import Path
import threading
import time
from runtime_client import RuntimeClient, RuntimeFailure, save_json, start_runtime, stop_runtime


class EventRecorder:
    def __init__(self, client, out, on_event=None):
        self.client=client;self.out=Path(out);self.stop=threading.Event();self.cursor=0;self.failures=[]
        self.on_event=on_event
        self.thread=threading.Thread(target=self.run,daemon=True);self.thread.start()
    def run(self):
        try:self._record()
        except Exception as exc:self.failures.append('Event recorder failed: '+str(exc))
    def _record(self):
        with (self.out/'events.jsonl').open('w',encoding='utf-8') as writer:
            while not self.stop.is_set():
                try:
                    result=self.client.events(self.cursor,200)
                    if result['lost']:self.failures.append('Event gap before '+str(result['earliest_cursor']))
                    received=time.monotonic()
                    for event in result['events']:
                        writer.write(json.dumps(event,ensure_ascii=False)+'\n')
                        if self.on_event:
                            try:self.on_event(event,received)
                            except Exception as exc:self.failures.append('Event observer: '+str(exc))
                    writer.flush();self.cursor=result['next_cursor']
                except Exception as exc:
                    self.failures.append(str(exc));self.stop.wait(.2)
    def close(self):
        self.stop.set();self.thread.join(6)
        save_json(self.out/'event-recording.json',{'cursor':self.cursor,'errors':self.failures,'thread_stopped':not self.thread.is_alive()})


class Driver:
    def __init__(self, client, out):
        self.client=client;self.out=Path(out);self.assertions=[];self.step_id='setup';self.step_number=0
    def mark(self,kind):
        self.step_number+=1;self.step_id='%03d-%s'%(self.step_number,kind)
    def check(self, condition, label):
        result={'label':label,'passed':bool(condition)}
        if hasattr(self,'step_id'):result['step_id']=self.step_id
        self.assertions.append(result)
        save_json(self.out/'assertions.json',self.assertions)
        if not condition:raise RuntimeFailure('Assertion failed: '+label)
    def state(self):return self.client.state()
    def actor(self,state,name):return next(a for a in state['scene']['actors'] if a['id']==name)
    def items(self,state):return {a['id'] for a in state['inventory']}
    def pump(self, advance=True, frames=1):
        s=self.state()
        if advance and s.get('text'):self.client.command('dialogue.advance')
        self.client.command('clock.step',{'frames':frames,'delta':.05})
        return self.state()
    def until(self, predicate, advance=True, label='condition', frames=1):
        deadline=time.monotonic()+45
        while time.monotonic()<deadline:
            s=self.state()
            if predicate(s):return s
            self.pump(advance,frames)
        raise RuntimeFailure('Scenario watchdog: '+label)
    def idle(self):
        self.until(lambda s:not s['cutMode'] and not s['choices'] and not s.get('text'),label='idle',frames=4)
        self.pump(True,4)
    def verb(self, actor, verb, target=None):
        self.mark('verb-'+actor+'-'+verb)
        before=self.actor(self.state(),actor)
        self.check(any(v['source'].rsplit('/',1)[-1].split('.')[0]==verb for v in before['verbs']), 'verb available '+actor+'/'+verb)
        args={'actor':actor,'verb':verb}
        if target:args['target']=target
        job=self.client.command('verb.run',args,wait=False,timeout_ms=45000)
        deadline=time.monotonic()+45
        while time.monotonic()<deadline:
            status=self.client.debug('/commands/'+job['command_id'])
            if status['status']=='completed':return
            if status['status'] not in ('accepted','running'):raise RuntimeFailure(str(status))
            self.pump(True,2)
        raise RuntimeFailure('Verb watchdog')
    def talk(self, actor):
        self.mark('talk-'+actor)
        self.client.command('verb.run',{'actor':actor,'verb':'talkto'},wait=False,timeout_ms=45000)
        return self.until(lambda s:bool(s['choices']),label='dialogue '+actor,frames=4)
    def choose(self,text):
        self.mark('choose')
        s=self.state();matches=[i for i,c in enumerate(s['choices']) if text in c]
        if len(matches)!=1:raise RuntimeFailure('Expected one choice containing '+text+': '+str(s['choices']))
        return self.client.command('dialogue.choose',{'choice_set':s['choice_set'],'option':matches[0]})
    def checkpoint(self,name):
        self.mark('checkpoint-'+name)
        profile=Path(self.client.debug('/session')['save_directory'])
        self.check(profile.resolve().is_relative_to((self.out/'profile').resolve()),'isolated save path '+name)
        self.client.command('gameplay',{'type':'saveGame','target':name})
        self.check((profile/name).is_file(),'save file created '+name)
        import hashlib
        checkpoint=self.out/'checkpoints';checkpoint.mkdir(exist_ok=True)
        data=(profile/name).read_bytes();(checkpoint/name).write_bytes(data)
        index=checkpoint/'manifest.json';entries=json.loads(index.read_text())['checkpoints'] if index.exists() else []
        state=self.state()
        entries.append({'file':name,'sha256':hashlib.sha256(data).hexdigest(),'length':len(data),'step_id':self.step_id,
            'chapter':state['chapter'],'scene':state['scene']['id'],'frame':state['frame'],'event_cursor':state['event_cursor'],
            'engine_build':self.client.debug('/session')['engine_build'],
            'build':json.loads((self.out/'process.json').read_text())['build']})
        save_json(index,{'schema_version':1,'scope':'Generated only from this run\'s isolated profile. Checkpoints are build-specific test artifacts, not personal saves or release art archives.','checkpoints':entries})
        self.client.command('gameplay',{'type':'loadGame','target':name})
        self.until(lambda s:s['assetState']=='LOADED',label='save/load',frames=1)
        self.client.evidence(self.out,name)


def run_suite(root,config,suite,output,jar=None):
    from project_config import load_project
    from runtime_scenario import run_scenario
    selected=load_project(root).get('tests',{}).get('suites',{}).get(suite)
    if selected is None:
        return [{'id':'runtime.suite','status':'warning','message':'Suite '+suite+' is not configured for this project.'}],{'passed':False,'qa_unavailable':True,'suite':suite}
    return run_scenario(root,config,Path(root)/selected['scenario'],output,jar,engine_replay=selected.get('engine_replay',True))

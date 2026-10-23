#!/usr/bin/env python3
"""Native smoke of a generated package's launcher/controller, using stdlib only.

Creates only owned child processes and isolated user.home profiles. Discovery
records remain private; result.json never contains the controller token.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import socket
import subprocess
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import uuid


def verify(jar, output, playable=False):
    jar=Path(jar).resolve();digest=hashlib.sha256(jar.read_bytes()).hexdigest()
    out=Path(output).resolve();out.mkdir(parents=True,exist_ok=False)
    assertions=[];processes=[];failure=None
    def check(ok,label):
        if not ok:raise AssertionError(label)
        assertions.append(label)
    def request(info,path,body=None,binary=False,token=True):
        headers={'Content-Type':'application/json'}
        if token and info.get('token'):headers['Authorization']='Bearer '+info['token']
        with urlopen(Request(info['url']+path,data=None if body is None else json.dumps(body).encode(),headers=headers),timeout=5) as response:
            value=response.read();return value if binary else json.loads(value)
    def command(info,operation,args=None):
        row=request(info,'/debug/v1/commands',{'request_id':str(uuid.uuid4()),'operation':operation,'arguments':args or {},'timeout_ms':10000})
        end=time.monotonic()+13
        while row['status'] in ('accepted','running','cancelling') and time.monotonic()<end:
            time.sleep(.03);row=request(info,'/debug/v1/commands/'+row['command_id'])
        if row['status']!='completed':
            (folder/'failed-command.json').write_text(json.dumps(row,indent=2))
            raise AssertionError('Command '+operation+': '+json.dumps(row.get('error')))
        check(True,'command completed: '+operation)
        return row['result']
    def start(name,flags,discovery=True):
        folder=out/name;folder.mkdir();profile=folder/'profile';profile.mkdir()
        path=folder/'connection.json'
        cmd=['java','-Duser.home='+str(profile),'-jar',str(jar),'-automation-window',*flags]
        if discovery:cmd+=['-http-info',str(path)]
        with (folder/'process.log').open('wb') as log:
            process=subprocess.Popen(cmd,cwd=folder,stdout=log,stderr=subprocess.STDOUT,stdin=subprocess.DEVNULL)
        processes.append(process)
        if not discovery:return process,folder,None
        end=time.monotonic()+25
        while not path.exists() and process.poll() is None and time.monotonic()<end:time.sleep(.05)
        check(process.poll() is None and path.exists(),'startup/discovery: '+name)
        info=json.loads(path.read_text())
        check(info['url'].startswith('http://127.0.0.1:'),'loopback binding: '+name)
        if os.name=='posix':check(path.stat().st_mode&0o077==0,'private discovery permissions: '+name)
        return process,folder,info
    def stop(process):
        process.terminate()
        try:process.wait(8)
        except subprocess.TimeoutExpired:process.kill();process.wait(5)
    try:
        help_run=subprocess.run(['java','-jar',str(jar),'-h'],capture_output=True,text=True,timeout=10)
        check(help_run.returncode==0 and all(flag in help_run.stdout for flag in ('-http-mode','-http-info','-automation-window','-content-overrides')),'help documents controller options')
        for flags in (['-http','65536'],['-http','no-port'],['-http','-1'],['-http','0'],['-http-mode','debug'],['-http-info','file'],['-http','0','-http-mode','other'],['-http','0','-http-mode','debug','-http-info'],['-content-overrides']):
            invalid=subprocess.run(['java','-jar',str(jar),*flags],capture_output=True,text=True,timeout=10)
            check(invalid.returncode!=0 and 'IllegalArgumentException' in invalid.stderr,'reject invalid configuration: '+' '.join(flags))
        with socket.socket() as selected:
            selected.bind(('127.0.0.1',0));port=selected.getsockname()[1]
        process,folder,info=start('player',['-http',str(port),'-d'])
        check(info['mode']=='player' and 'token' not in info,'logging does not elevate player mode')
        check(request(info,'/health')['mode']=='player','legacy player health')
        request(info,'/state')
        try:request(info,'/debug/v1/state')
        except HTTPError as error:check(error.code==404,'debug endpoint hidden in player mode')
        else:raise AssertionError('player exposed debug state')
        stop(process)
        process,folder,info=start('debug',['-http','0','-http-mode','debug','-content-overrides',str(out/'overrides')])
        check(info['mode']=='debug' and bool(info.get('token')),'explicit debug token and available port')
        try:request(info,'/debug/v1/state',token=False)
        except HTTPError as error:check(error.code==401,'debug state requires token')
        else:raise AssertionError('unauthenticated debug state')
        check(request(info,'/debug/v1/session')['session_id']==info['session_id'],'verified debug session identity')
        command(info,'clock.freeze',{'value':True})
        command(info,'fixture.scene',{'chapter':'00','scene':'scene-1'})
        command(info,'wait',{'conditions':[{'path':'/assetState','equals':'LOADED'}]})
        focused=request(info,'/debug/v1/state?path=%2Fscene%2Fid')
        check(focused['value']=='scene-1','neutral generated scene through focused query')
        state=request(info,'/debug/v1/state');(folder/'state.json').write_text(json.dumps(state,indent=2))
        status=command(info,'content.status');check('content_overrides' in state,'explicit content store visible in full state')
        if playable:
            check(any(actor['id']=='note' for actor in state['scene']['actors']),'playable note actor loaded')
            command(info,'gameplay',{'type':'actorVerb','actorId':'note','verb':'lookat'})
            for _ in range(30):
                command(info,'clock.step',{'frames':4,'delta':.05})
                state=request(info,'/debug/v1/state')
                if state['choices']:break
                if state.get('text'):command(info,'dialogue.advance')
            check('Close the note' in state['choices'],'actual compiled Ink offers the declared choice')
            check(state['ink']['variables']['inspected']['value'] is True,'actual Ink state is typed and changed by interaction')
            command(info,'dialogue.choose',{'choice_set':state['choice_set'],'option':state['choices'].index('Close the note')})
            for _ in range(20):
                command(info,'clock.step',{'frames':4,'delta':.05});state=request(info,'/debug/v1/state')
                if not state.get('text') and not state['choices'] and not state['cutMode']:break
                if state.get('text'):command(info,'dialogue.advance')
            check(not state['cutMode'] and not state['choices'] and not state.get('text'),'starter dialogue returns control')
            command(info,'gameplay',{'type':'actorVerb','actorId':'note','verb':'pickup'})
            command(info,'clock.step',{'frames':4,'delta':.05});state=request(info,'/debug/v1/state')
            check(any(actor['id']=='note' for actor in state['inventory']),'real note acquisition updates inventory')
            session=request(info,'/debug/v1/session');save=Path(session['save_directory'])/'starter-smoke.gamestate'
            check(save.resolve().is_relative_to((folder/'profile').resolve()),'starter checkpoint is isolated')
            command(info,'gameplay',{'type':'saveGame','target':save.name});check(save.is_file(),'starter checkpoint file written')
            command(info,'gameplay',{'type':'loadGame','target':save.name})
            command(info,'wait',{'conditions':[{'path':'/assetState','equals':'LOADED'}]})
            state=request(info,'/debug/v1/state')
            check(any(actor['id']=='note' for actor in state['inventory']) and state['ink']['variables']['inspected']['value'] is True,'inventory and narrative state survive reload')
            check(not state['diagnostics'].get('last_error') and not state['diagnostics'].get('exception'),'playable starter has no runtime diagnostic error')
            (folder/'playable-state.json').write_text(json.dumps(state,indent=2))
        receipt=command(info,'capture',{'kind':'ui'})
        png=request(info,'/debug/v1/artifacts/'+receipt['artifact_id'],binary=True)
        check(hashlib.sha256(png).hexdigest()==receipt['sha256'] and png.startswith(b'\x89PNG'),'rendered capture and receipt hash')
        (folder/'capture.png').write_bytes(png)
        events=request(info,'/debug/v1/events?after=0');(folder/'events.json').write_text(json.dumps(events,indent=2))
        stop(process)
        process,folder,_=start('inactive',['-w','-d'],discovery=False)
        time.sleep(1)
        check(process.poll() is None,'normal package remains running')
        if Path('/proc').is_dir():
            inodes=set()
            for file in Path('/proc',str(process.pid),'fd').iterdir():
                try:target=os.readlink(file)
                except FileNotFoundError:continue
                if target.startswith('socket:['):inodes.add(target[8:-1])
            listening=[]
            for table in ('tcp','tcp6'):
                for line in Path('/proc',str(process.pid),'net',table).read_text().splitlines()[1:]:
                    row=line.split()
                    if row[3]=='0A' and row[9] in inodes:listening.append(row[1])
            check(not listening,'normal package opens no listening sockets')
        stop(process)
    except Exception as error:failure=type(error).__name__+': '+str(error)
    finally:
        for process in processes:
            if process.poll() is None:stop(process)
        result={'schema_version':1,'passed':failure is None,'error':failure,'jar_sha256':digest,'assertions':assertions,'owned_processes_stopped':all(p.poll() is not None for p in processes),'scope':'Actual neutral generated desktop package; no editor GUI or Bonasera content. Mobile and native OS event delivery are not verified.'}
        (out/'result.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--jar',required=True);parser.add_argument('--out',required=True);parser.add_argument('--playable',action='store_true')
    args=parser.parse_args();result=verify(args.jar,args.out,args.playable);print(json.dumps(result,indent=2));raise SystemExit(0 if result['passed'] else 1)

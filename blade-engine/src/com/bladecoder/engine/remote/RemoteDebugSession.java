package com.bladecoder.engine.remote;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.*;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Story;

/** Debug-only session. HTTP threads submit jobs; world access stays on the render thread. */
final class RemoteDebugSession {
    final String id=UUID.randomUUID().toString();
    final String token=UUID.randomUUID().toString()+UUID.randomUUID().toString();
    final RemoteDebugEvents events;
    private final World world;
    private final UI ui;
    private final RemoteCommandExecutor gameplay;
    private final RemoteDebugState state;
    private final Map<String,Job> jobs=new LinkedHashMap<String,Job>();
    private final Map<String,FileHandle> artifacts=new LinkedHashMap<String,FileHandle>();
    private final FileHandle directory;
    private final java.util.concurrent.Semaphore eventPolls=new java.util.concurrent.Semaphore(4);
    private long frame;
    private double simulationTime;
    private boolean frozen;
    private int steps;
    private float stepDelta;
    private volatile boolean open=true;
    private Story observedStory;
    private long sceneGeneration;
    private Scene lastScene;
    private final Story.VariableObserver variables=new Story.VariableObserver() {
        public void call(String name,Object value) {
            if(open)events.add("ink.variable",RemoteDebugState.map("name",name,"value",RemoteDebugState.primitive(value),"type",value==null?"null":value.getClass().getSimpleName()));
        }
    };

    RemoteDebugSession(UI ui,World world,RemoteCommandExecutor gameplay) {
        this.ui=ui;this.world=world;this.gameplay=gameplay;this.state=new RemoteDebugState(world);this.events=new RemoteDebugEvents(world,state);
        directory=EngineAssetManager.getInstance().getUserFile("automation/"+id);
        directory.mkdirs();world.getEvents().addListener(events);EngineLogger.addListener(events);
        events.add("session.started",RemoteDebugState.map("session_id",id,"mode","debug"));
    }
    void close() {
        open=false;world.getEvents().removeListener(events);EngineLogger.removeListener(events);
        if(observedStory!=null) {
            try { observedStory.removeVariableObserver(variables,null); }
            catch(Exception failure) { events.add("observer.error",failure.toString()); }
        }
        synchronized(jobs) {for(Job job:jobs.values())if(!job.terminal())job.fail("session_stopped","Session stopped");}
        events.add("session.stopped",null);
    }
    Map<String,Object> session() {
        return RemoteDebugState.map("protocol",1,"session_id",id,"mode","debug",
                "engine_build",buildIdentity(),
                "engine_location",RemoteControlServer.class.getProtectionDomain().getCodeSource().getLocation().toString(),
                "save_directory",EngineAssetManager.getInstance().getUserFile("").file().getAbsolutePath(),
                "capabilities",Arrays.asList("state.full","state.typed-ink","events.cursor","events.ink","logs","commands.async","commands.cancel",
                        "capture.scene","capture.ui","fixture.scene","fixture.actor","fixture.variables","animation.atlas",
                        "clock.world-step","wait.conditions","save.load","events.actions","events.audio"),
                "limits",RemoteDebugState.map("commands",1024,"events",2048,"artifacts",128,"long_poll_ms",20000,
                        "clock_scope","world/UI delta only; asynchronous IO and platform timers use real time"));
    }
    private Map<String,Object> buildIdentity() {
        java.util.Properties info=new java.util.Properties();
        try(java.io.InputStream stream=RemoteControlServer.class.getResourceAsStream("/blade-engine-build.properties")) {
            if(stream!=null)info.load(stream);
        } catch(java.io.IOException failure) { events.add("identity.error",failure.toString()); }
        return RemoteDebugState.map("version",info.getProperty("engine_version"),"source_sha256",info.getProperty("source_sha256"));
    }
    Map<String,Object> snapshot() throws Exception {
        Map<String,Object> data=state.snapshot();
        data.put("session_id",id);data.put("frame",frame);data.put("event_cursor",events.sequence());
        data.put("scene_generation",sceneGeneration);data.put("simulation_time",simulationTime);
        data.put("clock",RemoteDebugState.map("frozen",frozen,"pending_steps",steps));return data;
    }
    float delta(float normal) {
        float result=frozen?(steps>0?stepDelta:0):normal;
        if(frozen && steps>0)steps--;
        simulationTime+=result;return result;
    }
    void update() {
        frame++;events.frame(frame,simulationTime);
        if(world.getCurrentScene()!=lastScene) {lastScene=world.getCurrentScene();sceneGeneration++;}
        observeInk();
        List<Job> current;
        synchronized(jobs) {current=new ArrayList<Job>(jobs.values());}
        int started=0;
        for(Job job:current) {
            if(job.terminal())continue;
            if(job.cancelRequested) {
                if("clock.step".equals(job.operation) && job.started)steps=0;
                job.result=RemoteDebugState.map("observation_cancelled",true,"gameplay_rolled_back",false);
                job.status="cancelled";events.add("command.cancelled",RemoteDebugState.map("command_id",job.id));continue;
            }
            if(System.nanoTime()>job.deadline) {if("clock.step".equals(job.operation) && job.started)steps=0;job.fail("deadline","Command deadline reached; performed actions are not rolled back");continue;}
            events.command(job.id);
            try {
                if(!job.started && started++<16) {job.started=true;job.startedFrame=frame;job.status="running";events.add("command.started",RemoteDebugState.map("operation",job.operation,"request_id",job.request,"arguments",RemoteDebugState.value(job.arguments)));execute(job);}
                else if(job.started)poll(job);
            } catch(Exception e) {job.fail("command_failed",e.getClass().getSimpleName()+": "+e.getMessage(),e);}
            finally {events.command(null);}
        }
    }
    private void observeInk() {
        Story story=world.getInkManager()==null?null:world.getInkManager().getStory();
        if(story==observedStory)return;
        if(observedStory!=null) {
            try { observedStory.removeVariableObserver(variables,null); }
            catch(Exception failure) { events.add("observer.error",failure.toString()); }
        }
        observedStory=story;
        if(story!=null)try {
            for(String name:story.getVariablesState())story.observeVariable(name,variables);
            events.add("ink.attached",RemoteDebugState.map("story",world.getInkManager().getStoryName()));
        } catch(Exception e) {events.add("observer.error",e.toString());}
    }
    private void poll(Job job) throws Exception {
        if("wait".equals(job.operation)) {
            Map<String,Object> current=snapshot();
            JsonValue conditions=job.arguments.get("conditions");
            boolean ready=true;
            for(JsonValue condition:conditions) {
                Object actual=pointer(current,condition.getString("path"));
                Object expected=RemoteDebugState.value(condition.get("equals"));
                if(!equal(actual,expected)){ready=false;break;}
            }
            if(ready)job.done(RemoteDebugState.map("conditions_met",true));
        } else if("capture".equals(job.operation) && frame>job.startedFrame)capture(job);
        else if("clock.step".equals(job.operation) && steps==0)job.done(RemoteDebugState.map("simulation_time",simulationTime,"frame",frame));
        else if("verb.run".equals(job.operation) && job.verb!=null && job.verb.isFinished())job.done(RemoteDebugState.map("completed",true));
    }
    private static boolean equal(Object actual,Object expected) {
        if(actual instanceof Number && expected instanceof Number)return Double.compare(((Number)actual).doubleValue(),((Number)expected).doubleValue())==0;
        return actual==null?expected==null:actual.equals(expected);
    }
    private static Object pointer(Object data,String path) {
        if(path.isEmpty())return data;
        if(!path.startsWith("/"))throw new IllegalArgumentException("Use JSON pointer paths");
        for(String part:path.substring(1).split("/")) {
            part=part.replace("~1","/").replace("~0","~");
            if(data instanceof Map)data=((Map<?,?>)data).get(part);
            else if(data instanceof List)data=((List<?>)data).get(Integer.parseInt(part));
            else return null;
        }
        return data;
    }
    private void execute(Job job) throws Exception {
        JsonValue a=job.arguments;
        if(a.has("scene_generation") && a.getLong("scene_generation")!=sceneGeneration)throw new IllegalArgumentException("Stale scene generation");
        if("wait".equals(job.operation)) {
            JsonValue conditions=a.require("conditions");
            if(!conditions.isArray() || conditions.size==0 || conditions.size>32)throw new IllegalArgumentException("Use 1-32 conditions");
            for(JsonValue c:conditions) {c.getString("path");c.require("equals");}
            poll(job);return;
        }
        if("capture".equals(job.operation))return; // Capture only after a fresh UI render.
        if("clock.freeze".equals(job.operation)) {if(steps>0)throw new IllegalArgumentException("A frame step is running; wait or cancel it");frozen=a.getBoolean("value");steps=0;job.done(RemoteDebugState.map("frozen",frozen));return;}
        if("clock.step".equals(job.operation)) {
            if(!frozen || steps!=0)throw new IllegalArgumentException("Freeze clock first; another step may be pending");
            int n=a.getInt("frames",1);float dt=a.getFloat("delta",1f/60f);
            if(n<1 || n>600 || !Float.isFinite(dt) || dt<=0 || dt>.1f)throw new IllegalArgumentException("Invalid bounded step");
            steps=n;stepDelta=dt;return;
        }
        if("fixture.scene".equals(job.operation)) {
            world.getSerializer().loadChapter(a.getString("chapter"),a.getString("scene"),false);
            world.getInkManager().newStory(a.getString("chapter"));ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
            events.add("fixture.scene",RemoteDebugState.value(a));job.done(RemoteDebugState.map("loading",true));return;
        }
        if("fixture.variable".equals(job.operation)) {
            String domain=a.getString("domain"),name=a.getString("name");Object value=RemoteDebugState.value(a.require("value"));
            if("ink".equals(domain)) {
                if(value instanceof Long) {
                    long number=((Long)value).longValue();
                    if(number<Integer.MIN_VALUE || number>Integer.MAX_VALUE)throw new IllegalArgumentException("Ink integer is outside 32-bit range");
                    value=Integer.valueOf((int)number);
                } else if(value instanceof Double) {
                    float number=((Double)value).floatValue();
                    if(!Float.isFinite(number))throw new IllegalArgumentException("Ink float must be finite");
                    value=Float.valueOf(number);
                }
                if(world.getInkManager().getStory()==null)throw new IllegalArgumentException("No loaded Ink story");
                world.getInkManager().getStory().getVariablesState().set(name,value);
            } else if("engine".equals(domain)) {
                if(value!=null && !(value instanceof String))throw new IllegalArgumentException("Engine properties are strings or null");
                world.setCustomProperty(name,(String)value);
            } else throw new IllegalArgumentException("Variable domain must be ink or engine");
            events.add("fixture.variable",RemoteDebugState.value(a));job.done(RemoteDebugState.value(a));return;
        }
        if("fixture.actor".equals(job.operation)) {
            BaseActor actor=actor(a);
            if(a.has("visible") && !a.get("visible").isBoolean())throw new IllegalArgumentException("visible must be boolean");
            for(String key:Arrays.asList("x","y","scaleX","scaleY"))if(a.has(key)) {
                JsonValue number=a.get(key);
                if(!(number.isLong() || number.isDouble()) || !Float.isFinite(number.asFloat()))throw new IllegalArgumentException("Actor coordinates/scales must be finite numbers");
            }
            if((a.has("scaleX") || a.has("scaleY")) && !(actor instanceof SpriteActor))throw new IllegalArgumentException("Not a sprite");
            if(a.has("visible"))actor.setVisible(a.getBoolean("visible"));
            if(a.has("x") || a.has("y"))actor.setPosition(a.getFloat("x",actor.getX()),a.getFloat("y",actor.getY()));
            if(a.has("scaleX") || a.has("scaleY")) {
                if(!(actor instanceof SpriteActor))throw new IllegalArgumentException("Not a sprite");
                SpriteActor sprite=(SpriteActor)actor;sprite.setScale(a.getFloat("scaleX",sprite.getScaleX()),a.getFloat("scaleY",sprite.getScaleY()));
            }
            events.add("fixture.actor",RemoteDebugState.value(a));job.done(state.actor(actor));return;
        }
        if("animation.play".equals(job.operation)) {
            BaseActor actor=actor(a);
            if(!(actor instanceof SpriteActor) || !(((SpriteActor)actor).getRenderer() instanceof AnimationRenderer))throw new IllegalArgumentException("No animation renderer");
            AnimationRenderer renderer=(AnimationRenderer)((SpriteActor)actor).getRenderer();String animation=a.getString("animation");
            if(!renderer.hasAnimation(animation))throw new IllegalArgumentException("Animation not found");
            renderer.startAnimation(animation,Tween.Type.SPRITE_DEFINED,-1,null);job.done(state.actor(actor));return;
        }
        if("dialogue.advance".equals(job.operation)) {
            if(world.getCurrentScene()==null || world.getCurrentScene().getTextManager().getCurrentText()==null)throw new IllegalArgumentException("No displayed text");
            world.getCurrentScene().getTextManager().next();job.done(RemoteDebugState.map("advanced",true));return;
        }
        if("dialogue.choose".equals(job.operation)) {
            Map<String,Object> current=snapshot();
            if(a.getLong("choice_set")!=((Number)current.get("choice_set")).longValue())throw new IllegalArgumentException("Stale choice set");
            RemoteCommandResult result=gameplay.execute(RemoteCommand.dialogOption(a.getInt("option")));require(result);
            job.done(RemoteDebugState.map("dispatched",true));return;
        }
        if("verb.run".equals(job.operation)) {
            if(world.inCutMode() || world.isPaused())throw new IllegalArgumentException("Gameplay unavailable while cut/paused");
            BaseActor actor=actor(a);
            if(!(actor instanceof InteractiveActor))throw new IllegalArgumentException("Actor is not interactive");
            String verb=a.getString("verb"),target=a.getString("target",null);
            require(gameplay.execute(RemoteCommand.actorVerb(actor.getId(),verb,target)));
            job.verb=((InteractiveActor)actor).getVerb(verb,target);
            if(job.verb==null || job.verb.isFinished())job.done(RemoteDebugState.map("dispatched",true));return;
        }
        if("gameplay".equals(job.operation)) {
            RemoteCommand command=new RemoteCommandParser().parse(a.toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json));
            if(world.inCutMode() && command.type!=RemoteCommand.Type.PAUSE)throw new IllegalArgumentException("Gameplay unavailable during cut mode");
            if(command.type==RemoteCommand.Type.SAVE_GAME || command.type==RemoteCommand.Type.LOAD_GAME || command.type==RemoteCommand.Type.SCREENSHOT)
                safeName(command.target);
            require(gameplay.execute(command));job.done(RemoteDebugState.map("dispatched",true));return;
        }
        throw new IllegalArgumentException("Unsupported operation: "+job.operation);
    }
    private static void safeName(String name) {
        if(name==null || !name.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,100}") || name.contains(".."))throw new IllegalArgumentException("Use a plain save/artifact basename");
    }
    private static void require(RemoteCommandResult result) {
        if(!result.successful)throw new IllegalArgumentException(result.message);
    }
    private BaseActor actor(JsonValue args) {
        if(world.getCurrentScene()==null)throw new IllegalArgumentException("No scene loaded");
        String expected=args.getString("scene",world.getCurrentScene().getId());
        if(!expected.equals(world.getCurrentScene().getId()))throw new IllegalArgumentException("Stale scene");
        BaseActor actor=world.getCurrentScene().getActor(args.getString("actor"),true);
        if(actor==null)throw new IllegalArgumentException("Actor not found");return actor;
    }
    private void capture(Job job) throws Exception {
        synchronized(artifacts) {if(artifacts.size()>=128)throw new IllegalArgumentException("Artifact limit reached");}
        String kind=job.arguments.getString("kind","ui");String artifact=UUID.randomUUID().toString();
        FileHandle file=directory.child(artifact+".png");
        if("scene".equals(kind)) {
            if(world.getCurrentScene()==null)throw new IllegalArgumentException("No scene loaded");
            world.takeScreenshot("automation/"+id+"/"+artifact+".png",1920);
        } else if("ui".equals(kind)) {
            int w=Gdx.graphics.getBackBufferWidth(),h=Gdx.graphics.getBackBufferHeight();
            if((long)w*h>16000000)throw new IllegalArgumentException("Capture exceeds pixel limit");
            Pixmap image=Pixmap.createFromFrameBuffer(0,0,w,h);
            try {PixmapIO.writePNG(file,image,-1,true);}finally {image.dispose();}
        } else throw new IllegalArgumentException("Capture kind must be ui or scene");
        byte[] bytes=file.readBytes();String hash=hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        synchronized(artifacts) {artifacts.put(artifact,file);}
        Map<String,Object> result=RemoteDebugState.map("artifact_id",artifact,"media_type","image/png","length",bytes.length,
                "sha256",hash,"width","scene".equals(kind)?1920:Gdx.graphics.getBackBufferWidth(),
                "height","scene".equals(kind)?(int)(1920*world.getCurrentScene().getCamera().viewportHeight/world.getCurrentScene().getCamera().viewportWidth):Gdx.graphics.getBackBufferHeight(),"frame",frame,"event_cursor",events.sequence(),"kind",kind,"state",snapshot());
        events.add("capture",RemoteDebugState.map("artifact_id",artifact,"kind",kind));job.done(result);
    }
    static String hex(byte[] bytes) {StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format("%02x",b&255));return result.toString();}

    RemoteHttpResponse route(RemoteHttpRequest request) throws Exception {
        if(!("Bearer "+token).equals(request.headers.get("authorization")))return error(401,"unauthorized","Debug token required");
        String path=request.path.substring("/debug/v1".length());
        if("GET".equals(request.method) && "/session".equals(path))return response(200,session());
        if("GET".equals(request.method) && "/state".equals(path)) {
            final Object[] result=new Object[1];final Exception[] failure=new Exception[1];final CountDownLatch ready=new CountDownLatch(1);
            Gdx.app.postRunnable(new Runnable(){public void run(){try{result[0]=snapshot();}catch(Exception e){failure[0]=e;}finally{ready.countDown();}}});
            if(!ready.await(2,TimeUnit.SECONDS))return error(503,"unresponsive","Render thread did not answer");
            if(failure[0]!=null)throw failure[0];
            String pointer=request.query.get("path");
            return response(200,pointer==null?result[0]:RemoteDebugState.map("path",pointer,"value",pointer(result[0],pointer)));
        }
        if("GET".equals(request.method) && "/events".equals(path)) {
            long after=Long.parseLong(request.query.containsKey("after")?request.query.get("after"):"0");
            int wait=Integer.parseInt(request.query.containsKey("wait_ms")?request.query.get("wait_ms"):"0");
            if(after<0 || wait<0 || wait>20000)return error(400,"invalid_cursor","Invalid cursor/wait");
            if(after>events.sequence())return error(409,"future_cursor","Cursor belongs to another session or future state");
            if(!eventPolls.tryAcquire())return error(429,"poll_limit","Too many event observers");
            try { return response(200,events.read(after,wait)); }
            finally { eventPolls.release(); }
        }
        if("POST".equals(request.method) && "/commands".equals(path)) {
            JsonValue body;
            try { body=new JsonReader().parse(request.body); }
            catch(RuntimeException invalid) { return error(400,"invalid_json","Expected a JSON command object"); }
            if(body==null || !body.isObject() || body.get("request_id")==null || !body.get("request_id").isString()
                    || body.get("operation")==null || !body.get("operation").isString())
                return error(400,"invalid_command","request_id and operation must be strings");
            for(JsonValue field:body)if(!Arrays.asList("request_id","operation","arguments","timeout_ms").contains(field.name))
                return error(400,"invalid_command","Unknown command field");
            if(body.has("timeout_ms") && !body.get("timeout_ms").isLong())return error(400,"invalid_command","timeout_ms must be an integer");
            String requestId=body.getString("request_id"),operation=body.getString("operation");
            if(requestId.length()>128 || requestId.isEmpty())throw new IllegalArgumentException("Invalid request ID");
            JsonValue args=body.get("arguments");if(args==null)args=new JsonValue(JsonValue.ValueType.object);
            if(!args.isObject())throw new IllegalArgumentException("arguments must be an object");
            long timeout=body.getLong("timeout_ms",10000);if(timeout<1 || timeout>60000)throw new IllegalArgumentException("Deadline must be 1..60000 ms");
            synchronized(jobs) {
                Job old=jobs.get(requestId);
                if(old!=null) {
                    if(!old.payload.equals(request.body))return error(409,"request_conflict","Request ID has another payload");
                    return response(200,old.json());
                }
                if(jobs.size()>=1024)return error(429,"history_full","Session command limit reached; establish a new isolated session");
                Job job=new Job(requestId,operation,args,request.body,timeout);jobs.put(requestId,job);
                return response(202,job.json());
            }
        }
        if("POST".equals(request.method) && path.startsWith("/commands/") && path.endsWith("/cancel")) {
            String commandId=path.substring(10,path.length()-7);
            synchronized(jobs) {
                for(Job job:jobs.values())if(job.id.equals(commandId)) {
                    if(!job.terminal()) {job.cancelRequested=true;job.status="cancelling";}
                    return response(202,job.json());
                }
            }
            return error(404,"unknown_command","Command not found");
        }
        if("GET".equals(request.method) && path.startsWith("/commands/")) {
            synchronized(jobs) {for(Job job:jobs.values())if(job.id.equals(path.substring(10)))return response(200,job.json());}
            return error(404,"unknown_command","Command not found in this session");
        }
        if("GET".equals(request.method) && path.startsWith("/artifacts/")) {
            FileHandle file;synchronized(artifacts){file=artifacts.get(path.substring(11));}
            return file==null?error(404,"unknown_artifact","Artifact not found"):new RemoteHttpResponse(file.readBytes(),"image/png");
        }
        return error(404,"unknown_endpoint","Unknown debug endpoint");
    }
    private RemoteHttpResponse response(int status,Object data) {return new RemoteHttpResponse(status,RemoteJson.toJson(data));}
    private RemoteHttpResponse error(int status,String code,String message) {return response(status,RemoteDebugState.map("error",RemoteDebugState.map("code",code,"message",message),"session_id",id));}

    private final class Job {
        final String request,id=UUID.randomUUID().toString(),operation,payload;
        final JsonValue arguments;
        final long deadline;
        volatile String status="accepted";
        volatile Object result,error;
        boolean started;volatile boolean cancelRequested;long startedFrame;Verb verb;
        Job(String request,String operation,JsonValue arguments,String payload,long timeout) {
            this.request=request;this.operation=operation;this.arguments=arguments;this.payload=payload;deadline=System.nanoTime()+timeout*1000000L;
        }
        boolean terminal(){return "completed".equals(status)||"failed".equals(status)||"timed_out".equals(status)||"cancelled".equals(status);}
        void done(Object value){result=value;status="completed";events.add("command.completed",RemoteDebugState.map("command_id",id,"request_id",request));}
        void fail(String code,String message,Throwable cause) {
            java.io.StringWriter trace=new java.io.StringWriter();cause.printStackTrace(new java.io.PrintWriter(trace));
            String text=trace.toString();
            error=RemoteDebugState.map("code",code,"message",message,"stack",text.length()>16384?text.substring(0,16384):text,"truncated",text.length()>16384);
            status="failed";events.add("command.failed",RemoteDebugState.map("command_id",id,"error",error));
        }
        void fail(String code,String message){error=RemoteDebugState.map("code",code,"message",message);status="deadline".equals(code)?"timed_out":"failed";events.add("command.failed",RemoteDebugState.map("command_id",id,"error",error));}
        Map<String,Object> json(){return RemoteDebugState.map("session_id",RemoteDebugSession.this.id,"request_id",request,"command_id",id,"operation",operation,"status",status,"result",result,"error",error);}
    }
}

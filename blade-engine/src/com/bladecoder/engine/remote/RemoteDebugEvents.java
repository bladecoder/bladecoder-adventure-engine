package com.bladecoder.engine.remote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.WorldEventListener;
import com.bladecoder.engine.util.EngineLogger;

/** Persistent bounded internal stream, independent of the player action buffer. */
final class RemoteDebugEvents implements WorldEventListener, EngineLogger.Listener {
    private static final int LIMIT=2048;
    private final com.bladecoder.engine.model.World world;
    private final RemoteDebugState state;
    RemoteDebugEvents() {this(null,null);}
    RemoteDebugEvents(com.bladecoder.engine.model.World world,RemoteDebugState state) {this.world=world;this.state=state;}
    private final List<Map<String,Object>> history=new ArrayList<Map<String,Object>>();
    private long sequence,frame;
    private double simulationTime;
    private String command;

    synchronized void frame(long frame,double time) {this.frame=frame;simulationTime=time;}
    synchronized void command(String command) {this.command=command;}
    synchronized Map<String,Object> add(String type,Object data) {
        Map<String,Object> event=new LinkedHashMap<String,Object>();
        event.put("sequence",++sequence);event.put("frame",frame);event.put("simulation_time",simulationTime);
        event.put("type",type);event.put("command_id",command);event.put("data",data);
        if(world!=null){event.put("chapter",world.getCurrentChapter());event.put("scene",world.getCurrentScene()==null?null:world.getCurrentScene().getId());}
        if(history.size()==LIMIT)history.remove(0);
        history.add(event);notifyAll();return event;
    }
    synchronized long sequence() {return sequence;}
    synchronized Map<String,Object> read(long after,int waitMs) throws InterruptedException {
        long deadline=System.nanoTime()+waitMs*1000000L;
        while(sequence<=after && waitMs>0) {
            long remaining=deadline-System.nanoTime();if(remaining<=0)break;
            wait(Math.max(1,remaining/1000000L));
        }
        List<Map<String,Object>> result=new ArrayList<Map<String,Object>>();
        for(Map<String,Object> event:history)if(((Number)event.get("sequence")).longValue()>after)result.add(event);
        long earliest=history.isEmpty()?sequence+1:((Number)history.get(0).get("sequence")).longValue();
        return RemoteDebugState.map("events",result,"next_cursor",sequence,"earliest_cursor",earliest,
                "lost",after<earliest-1,"frame",frame);
    }
    public void text(Text text) {if(text!=null)add("dialogue.text",RemoteDebugState.map("text",text.str,"speaker",text.actorId,"text_type",String.valueOf(text.type)));}
    public void sceneChanged(String id) {add("scene.changed",RemoteDebugState.map("scene",id));}
    public void dialogOptionsChanged() {
        List<String> choices=world!=null && world.hasDialogOptions()?new ArrayList<String>(world.getDialogOptions()):new ArrayList<String>();
        add("dialogue.choices",RemoteDebugState.map("choices",choices,"choice_set",state==null?null:state.choicesChanged()));
    }
    public void cutMode(boolean value) {add("world.cutMode",value);}
    public void pause(boolean value) {add("world.pause",value);}
    public void inventoryChanged() {add("inventory.changed",null);}
    public void animationStarted(String actor,String animation,boolean finite) {add("animation.started",RemoteDebugState.map("actor",actor,"animation",animation,"finite",finite));}
    public void gameEnded() {add("game.ended",null);}
    public void diagnostic(String type,Map<String,Object> data) {
        Map<String,Object> enriched=new LinkedHashMap<String,Object>(data);
        if(state!=null && (type.startsWith("action.") || type.startsWith("verb.")) && data.get("runner_id") instanceof String && data.get("ip") instanceof Number) {
            Map<String,Object> source=state.sourceForRunner((String)data.get("runner_id"),((Number)data.get("ip")).intValue());
            enriched.put("source_ref",source);enriched.put("source",source==null?null:source.get("source"));
        }
        add(type,enriched);
    }
    public void log(String level,String message,Throwable failure) {
        String text=message==null?"":message;
        add("log",RemoteDebugState.map("level",level,"message",text.length()>8192?text.substring(0,8192):text,
                "truncated",text.length()>8192,"exception",failure==null?null:failure.toString()));
    }
}

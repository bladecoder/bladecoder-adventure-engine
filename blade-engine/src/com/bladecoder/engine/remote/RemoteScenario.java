package com.bladecoder.engine.remote;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Incremental version-1 scenario replay through the session's ordinary command queue. */
final class RemoteScenario {
    interface Backend {
        Map<String,Object> state() throws Exception;
        String submit(String operation,JsonValue arguments,long timeout,String step) throws Exception;
        Map<String,Object> command(String id);
        void cancel(String id);
        void event(String type,Object data);
        long frame();
        long cursor();
    }
    private static final Object MISSING=new Object();
    private static final Set<String> TYPES=new HashSet<String>(Arrays.asList("command","assert","wait","capture","click","key","frames","choose"));
    private static final Set<String> RESERVED=new HashSet<String>(Arrays.asList("setup","final","failure","manifest","result","report","scenario","coverage","steps","assertions","input-history","event-recording","connection","process","session"));
    private final JsonValue spec;
    private final Backend backend;
    private final List<Object> results=new ArrayList<Object>();
    private final List<Action> actions=new ArrayList<Action>();
    private final List<String> commands=new ArrayList<String>();
    private int step=-1,action;
    private boolean planned,complete;
    private String child;
    private long stepDeadline;
    private Object lastResult;
    private final List<Object> assertions=new ArrayList<Object>();

    RemoteScenario(JsonValue spec,Backend backend) {
        validate(spec);this.spec=spec;this.backend=backend;
        JsonValue setup=spec.get("setup"),viewport=setup.get("viewport");
        add("clock.freeze",map("value",true));add("fixture.viewport",map("width",viewport.getInt(0),"height",viewport.getInt(1)));
        add("fixture.locale",map("locale",setup.getString("locale")));add("fixture.seed",map("seed",setup.getInt("seed")));
        add("fixture.scene",map("chapter",setup.getString("chapter"),"scene",setup.getString("scene")));
        add("wait",map("conditions",Arrays.asList(map("path","/assetState","equals","LOADED"))));
        add("input.event",map("type","mouseMoved","x",0,"y",0));add("clock.step",map("frames",4,"delta",.05));
        planned=true;stepDeadline=System.nanoTime()+30000000000L;
    }
    static void validate(JsonValue spec) {
        keys(spec,"scenario","schema_version","id","description","setup","steps");
        integer(spec,"schema_version",1,1);id(spec.getString("id"));
        JsonValue setup=spec.require("setup");keys(setup,"setup","chapter","scene","seed","locale","viewport");
        for(String key:Arrays.asList("chapter","scene","locale"))text(setup,key);
        integer(setup,"seed",0,Integer.MAX_VALUE);
        JsonValue viewport=setup.require("viewport");require(viewport.isArray() && viewport.size==2,"Viewport must be [width,height]");
        number(viewport.get(0),320,3840);number(viewport.get(1),240,2160);
        JsonValue steps=spec.require("steps");require(steps.isArray() && steps.size>0 && steps.size<=256,"Use 1–256 scenario steps");
        Set<String> seen=new HashSet<String>();
        for(JsonValue row:steps) {
            require(row.isObject(),"Step must be an object");String name=row.getString("id");id(name);require(seen.add(name) && !RESERVED.contains(name),"Step id is duplicate/reserved");
            String type=row.getString("type");require(TYPES.contains(type),"Unsupported scenario step type");
            List<String> allowed=new ArrayList<String>(Arrays.asList("id","type","label"));
            if(type.equals("command"))allowed.addAll(Arrays.asList("operation","arguments","timeout_ms","expect_error"));
            else if(type.equals("assert"))allowed.add("condition");
            else if(type.equals("wait"))allowed.addAll(Arrays.asList("condition","timeout_ms","advance_text"));
            else if(type.equals("capture"))allowed.add("kind");
            else if(type.equals("click"))allowed.addAll(Arrays.asList("name","text","actor","point","button"));
            else if(type.equals("key"))allowed.add("keycode");
            else if(type.equals("frames"))allowed.add("count");
            else if(type.equals("choose"))allowed.add("text");
            keys(row,"step",allowed.toArray(new String[allowed.size()]));
            if(row.has("timeout_ms"))integer(row,"timeout_ms",1,60000);
            if(type.equals("command")) {
                text(row,"operation");require(!row.getString("operation").equals("scenario.run"),"Nested scenario replay is not supported");
                if(row.has("arguments"))require(row.get("arguments").isObject(),"Command arguments must be an object");
                if(row.has("expect_error"))text(row,"expect_error");
            }
            if(type.equals("assert") || type.equals("wait"))validateCondition(row.require("condition"));
            if(row.has("advance_text"))require(row.get("advance_text").isBoolean(),"advance_text must be boolean");
            if(type.equals("key"))integer(row,"keycode",0,255);
            if(type.equals("frames"))integer(row,"count",1,600);
            if(type.equals("choose"))text(row,"text");
            if(type.equals("capture"))require(Arrays.asList("ui","scene").contains(row.getString("kind","ui")),"Invalid capture kind");
            if(type.equals("click")) {
                int selectors=0;for(String key:Arrays.asList("name","text","actor","point"))if(row.has(key)){selectors++;if(!key.equals("point"))text(row,key);}
                require(selectors==1,"Click requires exactly one selector");
                if(row.has("point")){JsonValue p=row.get("point");require(p.isArray() && p.size==2,"Invalid click point");number(p.get(0),0,Integer.MAX_VALUE);number(p.get(1),0,Integer.MAX_VALUE);}
                if(row.has("button"))integer(row,"button",0,2);
            }
        }
    }
    private static void keys(JsonValue object,String label,String... allowed) {
        require(object!=null && object.isObject(),label+" must be an object");List<String> names=Arrays.asList(allowed);Set<String> seen=new HashSet<String>();
        for(JsonValue field:object)require(names.contains(field.name) && seen.add(field.name),"Unknown/duplicate "+label+" field: "+field.name);
    }
    private static void id(String id){require(id!=null && id.matches("[a-z0-9][a-z0-9-]{0,63}"),"Invalid stable scenario/step id");}
    private static void text(JsonValue row,String name){require(row.has(name) && row.get(name).isString() && !row.getString(name).isEmpty(),"Missing string "+name);}
    private static void integer(JsonValue row,String name,int min,int max){number(row.require(name),min,max);}
    private static void number(JsonValue value,int min,int max){require(value!=null && value.isLong() && value.asLong()>=min && value.asLong()<=max,"Integer outside supported range");}
    private static void require(boolean value,String message){if(!value)throw new IllegalArgumentException(message);}
    static void validateCondition(JsonValue condition) {validateCondition(condition,0);}
    private static void validateCondition(JsonValue condition,int depth) {
        require(depth<=4,"Condition nesting exceeds four levels");
        if(condition!=null && condition.isObject() && condition.size==1 && (condition.has("all") || condition.has("any"))) {
            JsonValue children=condition.child;require(children.isArray() && children.size>0 && children.size<=32,"Use 1–32 composite conditions");
            for(JsonValue child:children)validateCondition(child,depth+1);return;
        }
        keys(condition,"condition","path","equals","not_equals","contains","has_id","truthy");textOrEmpty(condition,"path");
        String path=condition.getString("path");require((path.isEmpty() || path.startsWith("/")) && !path.replace("~0","").replace("~1","").contains("~"),"Invalid JSON pointer");
        require(condition.size==2,"Use exactly one condition operator");
        if(condition.has("truthy"))require(condition.get("truthy").isBoolean(),"truthy must be boolean");
        if(condition.has("has_id"))text(condition,"has_id");
    }
    private static void textOrEmpty(JsonValue row,String key){require(row.has(key) && row.get(key).isString(),"Missing string "+key);}
    static Object pointer(Object data,String path) {
        if(path.isEmpty())return data;
        for(String part:path.substring(1).split("/",-1)) {
            part=part.replace("~1","/").replace("~0","~");
            if(data instanceof Map){Map<?,?> map=(Map<?,?>)data;if(!map.containsKey(part))return MISSING;data=map.get(part);}
            else if(data instanceof List){if(!part.matches("0|[1-9][0-9]*"))return MISSING;try{data=((List<?>)data).get(Integer.parseInt(part));}catch(RuntimeException missing){return MISSING;}}
            else return MISSING;
        }
        return data;
    }
    static boolean matches(Object state,JsonValue condition) {
        if(condition.has("all")){for(JsonValue child:condition.get("all"))if(!matches(state,child))return false;return true;}
        if(condition.has("any")){for(JsonValue child:condition.get("any"))if(matches(state,child))return true;return false;}
        Object actual=pointer(state,condition.getString("path"));if(actual==MISSING)return false;
        JsonValue operand=null;for(JsonValue v:condition)if(!v.name.equals("path"))operand=v;
        Object expected=RemoteDebugState.value(operand);String operator=operand.name;
        if(operator.equals("truthy"))return truthy(actual)==(Boolean)expected;
        if(operator.equals("has_id")) {if(actual instanceof List)for(Object item:(List<?>)actual)if(item instanceof Map && expected.equals(((Map<?,?>)item).get("id")))return true;return false;}
        if(operator.equals("contains")) {
            if(actual instanceof String && expected instanceof String)return ((String)actual).contains((String)expected);
            if(actual instanceof List){for(Object item:(List<?>)actual)if(jsonEquals(item,expected))return true;return false;}
            if(actual instanceof Map)return ((Map<?,?>)actual).containsKey(expected);return false;
        }
        boolean equal=jsonEquals(actual,expected);
        return operator.equals("equals")?equal:!equal;
    }
    static boolean jsonEquals(Object actual,Object expected) {
        if(actual instanceof Number && expected instanceof Number) {
            boolean af=actual instanceof Float || actual instanceof Double,ef=expected instanceof Float || expected instanceof Double;
            if(af!=ef)return false;
            return af?Double.compare(Double.parseDouble(actual.toString()),Double.parseDouble(expected.toString()))==0:
                ((Number)actual).longValue()==((Number)expected).longValue();
        }
        if(actual instanceof List && expected instanceof List) {
            List<?> a=(List<?>)actual,b=(List<?>)expected;if(a.size()!=b.size())return false;
            for(int i=0;i<a.size();i++)if(!jsonEquals(a.get(i),b.get(i)))return false;return true;
        }
        if(actual instanceof Map && expected instanceof Map) {
            Map<?,?> a=(Map<?,?>)actual,b=(Map<?,?>)expected;if(!a.keySet().equals(b.keySet()))return false;
            for(Object key:a.keySet())if(!jsonEquals(a.get(key),b.get(key)))return false;return true;
        }
        return actual==null?expected==null:actual.equals(expected);
    }
    private static boolean truthy(Object v) {
        if(v==null)return false;if(v instanceof Boolean)return (Boolean)v;if(v instanceof Number)return ((Number)v).doubleValue()!=0;
        if(v instanceof String)return !((String)v).isEmpty();if(v instanceof List)return !((List<?>)v).isEmpty();if(v instanceof Map)return !((Map<?,?>)v).isEmpty();return true;
    }
    private static Map<String,Object> map(Object... values){return RemoteDebugState.map(values);}
    private void add(String operation,Map<String,Object> arguments){actions.add(new Action(operation,new JsonReader().parse(RemoteJson.toJson(arguments))));}
    private void add(String operation,JsonValue arguments){actions.add(new Action(operation,arguments==null?new JsonValue(JsonValue.ValueType.object):arguments));}
    private String stepId(){return step<0?"setup":spec.get("steps").get(step).getString("id");}
    Map<String,Object> progress(){return map("id",spec.getString("id"),"step_id",complete?null:stepId(),"steps_completed",results.size(),"complete",complete);}
    Object result(){return map("scenario_id",spec.getString("id"),"steps",results,"assertions",assertions,"control_scope","Explicit setup; semantic and input steps are reported separately");}
    boolean complete(){return complete;}
    void cancel(){if(child!=null)backend.cancel(child);}
    void tick() throws Exception {
        if(complete)return;
        require(System.nanoTime()<=stepDeadline,"Scenario step deadline: "+stepId());
        JsonValue row=step<0?null:spec.get("steps").get(step);
        if(child!=null) {
            Map<String,Object> job=backend.command(child);String status=(String)job.get("status");
            if(Arrays.asList("accepted","running","cancelling").contains(status))return;
            String expected=row==null?null:row.getString("expect_error",null);
            if(!status.equals("completed")) {
                if(expected==null || !String.valueOf(job.get("error")).contains(expected))throw new IllegalStateException("Scenario step "+stepId()+" / "+job.get("operation")+": "+job.get("error"));
                assertions.add(map("step_id",stepId(),"label",row.getString("label",stepId())+" rejects expected error","passed",true));
            } else if(expected!=null)throw new IllegalStateException("Expected command rejection did not occur: "+stepId());
            lastResult=job.get("result");child=null;action++;
        }
        if(planned && action<actions.size()) {
            Action next=actions.get(action);
            if(next.operation.equals("scenario.hit")) {
                Object ui=backend.state().get("ui");boolean hit=ui instanceof Map && next.arguments.getString("actor").equals(((Map<?,?>)ui).get("scene_hit"));
                assertions.add(map("step_id",stepId(),"label","input hit-test "+next.arguments.getString("actor"),"passed",hit));
                require(hit,"Actor hit-test failed at "+stepId());action++;return;
            }
            long timeout=Math.max(1,Math.min(60000,(stepDeadline-System.nanoTime())/1000000L));
            child=backend.submit(next.operation,next.arguments,timeout,stepId());commands.add(child);return;
        }
        if(step<0) {beginStep(0);return;}
        if(!planned) {plan(row);return;}
        if(row.getString("type").equals("wait") && !matches(backend.state(),row.get("condition"))) {planned=false;return;}
        String type=row.getString("type"),level=type.equals("click") || type.equals("key") || (type.equals("command") && row.getString("operation").equals("input.event"))?"input":
            type.equals("choose") || (type.equals("command") && !row.getString("operation").startsWith("fixture.") && !row.getString("operation").startsWith("clock."))?"semantic":type.equals("command")?"fixture":"observation";
        Map<String,Object> result=map("id",stepId(),"type",type,"control_level",level,"status","passed","frame",backend.frame(),"event_cursor",backend.cursor(),"commands",new ArrayList<String>(commands));
        if(type.equals("capture") && lastResult instanceof Map){Map<?,?> capture=(Map<?,?>)lastResult;result.put("capture",map("artifact_id",capture.get("artifact_id"),"sha256",capture.get("sha256"),"width",capture.get("width"),"height",capture.get("height")));}
        results.add(result);backend.event("scenario.step",result);
        if(step+1>=spec.get("steps").size){complete=true;backend.event("scenario.completed",map("id",spec.getString("id"),"steps",results.size()));}
        else beginStep(step+1);
    }
    private void beginStep(int index) {
        step=index;actions.clear();commands.clear();action=0;planned=false;lastResult=null;
        JsonValue row=spec.get("steps").get(step);stepDeadline=System.nanoTime()+row.getLong("timeout_ms",30000)*1000000L;
        backend.event("scenario.step.started",map("scenario_id",spec.getString("id"),"step_id",stepId(),"type",row.getString("type")));
    }
    @SuppressWarnings("unchecked")
    private void plan(JsonValue row) throws Exception {
        actions.clear();action=0;planned=true;String type=row.getString("type");Map<String,Object> state=backend.state();
        if(type.equals("assert")) {
            boolean ok=matches(state,row.get("condition"));assertions.add(map("step_id",stepId(),"label",row.getString("label",stepId()),"passed",ok));
            require(ok,"Assertion failed at scenario step "+stepId());
        } else if(type.equals("command"))add(row.getString("operation"),row.get("arguments"));
        else if(type.equals("frames"))add("clock.step",map("frames",row.getInt("count"),"delta",.05));
        else if(type.equals("capture"))add("capture",map("kind",row.getString("kind","ui")));
        else if(type.equals("key")) {
            add("input.event",map("type","keyDown","keycode",row.getInt("keycode")));add("input.event",map("type","keyUp","keycode",row.getInt("keycode")));add("clock.step",map("frames",2,"delta",.05));
        } else if(type.equals("choose")) {
            List<?> choices=(List<?>)state.get("choices");int selected=-1;
            for(int i=0;i<choices.size();i++)if(choices.get(i).toString().contains(row.getString("text"))){require(selected<0,"Choice text is ambiguous");selected=i;}
            require(selected>=0,"Choice text was not offered");add("dialogue.choose",map("choice_set",state.get("choice_set"),"option",selected));
        } else if(type.equals("wait")) {
            if(!matches(state,row.get("condition"))) {
                add("clock.step",map("frames",4,"delta",.05));
                if(row.getBoolean("advance_text",false) && state.get("text")!=null)add("dialogue.advance",map());
            }
        } else if(type.equals("click")) {
            Map<String,Object> ui=(Map<String,Object>)state.get("ui");int x,y;
            if(row.has("point")){x=row.get("point").getInt(0);y=row.get("point").getInt(1);}
            else {
                Map<?,?> selected=null;List<?> candidates=(List<?>)ui.get(row.has("actor")?"scene_actors":"widgets");
                for(Object value:candidates) {
                    Map<?,?> candidate=(Map<?,?>)value;boolean match;
                    if(row.has("actor"))match=row.getString("actor").equals(candidate.get("id")) && Boolean.TRUE.equals(candidate.get("visible"));
                    else match=Boolean.TRUE.equals(candidate.get("visible")) && Boolean.TRUE.equals(candidate.get("hit_center")) && !Boolean.TRUE.equals(candidate.get("disabled")) &&
                        (row.has("name")?row.getString("name").equals(candidate.get("name")):String.valueOf(candidate.get("type")).endsWith("TextButton") && candidate.get("text")!=null && candidate.get("text").toString().contains(row.getString("text")));
                    if(match){require(selected==null,"Click selector is ambiguous");selected=candidate;}
                }
                require(selected!=null,"No hit-testable click target");Map<?,?> bounds=(Map<?,?>)selected.get("bounds");
                x=(int)(((Number)bounds.get("x")).doubleValue()+((Number)bounds.get("width")).doubleValue()/2);
                y=(int)(((Number)bounds.get("y")).doubleValue()+((Number)bounds.get("height")).doubleValue()/2);
            }
            require(x>=0 && y>=0 && x<((Number)ui.get("width")).intValue() && y<((Number)ui.get("height")).intValue(),"Click target is outside window");
            add("input.event",map("type","mouseMoved","x",x,"y",y));add("clock.step",map("frames",1,"delta",.05));
            if(row.has("actor"))actions.add(new Action("scenario.hit",new JsonReader().parse(RemoteJson.toJson(map("actor",row.getString("actor"))))));
            add("input.event",map("type","touchDown","x",x,"y",y,"button",row.getInt("button",0)));add("input.event",map("type","touchUp","x",x,"y",y,"button",row.getInt("button",0)));add("clock.step",map("frames",4,"delta",.05));
        }
    }
    private static final class Action {
        final String operation;final JsonValue arguments;
        Action(String operation,JsonValue arguments){this.operation=operation;this.arguments=arguments;}
    }
}

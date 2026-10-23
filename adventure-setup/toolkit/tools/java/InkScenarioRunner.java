import com.badlogic.gdx.utils.*;
import com.bladecoder.ink.runtime.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Executes real Ink; engine commands are recorded, never silently simulated. */
class InkScenarioRunner {
    String compiled;
    Story story;
    JsonValue world;
    final List<String> commands=new ArrayList<>(), lines=new ArrayList<>();
    void require(boolean value,String message) {if(!value) throw new AssertionError(message);}
    void keys(JsonValue value,String label,String... allowed) {
        require(value!=null && value.isObject(),label+" must be an object");
        Set<String> names=new HashSet<>(Arrays.asList(allowed));
        for(JsonValue item:value)require(names.contains(item.name),"Unknown "+label+" field: "+item.name);
    }
    void validateWorld(JsonValue value) {
        keys(value,"world","inventory","inventories","player","scene","debug","visible","actor_states","scene_states","properties");
        for(String key:Arrays.asList("inventory","player","scene","debug","visible","actor_states","scene_states","properties"))require(value.has(key),"World simulation omits "+key);
        require(value.get("inventory").isArray(),"inventory must be an array");
        require(value.get("player").isString() && value.get("scene").isString() && value.get("debug").isBoolean(),"Invalid player/scene/debug simulation types");
        for(String key:Arrays.asList("visible","actor_states","scene_states","properties"))require(value.get(key).isObject(),key+" must be a map");
    }
    static String clean(Object value) {String s=String.valueOf(value);return s.startsWith(">")?s.substring(1):s;}
    Object query(String name,Object... args) {
        String key=args.length>0?clean(args[0]):"";
        switch(name) {
        case "inInventory": return contains(world.get("inventory"),key);
        case "inInventory2": return contains(world.get("inventories")==null?null:world.get("inventories").get(clean(args[1])),key);
        case "getPlayer": return world.getString("player");
        case "getCurrentScene": return world.getString("scene");
        case "isDebug": return world.getBoolean("debug");
        case "isVisible": return world.get("visible").getBoolean(key,false);
        case "getActorState": return world.get("actor_states").getString(key,"");
        case "getSceneState": return world.get("scene_states").getString(key,"");
        case "getProperty": return world.get("properties").getString(key,"");
        default:throw new IllegalArgumentException("Unsimulated external: "+name);
        }
    }
    boolean contains(JsonValue array,String name) {
        if(array!=null) for(JsonValue v:array) if(name.equals(v.asString())) return true;
        return false;
    }
    Story fresh() throws Exception {
        Story result=new Story(compiled);
        for(String name:Arrays.asList("inInventory","inInventory2","getPlayer","getCurrentScene","isDebug","isVisible","getActorState","getSceneState","getProperty"))
            result.bindExternalFunction(name,(Story.ExternalFunction<Object>)(args)->query(name,args));
        result.onError=(message,type)->{throw new AssertionError("Ink "+type+": "+message);};
        return result;
    }
    void drain() throws Exception {
        int count=0;
        while(story.canContinue()) {
            require(++count<10000,"Continuation exceeded safety bound");
            String line=story.Continue().trim();
            lines.add(line);
            if(line.startsWith(">"))commands.add(line);
        }
    }
    List<String> choices() {
        List<String> result=new ArrayList<>();
        for(Choice c:story.getCurrentChoices())result.add(c.getText());
        return result;
    }
    void matchStrings(JsonValue values,List<String> actual,boolean present,String kind) {
        require(values==null || values.isArray(),kind+" assertions must be arrays");
        if(values!=null) for(JsonValue v:values) {
            require(v.isString(),kind+" assertion must be a string");
            boolean found=actual.stream().anyMatch(s->s.contains(v.asString()));
            require(found==present,kind+" "+(present?"missing: ":"unexpected: ")+v.asString()+"; actual="+actual);
        }
    }
    void step(JsonValue step) throws Exception {
        keys(step,"step","world","set","lists","path","choose","save_load","assert");
        require(step.size>0,"Empty scenario step");
        for(String field:Arrays.asList("set","lists","assert"))require(!step.has(field) || step.get(field).isObject(),field+" must be a map");
        for(String field:Arrays.asList("path","choose"))require(!step.has(field) || step.get(field).isString(),field+" must be a string");
        require(!step.has("save_load") || step.get("save_load").isBoolean(),"save_load must be boolean");
        if(step.has("world")) {validateWorld(step.get("world"));world=step.get("world");}
        if(step.has("set"))for(JsonValue v:step.get("set")) {
            require(v.isBoolean() || v.isString() || v.isNumber(),"Unsupported scalar simulation value");
            Object value=v.isBoolean()?v.asBoolean():v.isDouble()?v.asFloat():v.isNumber()?v.asInt():v.asString();
            story.getVariablesState().set(v.name,value);
        }
        if(step.has("lists"))for(JsonValue v:step.get("lists")) {
            require(v.isArray(),"List initialization needs an array");
            InkList list=new InkList(v.name,story);
            for(JsonValue item:v)list.addItem(item.asString());
            story.getVariablesState().set(v.name,list);
        }
        if(step.has("path")) {story.choosePathString(step.getString("path"));drain();}
        if(step.has("choose")) {
            List<String> options=choices();int found=-1;
            for(int i=0;i<options.size();i++)if(options.get(i).contains(step.getString("choose"))) {require(found==-1,"Ambiguous choice selector");found=i;}
            require(found>=0,"Choice absent: "+step.getString("choose")+"; actual="+options);
            story.chooseChoiceIndex(found);drain();
        }
        if(step.getBoolean("save_load",false)) {
            String saved=story.getState().toJson();story=fresh();story.getState().loadJson(saved);
        }
        JsonValue assertions=step.get("assert");
        if(assertions!=null) {
            keys(assertions,"assertion","choices_present","choices_absent","commands_contain","commands_absent","text_contains","variables","list_contains","command_counts");
            for(String field:Arrays.asList("variables","list_contains","command_counts"))require(!assertions.has(field) || assertions.get(field).isObject(),field+" must be a map");
            matchStrings(assertions.get("choices_present"),choices(),true,"choice");
            matchStrings(assertions.get("choices_absent"),choices(),false,"choice");
            matchStrings(assertions.get("commands_contain"),commands,true,"command");
            matchStrings(assertions.get("commands_absent"),commands,false,"command");
            matchStrings(assertions.get("text_contains"),lines,true,"text");
            if(assertions.has("variables"))for(JsonValue v:assertions.get("variables"))
                require(String.valueOf(story.getVariablesState().get(v.name)).equals(v.asString()),"Variable mismatch: "+v.name);
            if(assertions.has("list_contains"))for(JsonValue v:assertions.get("list_contains")) {
                require(v.isArray(),"List membership assertion needs an array");
                for(JsonValue item:v)require(((InkList)story.getVariablesState().get(v.name)).containsItemNamed(item.asString()),"Missing list item: "+item.asString());
            }
            if(assertions.has("command_counts"))for(JsonValue v:assertions.get("command_counts")) {
                require(v.isLong() && v.asLong()>=0,"Command count must be a nonnegative integer");
                require(commands.stream().filter(s->s.contains(v.name)).count()==v.asInt(),"Command count mismatch: "+v.name+"; commands="+commands);
            }
        }
    }
    void run(JsonValue scenario) throws Exception {
        keys(scenario,"scenario","id","world","steps");
        world=scenario.get("world");require(world!=null,"Explicit world simulation is required");
        validateWorld(world);require(scenario.get("steps")!=null && scenario.get("steps").isArray() && scenario.get("steps").size>0,"Scenario needs nonempty steps");
        story=fresh();commands.clear();lines.clear();
        for(JsonValue step:scenario.get("steps"))step(step);
        System.out.println("INK_CASE_OK "+scenario.getString("id")+" lines="+lines.size()+" commands="+commands.size());
    }
    public static void main(String[] args) throws Exception {
        InkScenarioRunner runner=new InkScenarioRunner();
        runner.compiled=new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8);
        JsonValue cases=new JsonReader().parse(new String(Files.readAllBytes(Paths.get(args[1])),StandardCharsets.UTF_8));
        runner.keys(cases,"suite","schema_version","story","scenarios");
        runner.require(cases.get("schema_version")!=null && cases.get("schema_version").isLong() && cases.getLong("schema_version")==1,"Unsupported scenario version");
        int count=0;
        runner.require(cases.get("scenarios")!=null && cases.get("scenarios").isArray() && cases.get("scenarios").size>0,"Suite needs a nonempty scenarios array");
        Set<String> ids=new HashSet<>();
        for(JsonValue scenario:cases.get("scenarios")) {runner.require(ids.add(scenario.getString("id")),"Duplicate scenario ID");runner.run(scenario);count++;}
        runner.require(count>0,"No narrative cases were run");
        System.out.println("INK_CASES_PASS "+count+" (engine commands recorded; no engine execution)");
    }
}

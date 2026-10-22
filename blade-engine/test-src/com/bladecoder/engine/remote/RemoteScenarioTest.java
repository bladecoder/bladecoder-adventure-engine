package com.bladecoder.engine.remote;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;
import java.util.Map;

/** Schema, JSON predicate and cancellation contracts; real replay is tested on a packaged game. */
final class RemoteScenarioTest {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static JsonValue json(String text){return new JsonReader().parse(text);}
    private static JsonValue sample() {
        return json("{schema_version:1,id:test,setup:{chapter:\"00\",scene:room,seed:7,locale:es,viewport:[960,540]},steps:[{id:check,type:assert,condition:{path:\"/paused\",equals:false}}]}");
    }
    private static void reject(JsonValue spec) {
        try{RemoteScenario.validate(spec);throw new AssertionError("Invalid scenario accepted");}catch(IllegalArgumentException expected){checks++;}
    }
    static int run() throws Exception {
        JsonValue valid=sample();RemoteScenario.validate(valid);checks++;
        JsonValue bad=sample();bad.get("schema_version").set(true);reject(bad);
        bad=sample();bad.get("setup").get("viewport").get(0).set(new JsonValue(99999L));reject(bad);
        bad=sample();bad.get("steps").get(0).get("id").set("result");reject(bad);
        bad=sample();bad.get("steps").get(0).get("condition").addChild("truthy",new JsonValue(false));reject(bad);
        bad=sample();bad.get("steps").get(0).get("type").set("unsupported");reject(bad);
        Map<String,Object> state=RemoteDebugState.map("none",null,"flag",false,"number",1,"fraction",.1f,"items",Arrays.asList(RemoteDebugState.map("id","gum")),"a/b",RemoteDebugState.map("~key","hello"));
        check(RemoteScenario.matches(state,json("{path:\"/none\",equals:null}")),"explicit null remains distinct from missing");
        check(!RemoteScenario.matches(state,json("{path:\"/missing\",equals:null}")),"missing does not equal null");
        check(!RemoteScenario.matches(state,json("{path:\"/missing\",truthy:false}")),"missing does not satisfy false assertion");
        check(!RemoteScenario.matches(state,json("{path:\"/number\",equals:true}")),"integer is not boolean");
        check(RemoteScenario.matches(state,json("{path:\"/number\",equals:1}")),"native integer matches JSON integer");
        check(!RemoteScenario.matches(state,json("{path:\"/number\",equals:1.0}")),"integer and float retain JSON type distinction");
        check(RemoteScenario.matches(state,json("{path:\"/fraction\",equals:0.1}")),"float predicates match wire JSON precision");
        check(RemoteScenario.matches(state,json("{path:\"/items\",has_id:gum}")),"inventory id predicate");
        check(!RemoteScenario.matches(state,json("{path:\"/items\",has_id:spray}")),"absent item is not inferred");
        check(RemoteScenario.matches(state,json("{path:\"/a~1b/~0key\",contains:ell}")),"escaped JSON pointer segments");
        check(!RemoteScenario.matches(state,json("{path:\"/items/-1/id\",equals:gum}")),"negative indices do not address JSON arrays");
        check(RemoteScenario.jsonEquals(Arrays.asList(1),Arrays.asList(1L)),"nested integer values compare as JSON integers");
        JsonValue compound=json("{all:[{path:\"/flag\",equals:false},{path:\"/number\",equals:1}]}");
        RemoteScenario.validateCondition(compound);check(RemoteScenario.matches(state,compound),"composite conditions preserve all invariants");
        check(!RemoteScenario.matches(state,json("{any:[{path:\"/flag\",equals:true},{path:\"/missing\",truthy:false}]}")),"any does not infer absent values");
        final int[] submitted={0},cancelled={0};
        RemoteScenario.Backend backend=new RemoteScenario.Backend() {
            public Map<String,Object> state(){return RemoteDebugState.map("paused",false);}
            public String submit(String op,JsonValue args,long timeout,String step){submitted[0]++;return "child";}
            public Map<String,Object> command(String id){return RemoteDebugState.map("status","running");}
            public void cancel(String id){if(id.equals("child"))cancelled[0]++;}
            public void event(String type,Object data){}
            public long frame(){return 1;}
            public long cursor(){return 1;}
        };
        RemoteScenario replay=new RemoteScenario(sample(),backend);
        check(submitted[0]==0,"construction validates without issuing setup mutations");replay.tick();
        check(submitted[0]==1,"tick queues a single ordinary child command");replay.tick();check(submitted[0]==1,"running child is not redispatched");
        replay.cancel();check(cancelled[0]==1,"parent cancellation reaches active child");
        return checks;
    }
}

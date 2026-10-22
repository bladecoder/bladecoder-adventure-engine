package com.bladecoder.engine.remote;

import com.bladecoder.engine.ink.ExternalFunctions;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Story;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class RemoteSourceTraceTest {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    static int run() throws Exception {
        Map<String,Object> first=RemoteDebugState.sourceRef("00","bank","Joe/A~B","lookat.target",3);
        check(first.get("json_pointer").equals("/scenes/bank/actors/Joe~1A~0B/verbs/lookat.target"),"JSON source pointer escapes identifiers");
        check(!first.get("source").equals(RemoteDebugState.sourceRef("00","office","Joe/A~B","lookat.target",3).get("source")),"same actor ID in different scenes has distinct source");
        check(RemoteDebugState.sourceRef("00",null,null,"lookat",1).get("resource").equals("model/world"),"global verbs retain world resource identity");
        com.badlogic.gdx.utils.JsonValue model=new com.badlogic.gdx.utils.JsonReader().parse("{scenes:{bank:{actors:{Joe:{verbs:{lookat:{actions:[]}}}}}}}");
        check(RemoteDebugState.sourceDefinition(model,"/scenes/bank/actors/Joe/verbs/lookat")!=null,"declared runtime source resolves to actual model definition");
        check(RemoteDebugState.sourceDefinition(model,"/scenes/bank/actors/RuntimeClone/verbs/lookat")==null,"runtime-created actor cannot claim an absent source definition");
        final int[] calls={0};
        Story.ExternalFunction<Object> delegate=new Story.ExternalFunction<Object>() {
            public Object call(Object... args){calls[0]++;return args[0];}
        };
        Object identity=new Object();
        check(ExternalFunctions.traced(null,"identity",delegate).call(identity)==identity && calls[0]==1,"inactive observation preserves result identity without querying Story");
        final List<String> types=new ArrayList<String>();final List<Map<String,Object>> traces=new ArrayList<Map<String,Object>>();
        EngineLogger.Listener observer=new EngineLogger.Listener() {
            public void log(String level,String message,Throwable failure){}
            public void diagnostic(String type,Map<String,Object> data){types.add(type);traces.add(data);}
        };
        Story story=new Story("{\"inkVersion\":21,\"root\":[[\"done\",null],\"done\",null],\"listDefs\":{}}");
        EngineLogger.addListener(observer);
        try {
            check(ExternalFunctions.traced(story,"identity",delegate).call(Boolean.FALSE)==Boolean.FALSE,"observed external preserves boolean result type/value");
            check(types.equals(java.util.Arrays.asList("ink.external.call","ink.external.result")),"external call and result are paired");
            check(traces.get(0).get("call_id").equals(traces.get(1).get("call_id")) && ((List<?>)traces.get(0).get("arguments")).get(0)==Boolean.FALSE,"call correlation and typed arguments retained");
            final Exception expected=new Exception("original external failure");
            try {
                ExternalFunctions.traced(story,"failing",new Story.ExternalFunction<Object>(){public Object call(Object... args)throws Exception{throw expected;}}).call();
                throw new AssertionError("external failure was swallowed");
            } catch(Exception actual){check(actual==expected,"external original exception identity preserved");}
            check(types.get(types.size()-1).equals("ink.external.failed") && traces.get(traces.size()-1).get("exception").toString().contains("original external failure"),"external failure trace retains original cause");
        } finally {EngineLogger.removeListener(observer);}
        java.util.Map<String,Object> nested=new java.util.LinkedHashMap<String,Object>();
        nested.put("a/b~c",java.util.Arrays.asList(Boolean.FALSE,null));nested.put("", "empty key");
        check(RemoteInspectorQuery.select(nested, "")==nested,"empty pointer retains full selected view");
        check(RemoteInspectorQuery.select(nested, "/a~1b~0c/0")==Boolean.FALSE,"focused pointer preserves escaped keys and typed values");
        check(RemoteInspectorQuery.select(nested, "/a~1b~0c/1")==null,"explicit null is not a missing field");
        check(RemoteInspectorQuery.select(nested, "/").equals("empty key"),"empty object key can be queried");
        for(String invalid:new String[]{"relative","/~2","/a~1b~0c/01","/a~1b~0c/2","/a~1b~0c/999999999999999","/missing"}) {
            try {RemoteInspectorQuery.select(nested,invalid);throw new AssertionError("invalid pointer accepted: "+invalid);}
            catch(IllegalArgumentException expected){checks++;}
        }
        return checks;
    }
}

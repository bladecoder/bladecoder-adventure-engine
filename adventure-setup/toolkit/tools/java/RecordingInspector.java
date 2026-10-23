import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/** Decode legacy Recorder files using LibGDX's actual relaxed JSON grammar. No game is launched. */
class RecordingInspector {
    static void require(boolean value,String message){if(!value)throw new IllegalArgumentException(message);}
    static JsonValue string(String value){return value==null?new JsonValue(JsonValue.ValueType.nullValue):new JsonValue(value);}
    public static void main(String[] args) throws Exception {
        require(args.length==1,"Supply one packaged .verbs.rec file");
        byte[] bytes=Files.readAllBytes(Paths.get(args[0]));require(bytes.length<=4*1024*1024,"Recording exceeds 4 MiB");
        JsonValue source=new JsonReader().parse(new String(bytes,StandardCharsets.UTF_8));
        require(source.isArray() && source.size>0 && source.size<=4096,"Use 1–4096 recorded steps");
        JsonValue result=new JsonValue(JsonValue.ValueType.array);int index=0;
        for(JsonValue row:source) {
            require(row.isObject(),"Recorded step must be an object");
            for(JsonValue field:row)require(Arrays.asList("time","verb","actorId","target","dialogOption","pos").contains(field.name),"Unknown legacy field: "+field.name);
            float time=row.getFloat("time",0);require(Float.isFinite(time) && time>=0,"Invalid legacy wait time");
            String verb=row.getString("verb",null),actor=row.getString("actorId",null),target=row.getString("target",null);
            JsonValue output=new JsonValue(JsonValue.ValueType.object);output.addChild("index",new JsonValue((long)index++));output.addChild("legacy_wait_seconds",new JsonValue((double)time));
            if(verb!=null) {
                output.addChild("kind",new JsonValue(verb.equals("SAVEGAME")?"save":"verb"));
                output.addChild("verb",new JsonValue(verb));output.addChild("actor",string(actor));output.addChild("target",string(target));
            } else if(row.has("pos") && !row.get("pos").isNull()) {
                JsonValue pos=row.get("pos");require(pos.isObject(),"Position must be an object");float x=pos.getFloat("x"),y=pos.getFloat("y");
                require(Float.isFinite(x) && Float.isFinite(y),"Movement coordinates must be finite");
                output.addChild("kind",new JsonValue("move"));output.addChild("x",new JsonValue((double)x));output.addChild("y",new JsonValue((double)y));
            } else {
                int option=row.getInt("dialogOption",0);require(option>=0,"Negative dialog index");
                output.addChild("kind",new JsonValue("choice"));output.addChild("legacy_option",new JsonValue((long)option));
            }
            result.addChild(output);
        }
        System.out.println(result.toJson(JsonWriter.OutputType.json));
    }
}

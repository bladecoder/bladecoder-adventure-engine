package com.bladecoder.engine.remote;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.ink.InkManager;
import com.bladecoder.engine.ink.InkVerbRunner;
import com.bladecoder.engine.model.*;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.ink.runtime.Story;

/** Game-thread snapshots and bounded selected-resource lookups for verified source identities. */
final class RemoteDebugState {
    final World world;
    private long choicesRevision;
    private final Map<String,JsonValue> sourceDocuments=new LinkedHashMap<String,JsonValue>();
    private final Map<String,Object> sourceDocumentStatus=new LinkedHashMap<String,Object>();
    static JsonValue sourceDefinition(JsonValue root,String pointer) {
        if(root==null)return null;
        JsonValue value=root;
        for(String part:pointer.substring(1).split("/")) {
            if(value==null || !value.isObject())return null;
            value=value.get(part.replace("~1","/").replace("~0","~"));
        }
        return value;
    }
    private JsonValue sourceDocument(String resource) {
        if(sourceDocumentStatus.containsKey(resource))return sourceDocuments.get(resource);
        try {
            byte[] bytes=com.bladecoder.engine.assets.EngineAssetManager.getInstance().getAsset(resource).readBytes();
            if(bytes.length>16*1024*1024)throw new IllegalArgumentException("Source lookup exceeds 16 MiB");
            JsonValue parsed=new JsonReader().parse(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hash=new StringBuilder();for(byte b:digest)hash.append(String.format("%02x",b&255));
            sourceDocuments.put(resource,parsed);sourceDocumentStatus.put(resource,map("sha256",hash.toString(),"verified",true));return parsed;
        } catch(Exception failure) {sourceDocumentStatus.put(resource,map("verified",false,"error",failure.toString()));return null;}
    }
    private Scene indexedScene;
    private String indexedChapter;
    private final Map<String,List<Map<String,Object>>> sourceIndex=new LinkedHashMap<String,List<Map<String,Object>>>();
    private final List<Map<String,Object>> sourceCatalogue=new ArrayList<Map<String,Object>>();
    static String pointerPart(String value){return value.replace("~","~0").replace("/","~1");}
    static Map<String,Object> sourceRef(String chapter,String scene,String actor,String verb,int actionCount) {
        String resource=scene==null?"model/world":"model/"+chapter+".chapter.json";
        String pointer=scene==null?"/verbs/"+pointerPart(verb):"/scenes/"+pointerPart(scene)+(actor==null?"":"/actors/"+pointerPart(actor))+"/verbs/"+pointerPart(verb);
        return map("source",resource+"#"+pointer,"resource",resource,"json_pointer",pointer,
            "chapter",chapter,"scene",scene,"actor",actor,"verb",verb,"action_count",actionCount,"mapping","runtime_model_identity");
    }
    private void indexManager(VerbManager manager,String chapter,String scene,String actor) {
        for(Map.Entry<String,Verb> entry:manager.getVerbs().entrySet()) {
            Verb verb=entry.getValue();String id=Integer.toHexString(System.identityHashCode(verb));
            Map<String,Object> ref=sourceRef(chapter,scene,actor,entry.getKey(),verb.getActions().size());
            JsonValue definition=sourceDefinition(sourceDocument((String)ref.get("resource")),(String)ref.get("json_pointer"));
            if(definition==null || !definition.isObject() || !definition.has("actions"))continue;
            ref.put("mapping","verified_model_definition");
            ref.put("resource_sha256",((Map<?,?>)sourceDocumentStatus.get(ref.get("resource"))).get("sha256"));
            ref.put("declared_action_count",definition.get("actions").size);
            ref.put("runtime_action_count",verb.getActions().size());ref.put("action_count",definition.get("actions").size);
            List<Map<String,Object>> refs=sourceIndex.get(id);
            if(refs==null){refs=new ArrayList<Map<String,Object>>();sourceIndex.put(id,refs);}
            refs.add(ref);sourceCatalogue.add(ref);
        }
    }
    private void rebuildSources() {
        indexedScene=world.getCurrentScene();indexedChapter=world.getCurrentChapter();
        sourceIndex.clear();sourceCatalogue.clear();sourceDocuments.clear();sourceDocumentStatus.clear();
        indexManager(world.getVerbManager(),indexedChapter,null,null);
        for(Scene scene:world.getScenes().values()) {
            indexManager(scene.getVerbManager(),indexedChapter,scene.getId(),null);
            for(BaseActor actor:scene.getActors().values())if(actor instanceof InteractiveActor)
                indexManager(((InteractiveActor)actor).getVerbManager(),indexedChapter,scene.getId(),actor.getId());
        }
    }
    private void ensureSources(){if(sourceIndex.isEmpty() || indexedScene!=world.getCurrentScene() || !java.util.Objects.equals(indexedChapter,world.getCurrentChapter()))rebuildSources();}
    Map<String,Object> sourceForRunner(String runner,int ip) {
        ensureSources();List<Map<String,Object>> refs=sourceIndex.get(runner);
        if(refs==null || refs.size()!=1)return null; // Missing/ambiguous identities remain explicit.
        Map<String,Object> ref=new LinkedHashMap<String,Object>(refs.get(0));
        int count=((Number)ref.get("action_count")).intValue();
        if(ip>=0 && ip<count && ip<((Number)ref.get("declared_action_count")).intValue())ref.put("action_pointer",ref.get("json_pointer")+"/actions/"+ip);
        return ref;
    }
    private String lastChoices="";
    RemoteDebugState(World world) {this.world=world;}

    private String choiceSignature() {
        Scene scene=world.getCurrentScene();
        return String.valueOf(scene==null?null:scene.getId())+(world.hasDialogOptions()?world.getDialogOptions().toString():"[]");
    }
    long choicesChanged() {lastChoices=choiceSignature();return ++choicesRevision;}
    static Map<String,Object> map(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<String,Object>();
        for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;
    }
    static Object value(JsonValue json) {
        if(json==null || json.isNull())return null;
        if(json.isObject()) {Map<String,Object> m=map();for(JsonValue child:json)m.put(child.name,value(child));return m;}
        if(json.isArray()) {List<Object> list=new ArrayList<Object>();for(JsonValue child:json)list.add(value(child));return list;}
        if(json.isBoolean())return json.asBoolean();if(json.isLong())return json.asLong();if(json.isDouble())return json.asDouble();return json.asString();
    }
    static Object serialized(Object object,World world) {
        return value(new JsonReader().parse(new BladeJson(world,BladeJson.Mode.STATE).toJson(object)));
    }
    Map<String,Object> audio() {
        Scene current = world.getCurrentScene();
        return map("music",world.getMusicManager().getDebugState(),
            "sfx",current == null ? null : current.getSoundManager().getDebugState());
    }
    Map<String,Object> snapshot() throws Exception {
        ensureSources();
        Map<String,Object> result=map("disposed",world.isDisposed(),"paused",world.isPaused(),"cutMode",world.inCutMode(),
                "assetState",String.valueOf(world.getAssetState()),"chapter",world.getCurrentChapter(),
                "properties",new LinkedHashMap<String,String>(world.getCustomProperties()),"time_of_game_ms",world.getTimeOfGame());
        Scene current=world.getCurrentScene();
        List<Object> scenes=new ArrayList<Object>();
        for(Scene scene:world.getScenes().values()) {
            List<Object> actors=new ArrayList<Object>();
            for(BaseActor actor:scene.getActors().values())actors.add(actor(actor));
            scenes.add(map("id",scene.getId(),"current",scene==current,"state",scene.getState(),"actors",actors));
        }
        result.put("scenes",scenes);
        result.put("source_catalogue",new ArrayList<Map<String,Object>>(sourceCatalogue));
        result.put("source_document_status",new LinkedHashMap<String,Object>(sourceDocumentStatus));
        List<Object> sceneVerbs=new ArrayList<Object>();
        if(current!=null)for(Map.Entry<String,Verb> entry:current.getVerbManager().getVerbs().entrySet())
            sceneVerbs.add(runner(entry.getValue(),"scene/"+current.getId()+"/"+entry.getKey()));
        result.put("scene_verbs",sceneVerbs);
        if(current!=null) {
            List<Object> actors=new ArrayList<Object>();
            for(BaseActor actor:current.getActors().values())actors.add(actor(actor));
            result.put("scene",map("id",current.getId(),"player",current.getPlayer()==null?null:current.getPlayer().getId(),
                    "actors",actors,"camera",map("x",current.getCamera().position.x,"y",current.getCamera().position.y,
                    "zoom",current.getCamera().zoom,"width",current.getCamera().viewportWidth,"height",current.getCamera().viewportHeight),
                    "background_atlas",current.getBackgroundAtlas(),"background_region",current.getBackgroundRegionId()));
            Text t=current.getTextManager().getCurrentText();
            result.put("text",t==null?null:map("text",t.str,"speaker",t.actorId,"type",String.valueOf(t.type)));
        } else result.put("scene",null);
        List<Object> items=new ArrayList<Object>();
        if(world.getInventory()!=null)for(int i=0;i<world.getInventory().getNumItems();i++)items.add(actor(world.getInventory().get(i)));
        result.put("inventory",items);
        List<String> choices=world.hasDialogOptions()?new ArrayList<String>(world.getDialogOptions()):new ArrayList<String>();
        String signature=choiceSignature();
        if(!signature.equals(lastChoices)){choicesRevision++;lastChoices=signature;}
        result.put("choices",choices);result.put("choice_set",choicesRevision);
        InkManager ink=world.getInkManager();Story story=ink==null?null:ink.getStory();
        if(story!=null) {
            Map<String,Object> variables=map();
            for(String name:story.getVariablesState()) {
                Object raw=story.getVariablesState().get(name);
                variables.put(name,map("type",raw==null?"null":raw.getClass().getSimpleName(),"value",primitive(raw)));
            }
            List<Object> runners=new ArrayList<Object>();
            for(Map.Entry<String,InkVerbRunner> entry:ink.getVerbRunners().entrySet())runners.add(runner(entry.getValue(),"ink/"+entry.getKey()));
            result.put("ink",map("story",ink.getStoryName(),"path",story.getState().getCurrentPathString(),
                    "flow",story.getCurrentFlowName(),"tags",story.getCurrentTags(),"variables",variables,"runners",runners,
                    "errors",story.getCurrentErrors(),"warnings",story.getCurrentWarnings(),
                    "random",inkRandom(story)));
        } else result.put("ink",null);
        if(current!=null && world.getAssetState()==World.AssetState.LOADED && current.getBackgroundAtlas()!=null) {
            List<Object> regions=new ArrayList<Object>();
            for(com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion r:com.bladecoder.engine.assets.EngineAssetManager.getInstance().getTextureAtlas(current.getBackgroundAtlas()).getRegions())
                regions.add(map("name",r.name,"index",r.index,"width",r.originalWidth,"height",r.originalHeight));
            result.put("background_regions",regions);
        }
        result.put("music",serialized(world.getMusicManager(),world));
        result.put("audio",audio());
        result.put("serialized_state",current==null?null:serialized(world.getSerializer(),world));
        result.put("diagnostics",map("last_error",EngineLogger.lastError,"exception",EngineLogger.lastException==null?null:EngineLogger.lastException.toString(),"error_trace_tail",EngineLogger.errorBuffer.substring(Math.max(0,EngineLogger.errorBuffer.length()-16384))));
        result.put("graphics",map("width",Gdx.graphics.getWidth(),"height",Gdx.graphics.getHeight(),"fps",Gdx.graphics.getFramesPerSecond()));
        return result;
    }
    static Map<String,Object> inkRandom(Story story) throws Exception {
        JsonValue saved=new JsonReader().parse(story.getState().toJson());
        return map("story_seed",saved.has("storySeed")?saved.getLong("storySeed"):null,
            "previous_random",saved.has("previousRandom")?saved.getLong("previousRandom"):null);
    }
    static Object primitive(Object raw) {
        if(raw==null || raw instanceof String || raw instanceof Boolean || raw instanceof Number)return raw;
        if(raw instanceof Map) {
            Map<String,Object> entries=map();
            for(Map.Entry<?,?> e:((Map<?,?>)raw).entrySet())entries.put(e.getKey().toString(),primitive(e.getValue()));
            return entries;
        }
        return raw.toString();
    }
    Map<String,Object> runner(VerbRunner runner,String source) {
        int ip=runner.getIP();
        return map("source",source,"source_ref",runner instanceof Verb?sourceForRunner(Integer.toHexString(System.identityHashCode(runner)),ip):runner instanceof InkVerbRunner && ip>=0 && ip<runner.getActions().size()?((InkVerbRunner)runner).getActionSource(runner.getActions().get(ip)):null,"ip",ip,"runner_id",Integer.toHexString(System.identityHashCode(runner)),"target",runner.getCurrentTarget(),"action_count",runner.getActions().size(),
                "active",ip>=0 && ip<runner.getActions().size(),
                "action",ip>=0 && ip<runner.getActions().size()?runner.getActions().get(ip).getClass().getName():null);
    }
    Map<String,Object> actor(BaseActor actor) {
        Map<String,Object> data=map("id",actor.getId(),"type",actor.getClass().getName(),"visible",actor.isVisible(),
                "x",actor.getX(),"y",actor.getY());
        List<Float> bounds=new ArrayList<Float>();
        for(float point:actor.getBBox().getTransformedVertices())bounds.add(point);
        data.put("bounds",bounds);
        if(actor instanceof InteractiveActor) {
            InteractiveActor interactive=(InteractiveActor)actor;
            List<Object> verbs=new ArrayList<Object>();
            for(Map.Entry<String,Verb> entry:interactive.getVerbManager().getVerbs().entrySet())verbs.add(runner(entry.getValue(),"actor/"+actor.getId()+"/"+entry.getKey()));
            data.put("layer",interactive.getLayer());data.put("z_index",interactive.getZIndex());
            data.put("interaction",interactive.getInteraction());data.put("description",interactive.getDesc());
            data.put("can_interact",interactive.canInteract());data.put("state",interactive.getState());data.put("verbs",verbs);
        }
        if(actor instanceof SpriteActor) {
            SpriteActor sprite=(SpriteActor)actor;ActorRenderer renderer=sprite.getRenderer();
            data.put("scaleX",sprite.getScaleX());data.put("scaleY",sprite.getScaleY());data.put("rotation",sprite.getRot());
            data.put("width",sprite.getWidth());data.put("height",sprite.getHeight());
            Map<String,Object> render=map("type",renderer==null?null:renderer.getClass().getName());
            if(renderer instanceof AnimationRenderer) {
                AnimationRenderer animation=(AnimationRenderer)renderer;
                render.put("animation",animation.getCurrentAnimationId());render.put("align",animation.getOrgAlign());
                render.put("available",new ArrayList<String>(animation.getAnimations().keySet()));
            }
            if(renderer instanceof AtlasRenderer) {
                AtlasRenderer atlas=(AtlasRenderer)renderer;
                render.put("frame",atlas.getCurrentFrameIndex());render.put("frame_count",atlas.getCurrentAnimation() instanceof AtlasAnimationDesc && ((AtlasAnimationDesc)atlas.getCurrentAnimation()).regions!=null?atlas.getNumFrames():0);
                if(atlas.getCurrentAnimation() instanceof AtlasAnimationDesc) {
                    AtlasAnimationDesc desc=(AtlasAnimationDesc)atlas.getCurrentAnimation();
                    render.put("duration",desc.duration);render.put("repeat",String.valueOf(desc.animationType));
                    List<Object> regions=new ArrayList<Object>();
                    if(desc.regions!=null)for(com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion region:desc.regions)
                        regions.add(map("index",region.index,"width",region.originalWidth,"height",region.originalHeight,"offsetX",region.offsetX,"offsetY",region.offsetY));
                    render.put("regions",regions);
                }
            }
            if(renderer!=null)render.put("details",renderer.getDebugState());
            data.put("renderer",render);
        }
        return data;
    }
}

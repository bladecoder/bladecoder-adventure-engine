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

/** Game-thread snapshots. No reflection, raw model reads, or observer side effects. */
final class RemoteDebugState {
    final World world;
    private long choicesRevision;
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
    Map<String,Object> snapshot() throws Exception {
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
                    "errors",story.getCurrentErrors(),"warnings",story.getCurrentWarnings()));
        } else result.put("ink",null);
        if(current!=null && world.getAssetState()==World.AssetState.LOADED && current.getBackgroundAtlas()!=null) {
            List<Object> regions=new ArrayList<Object>();
            for(com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion r:com.bladecoder.engine.assets.EngineAssetManager.getInstance().getTextureAtlas(current.getBackgroundAtlas()).getRegions())
                regions.add(map("name",r.name,"index",r.index,"width",r.originalWidth,"height",r.originalHeight));
            result.put("background_regions",regions);
        }
        result.put("music",serialized(world.getMusicManager(),world));
        result.put("serialized_state",current==null?null:serialized(world.getSerializer(),world));
        result.put("diagnostics",map("last_error",EngineLogger.lastError,"exception",EngineLogger.lastException==null?null:EngineLogger.lastException.toString(),"error_trace_tail",EngineLogger.errorBuffer.substring(Math.max(0,EngineLogger.errorBuffer.length()-16384))));
        result.put("graphics",map("width",Gdx.graphics.getWidth(),"height",Gdx.graphics.getHeight(),"fps",Gdx.graphics.getFramesPerSecond()));
        return result;
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
    static Map<String,Object> runner(VerbRunner runner,String source) {
        int ip=runner.getIP();
        return map("source",source,"ip",ip,"runner_id",Integer.toHexString(System.identityHashCode(runner)),"target",runner.getCurrentTarget(),"action_count",runner.getActions().size(),
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
            data.put("renderer",render);
        }
        return data;
    }
}

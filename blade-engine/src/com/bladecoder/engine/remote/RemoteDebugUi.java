package com.bladecoder.engine.remote;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.UI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Read-only UI geometry in logical top-left input coordinates, not framebuffer pixels. */
final class RemoteDebugUi {
    static Stage stage(InputProcessor processor) {
        if(processor instanceof Stage)return (Stage)processor;
        if(processor instanceof InputMultiplexer)
            for(InputProcessor child:((InputMultiplexer)processor).getProcessors()) {
                Stage found=stage(child);if(found!=null)return found;
            }
        return null;
    }
    static Map<String,Object> snapshot(UI ui) {
        InputProcessor processor=Gdx.input.getInputProcessor();Stage stage=stage(processor);
        int x=Gdx.input.getX(),y=Gdx.input.getY();
        List<Object> widgets=new ArrayList<Object>();
        if(stage!=null)walk(stage.getRoot(),"root",true,widgets,stage);
        Vector2 point=new Vector2(x,y);Actor hit=null;
        if(stage!=null){stage.screenToStageCoordinates(point);hit=stage.hit(point.x,point.y,true);}
        Map<String,Object> result=RemoteDebugState.map("screen",ui.getCurrentScreen().getClass().getName(),
            "coordinates","logical window pixels; origin top-left", "width",Gdx.graphics.getWidth(),"height",Gdx.graphics.getHeight(),
            "input_processor",processor==null?null:processor.getClass().getName(),"widgets",widgets,"truncated",widgets.size()>=800,
            "pointer",RemoteDebugState.map("x",x,"y",y,"pressed",Gdx.input.isTouched(),"widget_hit",hit==null?null:path(hit)));
        World world=ui.getWorld();
        result.put("locale",world.getI18N().getCurrentLocale().toLanguageTag());
        com.badlogic.gdx.graphics.glutils.GLVersion gl=Gdx.graphics.getGLVersion();
        result.put("render_environment",RemoteDebugState.map(
            "backend",Gdx.graphics.getType().name(),"application",Gdx.app.getType().name(),
            "gl_version",gl==null?null:gl.getVersionString(),"gl_vendor",gl==null?null:gl.getVendorString(),
            "gl_renderer",gl==null?null:gl.getRendererString(),
            "backbuffer_width",Gdx.graphics.getBackBufferWidth(),"backbuffer_height",Gdx.graphics.getBackBufferHeight(),
            "backbuffer_scale",Gdx.graphics.getBackBufferScale(),"density",Gdx.graphics.getDensity(),
            "ppi_x",Gdx.graphics.getPpiX(),"ppi_y",Gdx.graphics.getPpiY(),
            "buffer_format",Gdx.graphics.getBufferFormat().toString()));
        Map<String,Object> fonts=new java.util.TreeMap<String,Object>();
        com.badlogic.gdx.utils.ObjectMap<String,com.badlogic.gdx.graphics.g2d.BitmapFont> registered=ui.getSkin().getAll(com.badlogic.gdx.graphics.g2d.BitmapFont.class);
        if(registered!=null)for(com.badlogic.gdx.utils.ObjectMap.Entry<String,com.badlogic.gdx.graphics.g2d.BitmapFont> entry:registered) {
            com.badlogic.gdx.graphics.g2d.BitmapFont font=entry.value;
            com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData data=font.getData();
            fonts.put(entry.key,RemoteDebugState.map("name",data.name,"source",data.fontFile==null?null:data.fontFile.path(),
                "line_height",data.lineHeight,"scale_x",data.scaleX,"scale_y",data.scaleY,
                "integer_positions",font.usesIntegerPositions(),"pages",font.getRegions().size));
        }
        result.put("fonts",fonts);
        List<Object> actors=new ArrayList<Object>();
        if(ui.getCurrentScreen() instanceof SceneScreen && world.getCurrentScene()!=null && world.getAssetState()==World.AssetState.LOADED) {
            SceneScreen screen=(SceneScreen)ui.getCurrentScreen();
            InteractiveActor target=world.getInteractiveActorAtInput(screen.getViewport(),0);
            result.put("scene_hit",target==null?null:target.getId());
            for(BaseActor actor:world.getCurrentScene().getActors().values()) {
                float[] vertices=actor.getBBox().getTransformedVertices();
                float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
                for(int i=0;i<vertices.length;i+=2) {
                    Vector3 p=new Vector3(vertices[i],vertices[i+1],0);
                    world.getSceneCamera().project(p,screen.getViewport().getScreenX(),screen.getViewport().getScreenY(),
                        screen.getViewport().getScreenWidth(),screen.getViewport().getScreenHeight());
                    float py=Gdx.graphics.getHeight()-p.y;
                    minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,py);maxY=Math.max(maxY,py);
                }
                if(vertices.length>0)actors.add(RemoteDebugState.map("id",actor.getId(),"visible",actor.isVisible(),
                    "bounds",RemoteDebugState.map("x",minX,"y",minY,"width",maxX-minX,"height",maxY-minY)));
            }
        }
        result.put("scene_actors",actors);
        if(ui.getCurrentScreen() instanceof com.bladecoder.engine.ui.defaults.DefaultSceneScreen) {
            com.bladecoder.engine.ui.InventoryUI inventory=((com.bladecoder.engine.ui.defaults.DefaultSceneScreen)ui.getCurrentScreen()).getInventoryUI();
            List<Object> items=new ArrayList<Object>();
            if(inventory.isVisible())for(int i=0;i<world.getInventory().getNumItems();i++) {
                com.badlogic.gdx.math.Rectangle tile=inventory.getItemBounds(i);
                Vector2 origin=inventory.localToScreenCoordinates(new Vector2(tile.x,tile.y+tile.height));
                items.add(RemoteDebugState.map("id",world.getInventory().get(i).getId(),
                    "bounds",RemoteDebugState.map("x",origin.x,"y",origin.y,"width",tile.width,"height",tile.height)));
            }
            result.put("inventory",RemoteDebugState.map("visible",inventory.isVisible(),
                "dragged_actor",inventory.getDraggedActor()==null?null:inventory.getDraggedActor().getId(),"items",items));
        }
        return result;
    }
    private static String path(Actor actor) {
        if(actor.getParent()==null)return "root";
        return path(actor.getParent())+"/"+actor.getParent().getChildren().indexOf(actor,true);
    }
    private static void walk(Actor actor,String path,boolean parentVisible,List<Object> rows,Stage stage) {
        if(rows.size()>=800)return;
        boolean visible=parentVisible && actor.isVisible();
        Vector2 a=stage.stageToScreenCoordinates(actor.localToStageCoordinates(new Vector2()));
        Vector2 b=stage.stageToScreenCoordinates(actor.localToStageCoordinates(new Vector2(actor.getWidth(),actor.getHeight())));
        Vector2 center=actor.localToStageCoordinates(new Vector2(actor.getWidth()/2,actor.getHeight()/2));
        Actor hit=stage.hit(center.x,center.y,true);
        String text=actor instanceof TextButton?((TextButton)actor).getText().toString():
            actor instanceof Label?((Label)actor).getText().toString():actor instanceof TextField?((TextField)actor).getText():null;
        rows.add(RemoteDebugState.map("path",path,"type",actor.getClass().getName(),"name",actor.getName(),
            "text",text!=null && text.length()>4096?text.substring(0,4096):text,"text_truncated",text!=null && text.length()>4096,"visible",visible,"touchable",actor.getTouchable().toString(),
            "disabled",actor instanceof Button && ((Button)actor).isDisabled(),
            "hit_center",visible && hit!=null && (hit==actor || actor.isAscendantOf(hit)),
            "bounds",RemoteDebugState.map("x",Math.min(a.x,b.x),"y",Math.min(a.y,b.y),"width",Math.abs(b.x-a.x),"height",Math.abs(b.y-a.y))));
        if(actor instanceof Group) {
            int i=0;for(Actor child:((Group)actor).getChildren())walk(child,path+"/"+i++,visible,rows,stage);
        }
    }
}

package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.BaseActor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.SpriteAtlasActor;
import org.bladecoder.engine.model.World;

public class PickUpAction implements Action {
	public static final String INFO = "Puts the selected actor in the inventory.";
	public static final Param[] PARAMS = {
		new Param("scene", "If not empty, pickup the actor from the current scene", Type.STRING),
		new Param("frame_animation", "The sprite to show while in inventory. If empty, the sprite will be 'actorid.inventory'", Type.STRING)
		};
	
	String actorId;
	String fa;
	String scene;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		fa = params.get("frame_animation");
		scene = params.get("scene");
	}

	@Override
	public void run() {
		BaseActor actor = null;
		
		Scene scn;
		
		if(scene != null) {
			scn = World.getInstance().getScene(scene);
			actor = scn.getActor(actorId);
			actor.loadAssets();
			EngineAssetManager.getInstance().getManager().finishLoading();
			actor.retrieveAssets();
		} else {
			scn = World.getInstance().getCurrentScene();
			actor = scn.getActor(actorId);
		}
		
		scn.removeActor(actor);
		
		if (actor instanceof SpriteAtlasActor) {
			SpriteAtlasActor a = (SpriteAtlasActor) actor;

			if(fa != null)
				a.startFrameAnimation(fa, null);
			else
				a.startFrameAnimation(a.getId() + ".inventory", null);
			
			World.getInstance().getInventory().addItem(a);
		}
	}
	

	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

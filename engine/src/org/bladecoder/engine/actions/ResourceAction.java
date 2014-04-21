package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

/**
 * Resource Management: Use for better control on loading/unloading atlases for
 * optimizing memory use
 * 
 * @author rgarcia
 */
public class ResourceAction implements Action {
	public static final String INFO = "Resource management action";
	public static final Param[] PARAMS = {
		new Param("load", "Load the selected atlas. Waits till loaded", Type.STRING),
		new Param("load_threaded", "Load the selected atlas. Doesn't wait", Type.STRING),
		new Param("unload", "Dispose the selected atlas", Type.STRING),
//		new Param("loadFA", "Retrieve the selected Frame Animation for the actor", Type.STRING),
		};	
	
	
	private String load;
	private String loadThreaded;
	private String unload;
//	private String loadFA;

//	private String actorId;

	@Override
	public void setParams(HashMap<String, String> params) {
//		actorId = params.get("actor");

		load = params.get("load");
		loadThreaded = params.get("load_threaded");
		unload = params.get("unload");
//		loadFA = params.get("loadFA");

	}

	@Override
	public void run() {
		EngineLogger.debug("RESOURCE ACTION");

		if (unload != null) {
			EngineAssetManager.getInstance().disposeAtlas(unload);
			World.getInstance().getCurrentScene().removeAtlas(unload);
		}
		
		if (load != null) {
			EngineAssetManager.getInstance().loadAtlas(load);
			World.getInstance().getCurrentScene().addAtlas(load);
			EngineAssetManager.getInstance().getManager().finishLoading();
		}
		
		if (loadThreaded != null) {
			EngineAssetManager.getInstance().loadAtlas(loadThreaded);
			World.getInstance().getCurrentScene().addAtlas(loadThreaded);
		}		
		
//		if (loadFA != null) {
//			SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId);
//			actor.retrieveFA(loadFA);
//		}

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

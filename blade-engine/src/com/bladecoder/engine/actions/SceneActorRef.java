package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;

public class SceneActorRef {
	private final String sceneId, actorId;

	public SceneActorRef() {
		this(null, null);
	}

	public SceneActorRef(String sceneId, String actorId) {
		this.sceneId = sceneId;
		this.actorId = actorId;
	}
	
	public SceneActorRef(String sceneActor) {
		if (sceneActor == null) {
			sceneId = null;
			actorId = null;
			return;
		}

		int idx = sceneActor.indexOf(Param.STRING_PARAM_SEPARATOR.charAt(0));
		
		// Also allow '.' character as separator because is more usable inside Ink files.
		if(idx == -1)
			idx = sceneActor.indexOf('.');

		if (idx != -1) {
			sceneId = sceneActor.substring(0, idx);
			actorId = sceneActor.substring(idx + 1);
		} else {
			sceneId = null;
			actorId = sceneActor;
		}
	}

	public String getSceneId() {
		return sceneId;
	}

	public String getActorId() {
		return actorId;
	}

	public Scene getScene(World world) {
		if (sceneId != null && !sceneId.trim().isEmpty()) {
			return world.getScene(sceneId);
		} else {
			return world.getCurrentScene();
		}
	}
	
	
	public BaseActor getActor(World w, boolean searchInventory) {
		Scene scn = getScene(w);
		
		if(scn == null)
			scn = w.getCurrentScene();
		
		return scn.getActor(actorId, searchInventory);
	}

	public BaseActor getActor(World w) {
		return getActor(w, true);
	}
	
	public String toString() {
		if( sceneId==null || sceneId.isEmpty())
			return actorId;
		
		return sceneId + Param.STRING_PARAM_SEPARATOR + actorId;
	}
}

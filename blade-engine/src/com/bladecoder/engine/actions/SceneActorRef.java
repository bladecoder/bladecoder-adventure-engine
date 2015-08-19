package com.bladecoder.engine.actions;

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

	public String getSceneId() {
		return sceneId;
	}

	public String getActorId() {
		return actorId;
	}

	public Scene getScene() {
		final World world = World.getInstance();
		if (sceneId != null && !sceneId.trim().isEmpty()) {
			return world.getScene(sceneId);
		} else {
			return world.getCurrentScene();
		}
	}
}

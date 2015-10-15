package com.bladecoder.engine.actions;

public class ActorAnimationRef {
	private final String actor, animation;

	public ActorAnimationRef() {
		this(null, null);
	}

	public ActorAnimationRef(String sceneId, String actorId) {
		this.actor = sceneId;
		this.animation = actorId;
	}

	public String getActorId() {
		return actor;
	}

	public String getAnimationId() {
		return animation;
	}
}

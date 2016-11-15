package com.bladecoder.engine.actions;

public class ActorAnimationRef {
	private final String actor, animation;

	public ActorAnimationRef() {
		this(null, null);
	}

	public ActorAnimationRef(String actorAnimation) {
		if (actorAnimation == null) {
			actor = null;
			animation = null;
			return;
		}

		int idx = actorAnimation.indexOf(Param.STRING_PARAM_SEPARATOR.charAt(0));

		if (idx != -1) {
			actor = actorAnimation.substring(0, idx);
			animation = actorAnimation.substring(idx + 1);
		} else {
			actor = null;
			animation = actorAnimation;
		}
	}

	public ActorAnimationRef(String actorId, String animationId) {
		this.actor = actorId;
		this.animation = animationId;
	}

	public String getActorId() {
		return actor;
	}

	public String getAnimationId() {
		return animation;
	}

	public String toString() {
		if (actor == null || actor.isEmpty())
			return animation;

		return actor + Param.STRING_PARAM_SEPARATOR + animation;
	}
}

package com.bladecoder.engine.anim;

import com.badlogic.gdx.math.Vector2;

public class AnimationDesc {
	public String id;
	public String source;
	public float duration;
	public Vector2 inD;
	public Vector2 outD;
	public Tween.Type animationType;
	public int count;

	public String sound;

	public boolean preload;
	public boolean disposeWhenPlayed;

	public AnimationDesc() {

	}

	public void set(String id, String source, float duration, int count, Tween.Type animationType,
			String sound, Vector2 inD, Vector2 outD, boolean preload, boolean disposeWhenPlayed) {
		this.id = id;
		this.duration = duration;
		this.animationType = animationType;
		this.count = count;

		this.source = source;
		this.sound = sound;

		this.inD = inD;
		this.outD = outD;

		this.preload = preload;
		this.disposeWhenPlayed = disposeWhenPlayed;
	}
}

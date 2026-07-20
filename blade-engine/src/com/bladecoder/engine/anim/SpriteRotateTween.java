package com.bladecoder.engine.anim;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.InterpolationMode;

/**
 * Tween for spriteactor scale animation
 */
public class SpriteRotateTween extends Tween<SpriteActor> {
	
	private float startRot;
	private float targetRot;
	
	public SpriteRotateTween() {
	}

	public void start(SpriteActor target, Type repeatType, int count, float tRot, float duration, InterpolationMode interpolation, ActionCallback cb) {
		this.target = target;
		
		startRot = target.getRot();
		targetRot = tRot;
		
		setDuration(duration);
		setType(repeatType);
		setCount(count);
		setInterpolation(interpolation);

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}

	@Override
	public void updateTarget() {
		target.setRot(startRot + getPercent() * (targetRot - startRot));
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startRot", startRot);
		json.writeValue("targetRot", targetRot);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		startRot = json.readValue("startRot", Float.class, jsonData);
		targetRot = json.readValue("targetRot", Float.class, jsonData);
	}
}

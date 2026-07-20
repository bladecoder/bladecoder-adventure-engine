package com.bladecoder.engine.anim;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.InterpolationMode;

/**
 * Tween for spriteactor scale animation
 */
public class SpriteScaleTween extends Tween<SpriteActor> {

	private float startSclX, startSclY;
	private float targetSclX, targetSclY;

	public SpriteScaleTween() {
	}

	public void start(SpriteActor target, Type repeatType, int count, float tSclX, float tSclY, float duration,
			InterpolationMode interpolation, ActionCallback cb) {
		this.target = target;

		startSclX = target.getScaleX();
		startSclY = target.getScaleY();
		targetSclX = tSclX;
		targetSclY = tSclY;

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
		float percent = getPercent();
		
		target.setScale(startSclX + percent * (targetSclX - startSclX),
				startSclY + percent * (targetSclY - startSclY));
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startSclX", startSclX);
		json.writeValue("startSclY", startSclY);
		
		json.writeValue("targetSclX", targetSclX);
		json.writeValue("targetSclY", targetSclY);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		startSclX = json.readValue("startSclX", Float.class, jsonData);
		startSclY = json.readValue("startSclY", Float.class, jsonData);
		
		targetSclX = json.readValue("targetSclX", Float.class, jsonData);
		targetSclY = json.readValue("targetSclY", Float.class, jsonData);
	}
}

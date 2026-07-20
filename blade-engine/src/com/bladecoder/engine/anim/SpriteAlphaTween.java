package com.bladecoder.engine.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.InterpolationMode;

/**
 * Tween for SpriteActor alpha animation
 */
public class SpriteAlphaTween extends Tween<SpriteActor> {

	private float startAlpha;
	private float targetAlpha;

	public SpriteAlphaTween() {
	}

	public void start(SpriteActor target, Type repeatType, int count, float tAlpha, float duration,
			InterpolationMode interpolation, ActionCallback cb) {

		setTarget(target);

		if (target.getTint() == null) {
			target.setTint(Color.WHITE.cpy());
		} else {
			// to set the flag dirty
			target.setTint(target.getTint());
		}

		startAlpha = target.getTint().a;
		targetAlpha = tAlpha;

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
		target.getTint().a = startAlpha + getPercent() * (targetAlpha - startAlpha);
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startAlpha", startAlpha);
		json.writeValue("targetAlpha", targetAlpha);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		startAlpha = json.readValue("startAlpha", float.class, 1.0f, jsonData);
		targetAlpha = json.readValue("targetAlpha", float.class, 1.0f, jsonData);
	}
}

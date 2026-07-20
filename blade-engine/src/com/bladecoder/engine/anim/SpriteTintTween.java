package com.bladecoder.engine.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.InterpolationMode;

/**
 * Tween for SpriteActor tint animation
 */
public class SpriteTintTween extends Tween<SpriteActor> {

	private Color startColor;
	private Color targetColor;

	public SpriteTintTween() {
	}

	public void start(SpriteActor target, Type repeatType, int count, Color tColor, float duration,
			InterpolationMode interpolation, ActionCallback cb) {

		setTarget(target);

		if (target.getTint() == null) {
			target.setTint(Color.WHITE.cpy());
		} else {
			// to set the flag dirty
			target.setTint(target.getTint());
		}

		startColor = target.getTint().cpy();
		targetColor = tColor.cpy();

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

		target.getTint().a = startColor.a + getPercent() * (targetColor.a - startColor.a);
		target.getTint().r = startColor.r + getPercent() * (targetColor.r - startColor.r);
		target.getTint().g = startColor.g + getPercent() * (targetColor.g - startColor.g);
		target.getTint().b = startColor.b + getPercent() * (targetColor.b - startColor.b);
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startColor", startColor);
		json.writeValue("targetColor", targetColor);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		startColor = json.readValue("startColor", Color.class, Color.WHITE, jsonData);
		targetColor = json.readValue("targetColor", Color.class, Color.WHITE, jsonData);
	}
}

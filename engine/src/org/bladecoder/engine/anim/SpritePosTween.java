package org.bladecoder.engine.anim;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;

import aurelienribon.tweenengine.TweenCallback;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for frame animation
 */
public class SpritePosTween extends EngineTween implements
		Serializable {
	
	protected SpriteActor target;
	
	public SpritePosTween() {
		type = EngineTween.SPRITE_POS_TYPE;
		
		startValues = new float[2];
		targetValues = new float[2];
		valueBuffer = new float[2];
	}

	public void start(SpriteActor target, int repeatType, int count, Vector2 targetPos, float duration, ActionCallback cb) {
		
		Vector2 currentPos = target.getPosition();
		
		startValues[0] = currentPos.x;
		startValues[1] = currentPos.y;
		targetValues[0] = targetPos.x;
		targetValues[1] = targetPos.y;
		
		this.target = target;
		this.duration = duration;

		if (cb != null) {
			this.cb = cb;
			setCallback(tweenCb);
			setCallbackTriggers(TweenCallback.COMPLETE);
		}

		switch (repeatType) {
		case REPEAT:
			repeat(count, 0);
			break;
		case YOYO:
			repeatYoyo(count, 0);
			break;

		}

		start();
	}

	@Override
	protected void setValues(float[] values) {
		target.setPosition(values[0], values[1]);
	}
	
	@Override
	protected Object getTarget() {
		return target;
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("targetId", target.getId());
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		String targetId = json
					.readValue("targetId", String.class, jsonData);
		target = (SpriteActor) World.getInstance().getCurrentScene()
					.getActor(targetId);
	}
}

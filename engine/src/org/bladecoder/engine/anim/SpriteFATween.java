package org.bladecoder.engine.anim;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.SpriteAtlasActor;
import org.bladecoder.engine.model.World;

import aurelienribon.tweenengine.TweenCallback;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for frame animation
 */
public class SpriteFATween extends EngineTween implements
		Serializable {
	
	protected boolean reverse;
	protected SpriteAtlasActor target;
	
	public SpriteFATween() {
		type = EngineTween.SPRITE_FA_TYPE;
		
		startValues = new float[1];
		targetValues = new float[1];
		valueBuffer = new float[1];
	}

	public void start(SpriteAtlasActor target, int repeatType, int count, boolean reverse, ActionCallback cb) {
		
		FrameAnimation fa = target.getCurrentFrameAnimation();
		
		this.target = target;
		this.duration = fa.speed;
		this.reverse = reverse;
		
		startValues[0] = getInitValue();
		targetValues[0] = getEndValue();

		if (cb != null) {
			this.cb = cb;
			setCallback(tweenCb);
			setCallbackTriggers(TweenCallback.COMPLETE);
		}
		
		int r = fa.animationType;
		int c = fa.count;
		
		if (repeatType != EngineTween.REPEAT_DEFAULT) {
			r = repeatType;
			c = count;
		}

		switch (r) {
		case REPEAT:
			repeat(c, 0);
			break;
		case YOYO:
			repeatYoyo(c, 0);
			break;

		}

		start();
	}
	

	private float getInitValue() {
		return reverse ? target.getNumFrames() - 1f : 0;
	}

	private float getEndValue() {
		return reverse ? 0 : target.getNumFrames() - 0.01f;
	}

	@Override
	protected void setValues(float[] values) {
		target.setCurrentFrame((int) values[0]);
	}
	
	@Override
	protected Object getTarget() {
		return target;
	}
	
	@Override
	public void write(Json json) {
		super.write(json);
		
		json.writeValue("reverse", reverse);
		json.writeValue("targetId", target.getId());
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);
		
		reverse = json.readValue("reverse", Boolean.class, jsonData);		
		

		String targetId = json
					.readValue("targetId", String.class, jsonData);
		target = (SpriteAtlasActor) World.getInstance().getCurrentScene()
					.getActor(targetId);
	}	
}

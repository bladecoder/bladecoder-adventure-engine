package org.bladecoder.engine.anim;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.model.SceneCamera;

import aurelienribon.tweenengine.TweenCallback;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for frame animation
 */
public class CameraTween extends EngineTween implements
		Serializable {
	
	protected SceneCamera target;
	
	public CameraTween() {
		type = EngineTween.CAMERA_TYPE;
		
		startValues = new float[3];
		targetValues = new float[3];
		valueBuffer = new float[3];
	}

	public void start( int repeatType, int count, Vector2 targetPos, float targetZoom, float duration, ActionCallback cb) {
		
		target = World.getInstance().getSceneCamera();
		
		Vector2 currentPos = target.getPosition();
		
		startValues[0] = currentPos.x;
		startValues[1] = currentPos.y;
		startValues[2] = target.getZoom();
		targetValues[0] = targetPos.x;
		targetValues[1] = targetPos.y;
		startValues[2] = targetZoom;
		
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
		target.setZoom(values[2]);
	}
	
	@Override
	protected Object getTarget() {
		return target;
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		target = World.getInstance().getSceneCamera();
	}
}

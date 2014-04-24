package org.bladecoder.engine.anim;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.util.ActionCallbackSerialization;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween to support animations in the engine. The Tween standard class cannot be used because serialization
 */
public abstract class EngineTween extends BaseTween<EngineTween> implements
		Serializable {
	public final static int SPRITE_FA_TYPE = 1;
	public final static int SPRITE_POS_TYPE = 2;
	public final static int CAMERA_TYPE = 3;
	public final static int WALK_TYPE = 4;

	public final static int NO_REPEAT = 0;
	public final static int REPEAT = 1;
	public final static int YOYO = 2;
	public final static int FROM_FA = 3;
	public final static int REVERSE = 4;
	public final static int REVERSE_REPEAT = 5;

	protected float savedTime;
	protected ActionCallback cb;
	protected int type;

	protected float[] startValues;
	protected float[] targetValues;
	protected float[] valueBuffer;
	
	protected TweenEquation equation = Linear.INOUT;
	
	public void resumeSaved() {
		forceStartValues();
		update(savedTime);
	}

	@Override
	protected void updateOverride(int step, int lastStep, boolean isIterationStep, float delta) {
		if (getTarget() == null || equation == null) return;

		// Case iteration end has been reached

		if (!isIterationStep && step > lastStep) {
			setValues(isReverse(lastStep) ? startValues : targetValues);
			
			return;
		}

		if (!isIterationStep && step < lastStep) {
			setValues(isReverse(lastStep) ? targetValues : startValues);
			return;
		}

		// Validation

		assert isIterationStep;
		assert getCurrentTime() >= 0;
		assert getCurrentTime() <= duration;

		// Case duration equals zero

		if (duration < 0.00000000001f && delta > -0.00000000001f) {
			setValues(isReverse(step) ? targetValues : startValues);
			return;
		}

		if (duration < 0.00000000001f && delta < 0.00000000001f) {
			setValues(isReverse(step) ? startValues : targetValues);
			return;
		}

		// Normal behavior

		float time = isReverse(step) ? duration - getCurrentTime() : getCurrentTime();
		float t = equation.compute(time/duration);

			for (int i=0; i< startValues.length; i++) {
				valueBuffer[i] = startValues[i] + t * (targetValues[i] - startValues[i]);
			}

		setValues(valueBuffer);
	}

	protected final TweenCallback tweenCb = new TweenCallback() {
		@Override
		public void onEvent(int type, BaseTween<?> source) {

			if (cb != null) {
				cb.onEvent();
			}
		}
	};

	protected abstract void setValues(float[] values);
	protected abstract Object getTarget();

	@Override
	protected void forceStartValues() {
		if (getTarget() == null) return;
		setValues(startValues);
	}

	@Override
	protected void forceEndValues() {
		if (getTarget() == null) return;
		setValues(targetValues);
	}

	@Override
	protected boolean containsTarget(Object target) {
		return this.getTarget() == target;
	}

	@Override
	protected boolean containsTarget(Object target, int tweenType) {
		return this.getTarget() == target && this.type == tweenType;
	}

	@Override
	public void write(Json json) {
		json.writeValue("startValues", startValues);
		json.writeValue("targetValues", targetValues);
		json.writeValue("count", getRepeatCount());
		json.writeValue("yoyo", isYoyo());
		json.writeValue("duration", duration);
		json.writeValue("currentTime", getCurrentTime());
		json.writeValue("hasCb", cb != null ? true : false);
		json.writeValue("cb", ActionCallbackSerialization.find(cb),
				cb == null ? null : String.class);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		startValues = json.readValue("startValues", float[].class, jsonData);
		targetValues = json.readValue("targetValues", float[].class, jsonData);
		
		valueBuffer = new float[startValues.length];
		
		
		int count = json.readValue("count", Integer.class, jsonData);
		boolean yoyo = json.readValue("yoyo", Boolean.class, jsonData);	
		
		if(yoyo) 
			repeatYoyo(count, 0);
		else if(count > 0)
			repeat(count, 0);
		
		duration = json.readValue("duration", Float.class, jsonData);
		savedTime = json.readValue("currentTime", Float.class, jsonData);		

		String cbSer = json.readValue("cb", String.class, jsonData);
		cb = ActionCallbackSerialization.find(cbSer);
		
		if (cb != null) {
			setCallback(tweenCb);
			setCallbackTriggers(TweenCallback.COMPLETE);
		}
	}

}

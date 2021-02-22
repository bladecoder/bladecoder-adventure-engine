/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.anim;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.util.InterpolationMode;

abstract public class Tween<T> implements Serializable {
	public enum Type {
		NO_REPEAT, REPEAT, YOYO, REVERSE, REVERSE_REPEAT, SPRITE_DEFINED;
	}

	public final static int INFINITY = -1;

	private float duration, time;
	private InterpolationMode interpolation;
	private boolean reverse, began, complete;
	private Type type;
	private int count;

	private ActionCallback cb;

	protected T target;

	public Tween() {
	}

	public void update(float delta) {
		if (complete)
			return;

		if (!began) {
			began = true;
		}

		time += delta;

		if (time >= duration) {
			if (type == Type.NO_REPEAT || type == Type.REVERSE || count == 1) {
				complete = true;
			} else if (count != 1) {
				complete = false;
				count--;
				time = 0.00001f;

				if (type == Type.YOYO)
					reverse = !reverse;
			}
		}

		updateTarget();

		if (complete) {
			callCb();
		}
	}

	/**
	 * Called to update the target property.
	 */
	abstract protected void updateTarget();

	private void callCb() {
		if (cb != null) {
			ActionCallback tmpcb = cb;
			cb = null;
			tmpcb.resume();
		}
	}

	public void setTarget(T t) {
		target = t;
	}

	public T getTarget() {
		return target;
	}

	public float getPercent() {
		return getPercent(interpolation);
	}

	public float getPercent(InterpolationMode i) {
		float percent;
		if (complete) {
			percent = 1;
		} else {
			percent = time / duration;
			if (i != null)
				percent = i.getInterpolation().apply(percent);
		}

		return (reverse ? 1 - percent : percent);
	}

	/** Skips to the end of the transition. */
	public void finish() {
		time = duration;
	}

	public void restart() {
		time = 0;
		began = false;
		complete = false;
	}

	public void reset() {
		reverse = false;
		interpolation = null;
	}

	/** Gets the transition time so far. */
	public float getTime() {
		return time;
	}

	/** Sets the transition time so far. */
	public void setTime(float time) {
		this.time = time;
	}

	public float getDuration() {
		return duration;
	}

	/** Sets the length of the transition in seconds. */
	public void setDuration(float duration) {
		this.duration = duration;
	}

	public void setInterpolation(InterpolationMode i) {
		interpolation = i;
	}

	public boolean isReverse() {
		return reverse;
	}

	/** When true, the action's progress will go from 100% to 0%. */
	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	public void setCb(ActionCallback cb) {
		this.cb = cb;
	}

	public boolean isComplete() {
		return complete;
	}

	public Tween.Type getType() {
		return type;
	}

	public void setType(Tween.Type type) {
		this.type = type;

		if (type == Tween.Type.REVERSE || type == Tween.Type.REVERSE_REPEAT)
			reverse = true;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public void write(Json json) {
		json.writeValue("duration", duration);
		json.writeValue("time", time);
		json.writeValue("reverse", reverse);
		json.writeValue("began", began);
		json.writeValue("complete", complete);
		json.writeValue("type", type);
		json.writeValue("count", count);

		json.writeValue("interpolation", interpolation);

		if (cb != null) {
			World w = ((BladeJson) json).getWorld();
			Scene s = ((BladeJson) json).getScene();
			json.writeValue("cb", ActionCallbackSerializer.serialize(w, s, cb));
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		duration = json.readValue("duration", Float.class, jsonData);
		time = json.readValue("time", Float.class, jsonData);

		reverse = json.readValue("reverse", Boolean.class, jsonData);
		began = json.readValue("began", Boolean.class, jsonData);
		complete = json.readValue("complete", Boolean.class, jsonData);
		type = json.readValue("type", Type.class, jsonData);
		count = json.readValue("count", Integer.class, jsonData);

		interpolation = json.readValue("interpolation", InterpolationMode.class, jsonData);

		BladeJson bjson = (BladeJson) json;
		cb = ActionCallbackSerializer.find(bjson.getWorld(), bjson.getScene(),
				json.readValue("cb", String.class, jsonData));
	}
}

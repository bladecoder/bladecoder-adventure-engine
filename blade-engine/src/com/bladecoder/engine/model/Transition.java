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
package com.bladecoder.engine.model;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.util.RectangleRenderer;

/**
 * A transition is used to fadein/fadeout the screen.
 * 
 * @author rgarcia
 */
public class Transition implements Serializable {
	public static enum Type {
		NONE, FADE_IN, FADE_OUT
	};

	private float time;
	private float currentTime;
	private ActionCallback cb;
	private Color c;
	private Type type = Type.NONE;

	public void update(float delta) {

		if (isFinish()) {
			// reset the transition when finish. Only in 'fade in' case, 'fade out'
			// must stay in screen even when finished
			if (type == Type.FADE_IN)
				reset();

			if (cb != null) {
				ActionCallback tmpcb = cb;
				cb = null;
				tmpcb.resume();
			}
		} else {
			currentTime += delta;
		}
	}

	public void reset() {
		type = Type.NONE;
	}

	public void draw(SpriteBatch batch, float width, float height) {

		if (type == Type.NONE)
			return;

		switch (type) {
		case FADE_IN:
			c.a = MathUtils.clamp(Interpolation.fade.apply(1 - currentTime / time), 0, 1);
			break;
		case FADE_OUT:
			c.a = MathUtils.clamp(Interpolation.fade.apply(currentTime / time), 0, 1);
			break;
		default:
			break;
		}

		RectangleRenderer.draw(batch, 0, 0, width, height, c);
	}

	public void create(float time, Color color, Type type, ActionCallback cb) {
		this.currentTime = 0f;
		this.c = color.cpy();
		this.type = type;
		this.time = time;
		this.cb = cb;
	}

	public boolean isFinish() {
		return (currentTime > time || type == Type.NONE);
	}

	@Override
	public void write(Json json) {
		json.writeValue("currentTime", currentTime);
		json.writeValue("time", time);
		json.writeValue("color", c);
		json.writeValue("type", type);

		if (cb != null) {
			World w = ((BladeJson) json).getWorld();
			Scene s = ((BladeJson) json).getScene();
			json.writeValue("cb", ActionCallbackSerializer.serialize(w, s, cb));
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		currentTime = json.readValue("currentTime", Float.class, jsonData);
		time = json.readValue("time", Float.class, jsonData);
		c = json.readValue("color", Color.class, jsonData);
		type = json.readValue("type", Type.class, jsonData);
		BladeJson bjson = (BladeJson) json;
		cb = ActionCallbackSerializer.find(bjson.getWorld(), bjson.getScene(),
				json.readValue("cb", String.class, jsonData));
	}
}

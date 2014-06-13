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
package org.bladecoder.engine.model;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.util.ActionCallbackSerialization;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/** 
 * A transition is used to fadein/fadeout the screen.
 * 
 * @author rgarcia
 */
public class Transition implements Serializable {
	public static enum Type {FADE_IN, FADE_OUT};
	
	private float time;
	private float currentTime;
	private ActionCallback cb;
	private Color c;
	private Type type;
	
	public void update(float delta) {
		currentTime += delta;
		
		if(isFinish() && cb != null) {
			ActionCallbackQueue.add(cb);
			cb = null;
		}
	}
	
	public void draw(SpriteBatch batch, float width, float height) {
		
		if(isFinish()) return;
		
		switch(type) {
		case FADE_IN:
			c.a = 1 - currentTime / time;
			break;
		case FADE_OUT:
			c.a = currentTime / time;
			break;
		default:
			break;
		}
		
		RectangleRenderer.draw(batch, 0, 0, width, height, c);
	}
	
	public void create(float time, Color color, Type type, ActionCallback cb) {
		this.c = color;
		this.type = type;
		this.time = time;
		this.cb = cb;
	}
	
	public boolean isFinish() {
		return (currentTime > time);
	}	
	
	@Override
	public void write(Json json) {	
		json.writeValue("currentTime", currentTime);
		json.writeValue("time", time);
		json.writeValue("color", c);
		json.writeValue("type", type);
		json.writeValue("cb", ActionCallbackSerialization.find(cb), cb == null ? null : String.class);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		currentTime = json.readValue("currentTime", Float.class, jsonData);
		time = json.readValue("time", Float.class, jsonData);
		c = json.readValue("color", Color.class, jsonData);
		type = json.readValue("type", Type.class, jsonData);
		String cbSer = json.readValue("cb", String.class, jsonData);
		if(cbSer != null)
			cb = ActionCallbackSerialization.find(cbSer);
	}
}

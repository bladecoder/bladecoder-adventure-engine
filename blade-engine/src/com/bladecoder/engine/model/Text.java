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
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.util.ActionCallbackSerialization;

public class Text implements Serializable {
	private static final float DEFAULT_TIME = 1f;
	
	public enum Type {
		PLAIN, SUBTITLE, TALK
	};
	
	public String str;
	public float x;
	public float y;
	public float time;
	public Type type;
	public Color color;
	public String style;
	private ActionCallback cb;
	public String actorId;
	public String voiceId;

	public Text() {
	}

	public Text(String str, float x, float y, float time, Type type, Color color, String style, String actorId, String voiceId, ActionCallback cb) {
		this.str = str;
		this.x = x;
		this.y = y;
		this.time = time;
		this.type = type;
		this.color = color;
		this.style = style;
		this.cb = cb;
		this.actorId = actorId;
		this.voiceId = voiceId;

		// 0s -> Auto duration
		// <0 -> Infinity
		
		if(this.time < 0 || voiceId != null) {
			this.time = Float.MAX_VALUE;
		} else if (this.time == 0) {
			setAutoTime();
		}
	}
	
	public void setAutoTime() {
		this.time = DEFAULT_TIME + DEFAULT_TIME * str.length() / 20f;
	}
	
	public void callCb() {
		if (cb != null) {
			ActionCallbackQueue.add(cb);
		}
	}
	
	@Override
	public void write(Json json) {	
		
		json.writeValue("str", str);
		json.writeValue("x", x);
		json.writeValue("y", y);
		json.writeValue("time",time);
		json.writeValue("type", type);
		json.writeValue("color", color);
		json.writeValue("style", style);
		json.writeValue("actorId", actorId);
		json.writeValue("voiceId", voiceId);
		json.writeValue("cb", ActionCallbackSerialization.find(cb), cb == null ? null
				: String.class);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		str = json.readValue("str", String.class, jsonData);
		x = json.readValue("x", Float.class, jsonData);
		y = json.readValue("y", Float.class, jsonData);
		time = json.readValue("time", Float.class, jsonData);
		type = json.readValue("type", Type.class, jsonData);
		color = json.readValue("color", Color.class, jsonData);
		style = json.readValue("style", String.class, jsonData);
		actorId = json.readValue("actorId", String.class, jsonData);
		voiceId = json.readValue("voiceId", String.class, jsonData);
		String cbSer = json.readValue("cb", String.class, jsonData);
		if(cbSer != null)
			cb = ActionCallbackSerialization.find(cbSer);
	}	
}

package org.bladecoder.engine.model;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.util.ActionCallbackSerialization;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Text implements Serializable {
	private static final float DEFAULT_TIME = 1f;
	
	public enum Type {
		PLAIN, RECTANGLE, TALK
	};
	
	public String str;
	public float x;
	public float y;
	public float time;
	public Type type;
	public Color color;
	private ActionCallback cb;

	public Text() {
	}

	public Text(String str, float x, float y, float time, Type type, Color color, ActionCallback cb) {
		this.str = str;
		this.x = x;
		this.y = y;
		this.time = time;
		this.type = type;
		this.color = color;
		this.cb = cb;

		if (this.time <= 0) {
			this.time = DEFAULT_TIME + DEFAULT_TIME * str.length() / 20f;
		}
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
		String cbSer = json.readValue("cb", String.class, jsonData);
		if(cbSer != null)
			cb = ActionCallbackSerialization.find(cbSer);
	}	
}

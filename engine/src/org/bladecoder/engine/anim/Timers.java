package org.bladecoder.engine.anim;

import java.util.ArrayList;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.util.ActionCallbackSerialization;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Timers {
	
	ArrayList<Timer> timers = new ArrayList<Timer>();

	public void addTimer(float time, ActionCallback cb) {
		Timer t = new Timer();
		
		t.time = time;
		t.cb = cb;
		
		timers.add(t);		
	}
	
	public void clear() {
		timers.clear();
	}
	
	public void update(float delta) {
		
		for(int i = 0; i < timers.size(); i++) {
			Timer t = timers.get(i);
			
			t.currentTime += delta;
			
			if(t.currentTime >= t.time) {
				timers.remove(i);
				ActionCallbackQueue.add(t.cb);			
				i--;
			}
		}
	}
	
	static class Timer implements Serializable {
		float time;
		float currentTime = 0;
		ActionCallback cb;
		
		@Override
		public void write(Json json) {	
			json.writeValue("time", time);
			json.writeValue("currentTime", currentTime);	
			json.writeValue("cb", ActionCallbackSerialization.find(cb), cb == null ? null : String.class);	
		}

		@Override
		public void read (Json json, JsonValue jsonData) {
			time = json.readValue("time", Float.class, jsonData);
			currentTime = json.readValue("currentTime", Float.class, jsonData);
			String cbSer = json.readValue("cb", String.class, jsonData);
			cb = ActionCallbackSerialization.find(cbSer);
		}		
	}
}

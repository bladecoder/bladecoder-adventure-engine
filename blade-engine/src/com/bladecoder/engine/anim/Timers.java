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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.util.ActionCallbackSerialization;

public class Timers {
	private List<Timer> timers = new ArrayList<>();

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
		final Iterator<Timer> it = timers.iterator();
		while (it.hasNext()) {
			final Timer t = it.next();

			t.currentTime += delta;

			if (t.currentTime >= t.time) {
				it.remove();
				ActionCallbackQueue.add(t.cb);
			}
		}
	}

	private static class Timer implements Serializable {
		private float time;
		private float currentTime = 0;
		private ActionCallback cb;
		
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

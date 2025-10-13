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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;

public class Timers {
	private final List<Timer> timers = new ArrayList<>(3);
	private final transient List<Timer> timersTmp = new ArrayList<>(3);

	public void addTimer(float time, ActionCallback cb) {
		Timer t = new Timer();

		t.time = time;
		t.cb = cb;

		timers.add(t);
	}

	public void clear() {
		timers.clear();
	}

	public boolean isEmpty() {
		return timers.isEmpty();
	}

	public void removeTimerWithCb(ActionCallback cb) {
		final Iterator<Timer> it = timers.iterator();

		while (it.hasNext()) {
			final Timer t = it.next();
			if (t.cb == cb) {
				it.remove();

				return;
			}
		}
	}

	public void update(float delta) {
		final Iterator<Timer> it = timers.iterator();
		while (it.hasNext()) {
			final Timer t = it.next();

			t.currentTime += delta;

			if (t.currentTime >= t.time) {
				it.remove();

				// we add the timers to call the 'cb' later because the 'cb' can add new timers
				// while iterating.
				timersTmp.add(t);
			}
		}

		if (!timersTmp.isEmpty()) {
			// process ended timers
			for (Timer t : timersTmp) {
				// t.cb can be null if the cb is not found when loading. This can happen because InkManager ended the verb.
				if(t.cb != null)
					t.cb.resume();
			}

			timersTmp.clear();
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

			if (cb != null) {
				World w = ((BladeJson) json).getWorld();
				Scene s = ((BladeJson) json).getScene();
				json.writeValue("cb", ActionCallbackSerializer.serialize(w, s, cb));
			}
		}

		@Override
		public void read(Json json, JsonValue jsonData) {
			time = json.readValue("time", Float.class, jsonData);
			currentTime = json.readValue("currentTime", Float.class, jsonData);

			BladeJson bjson = (BladeJson) json;
			cb = ActionCallbackSerializer.find(bjson.getWorld(), bjson.getScene(),
					json.readValue("cb", String.class, jsonData));
		}
	}
}

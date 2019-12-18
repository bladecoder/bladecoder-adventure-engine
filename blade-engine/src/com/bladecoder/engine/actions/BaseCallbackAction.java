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
package com.bladecoder.engine.actions;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;

public abstract class BaseCallbackAction implements Action, ActionCallback, Serializable {
	private ActionCallback verbCb;

	// Depending on the reading order of Inventory, InkManager and Actor verbs,
	// the verbCallbacks may not exist. So, we search the Cb lazily when needed.
	private String sCb;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	protected World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public void resume() {
		if (verbCb != null || sCb != null) {

			if (verbCb == null) {
				verbCb = ActionCallbackSerializer.find(w, w.getCurrentScene(), sCb);
			}

			ActionCallback cb2 = verbCb;
			verbCb = null;
			sCb = null;
			cb2.resume();
		}
	}

	public void setVerbCb(ActionCallback cb) {
		verbCb = cb;
		sCb = null;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}

	public boolean getWait() {
		return wait;
	}

	@Override
	public void write(Json json) {
		if (verbCb != null) {
			World w = ((BladeJson) json).getWorld();
			Scene s = ((BladeJson) json).getScene();
			json.writeValue("cb", ActionCallbackSerializer.find(w, s, verbCb));
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		sCb = json.readValue("cb", String.class, jsonData);
	}
}

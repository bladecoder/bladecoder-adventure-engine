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
import com.bladecoder.engine.common.ActionCallbackSerialization;
import com.badlogic.gdx.utils.JsonValue;

public abstract class BaseCallbackAction implements Action, ActionCallback, Serializable {	
	private ActionCallback verbCb;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	@Override
	public void resume() {
		if(verbCb != null) {			
			ActionCallback cb2 = verbCb;
			verbCb = null;
			cb2.resume();
		}
	}
	
	public void setVerbCb(ActionCallback cb) {
		this.verbCb = cb;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}
	
	public boolean getWait() {
		return wait;
	}

	@Override
	public void write(Json json) {
		json.writeValue("cb", ActionCallbackSerialization.find(verbCb));	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {		
		verbCb = ActionCallbackSerialization.find(json.readValue("cb", String.class, jsonData));
	}
}

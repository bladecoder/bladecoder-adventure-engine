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

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.util.ActionCallbackSerialization;

public abstract class BaseCallbackAction implements Action, ActionCallback, Serializable {	
	private ActionCallback verbCb;
	private String verbCbSer;
	private boolean wait = true;	
	
	@Override
	public void resume() {
		if(verbCb != null || verbCbSer != null) {
			if(verbCb==null) {
				verbCb =  ActionCallbackSerialization.find(verbCbSer);
				verbCbSer = null;
			}
			
			ActionCallback cb2 = verbCb;
			verbCb = null;
			cb2.resume();
		}
	}

	@Override
	public boolean waitForFinish(ActionCallback cb) {
		this.verbCb = cb;
		
		return wait;
	}	

	public void setWait(boolean wait) {
		this.wait = wait;
	}
	
	public boolean getWait() {
		return wait;
	}

	@Override
	public void write(Json json) {
		json.writeValue("wait", wait);
		
		if(verbCbSer != null)
			json.writeValue("cb", verbCbSer);
		else
			json.writeValue("cb", ActionCallbackSerialization.find(verbCb), verbCb == null ? null : String.class);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		wait = json.readValue("wait", Boolean.class, jsonData);
		
		verbCbSer = json.readValue("cb", String.class, jsonData);
	}
}

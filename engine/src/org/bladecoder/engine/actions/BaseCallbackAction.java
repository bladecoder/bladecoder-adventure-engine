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
package org.bladecoder.engine.actions;

import org.bladecoder.engine.util.ActionCallbackSerialization;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class BaseCallbackAction implements ActionCallback, ActionEndTrigger, Serializable {	
	private ActionCallback cb;
	private String cbSer;

	@Override
	public void onEvent() {
		if(cb != null || cbSer != null) {
			if(cb==null) {
				cb =  ActionCallbackSerialization.find(cbSer);
				cbSer = null;
			}
			
			ActionCallback cb2 = cb;
			cb = null;
			cb2.onEvent();
		}
	}

	@Override
	public void setCallback(ActionCallback cb) {
		this.cb = cb;
	}	

	@Override
	public void write(Json json) {		
		json.writeValue("cb", ActionCallbackSerialization.find(cb), cb == null ? null : String.class);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {	
		cbSer = json.readValue("cb", String.class, jsonData);
	}
}

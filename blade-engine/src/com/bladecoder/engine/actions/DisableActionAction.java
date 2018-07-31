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

import java.io.StringWriter;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;

/**
 * This action wraps an action that has been disabled.
 * 
 * @author rgarcia
 */
@ActionDescription("Helper action to allow disabled actions.")
public class DisableActionAction implements Action {
	@ActionProperty(required = true)
	private String serializedAction;
	
	private Action action;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		return false;
	}

	public void setAction(Action a) {
		action = a;
		Json json = new Json();
		StringWriter buffer = new StringWriter();
		json.setWriter(buffer);
		ActionUtils.writeJson(a, json);
		serializedAction = buffer.toString();
	}
	
	public Action getAction() {
		if(action == null) {
			Json json = new Json();
			JsonValue root = new JsonReader().parse(serializedAction);
			action =  ActionUtils.readJson(w, json, root);
		}
		
		return action;
	}
}

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
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Execute the actions inside the RunOnce/EndRunOnce only once.")
public class RunOnceAction extends AbstractControlAction implements Serializable {
	boolean executed = false;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		VerbRunner v = (VerbRunner)cb;
		
		if (executed) {
			final int ip = skipControlIdBlock(v.getActions(), v.getIP());

			v.setIP(ip);
		}
		
		executed=true;
		
		return false;
	}

	
	@Override
	public void write(Json json) {
		json.writeValue("executed", executed);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		executed = json.readValue("executed", boolean.class, false, jsonData);
	}
}

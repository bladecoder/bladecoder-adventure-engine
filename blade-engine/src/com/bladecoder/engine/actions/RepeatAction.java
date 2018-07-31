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

@ActionDescription("Repeats the actions inside the Repeat/EndRepeat actions.")
public class RepeatAction extends AbstractControlAction implements Serializable {
	@ActionProperty(required = true, defaultValue = "1")
	@ActionPropertyDescription("Repeat the actions the specified times. -1 to infinity")
	private int repeat = 1;

	private int currentRepeat = 0;
	
	@Override
	public void init(World w) {
	}

	@Override
	public boolean run(VerbRunner cb) {
		VerbRunner v = (VerbRunner)cb;
		
		currentRepeat++;
		
		if(currentRepeat > repeat && repeat >= 0) {
			final int ip = skipControlIdBlock(v.getActions(), v.getIP());

			v.setIP(ip);
			currentRepeat = 0;
		}
		
		return false;
	}
	
	
	@Override
	public void write(Json json) {
		json.writeValue("currentRepeat", currentRepeat);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		currentRepeat = json.readValue("currentRepeat", int.class, 0, jsonData);
	}
}

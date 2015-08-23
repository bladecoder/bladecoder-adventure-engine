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

import java.util.HashMap;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SetCutmode")
@ModelDescription("Set/Unset the cutmode. Also shows/hide the inventory")
public class SetCutmodeAction implements Action {
	@JsonProperty(required = true, defaultValue = "true")
	@JsonPropertyDescription("when 'true' sets the scene in 'cutmode'")
	@ModelPropertyType(Type.BOOLEAN)
	private boolean value = true;

	@Override
	public void setParams(HashMap<String, String> params) {	
		if(params.get("value") != null)
			value = Boolean.parseBoolean(params.get("value"));
	}

	@Override
	public boolean run(ActionCallback cb) {
		World.getInstance().setCutMode(value);
		
		return false;
	}

}

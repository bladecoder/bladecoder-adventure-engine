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
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ModelDescription("Execute the actions inside the If/EndIf if the game propert has the specified value.")
public class IfPropertyAction extends AbstractIfAction {
	@JsonProperty(required = true)
	@JsonPropertyDescription("The property name")
	@ModelPropertyType(Type.STRING)
	private String name;

	@JsonProperty
	@JsonPropertyDescription("The property value")
	@ModelPropertyType(Type.STRING)
	private String value;

	private String caID;

	@Override
	public void setParams(HashMap<String, String> params) {
		name = params.get("name");
		value = params.get("value");

		caID = params.get(XMLConstants.CONTROL_ACTION_ID_ATTR);
	}

	@Override
	public boolean run(ActionCallback cb) {
		String valDest = World.getInstance().getCustomProperty(name); 
		
		if ( (value == null && valDest != null) ||
			 (value != null && !value.equals(valDest))
				) {
			gotoElse((VerbRunner) cb);
		}

		return false;
	}

	@Override
	public String getControlActionID() {
		return caID;
	}
}

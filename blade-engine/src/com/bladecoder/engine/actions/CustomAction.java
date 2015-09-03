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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.TrackPropertyChanges;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.HashMap;

@JsonTypeName("Custom")
@ModelDescription("With this action, you can specify your own code")
public class CustomAction extends AbstractAction {
	@JsonProperty(value = "class", required = true)
	@JsonPropertyDescription("The full class name for this custom action")
	@ModelPropertyType(Type.VECTOR2)
	private String className;

	@Override
	public void setParams(HashMap<String, String> params) {
		className = params.get("class");
	}

	@Override
	public boolean run(ActionCallback cb) {
		return false;
	}

	public String getClassName() {
		return className;
	}

	@TrackPropertyChanges
	public void setClassName(String className) {
		this.className = className;
	}
}

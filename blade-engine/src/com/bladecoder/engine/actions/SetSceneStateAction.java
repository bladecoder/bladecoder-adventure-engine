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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Sets the scene state")
public class SetSceneStateAction implements Action {
	@JsonProperty("scene")
	@JsonPropertyDescription("The scene")
	@ActionPropertyType(Type.SCENE)
	private String sceneId;

	@JsonProperty
	@JsonPropertyDescription("The scene 'state'")
	@ActionPropertyType(Type.STRING)
	private String state;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		sceneId = params.get("scene");
		state = params.get("state");
	}

	@Override
	public boolean run(ActionCallback cb) {			
		Scene s = (sceneId != null && !sceneId.isEmpty())? World.getInstance().getScene(sceneId): World.getInstance().getCurrentScene();
		
		s.setState(state);
		
		return false;
	}


}

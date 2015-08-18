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

@ActionDescription("Sets the scene state")
public class SetSceneStateAction implements Action {
	public static final Param[] PARAMS = {
		new Param("scene", "The scene", Type.SCENE),
		new Param("state", "The scene 'state'", Type.STRING)
		};		
	
	String sceneId;
	String state;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		
		sceneId = params.get("scene");;
		state = params.get("state");
	}

	@Override
	public boolean run(ActionCallback cb) {			
		Scene s = (sceneId != null && !sceneId.isEmpty())? World.getInstance().getScene(sceneId): World.getInstance().getCurrentScene();
		
		s.setState(state);
		
		return false;
	}


	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

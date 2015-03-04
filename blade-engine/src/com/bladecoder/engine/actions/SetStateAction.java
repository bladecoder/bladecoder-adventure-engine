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

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class SetStateAction implements Action {
	public static final String INFO = "Sets the actor state";
	public static final Param[] PARAMS = {
		new Param("actor", "The target actor", Type.SCENE_ACTOR),
		new Param("state", "The actor 'state'", Type.STRING)
		};		
	
	String actorId;
	String sceneId;
	String state;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));
		state = params.get("state");
		
		if(a==null) // Called inside a scene
			return;
		
		sceneId = a[0];
		actorId = a[1];
	}

	@Override
	public boolean run(ActionCallback cb) {			
		Scene s = (sceneId != null && !sceneId.isEmpty())? World.getInstance().getScene(sceneId): World.getInstance().getCurrentScene();
		
		if(actorId == null) { 
			// if called in a scene verb and no actor is specified, set the state of the scene
			s.setState(state);
			return false;
		}
		
		BaseActor a = s.getActor(actorId, false);
		
		if(a == null) { // search in inventory
			a = World.getInstance().getInventory().getItem(actorId);
		}
		
		if(a != null)
			a.setState(state);
		else
			EngineLogger.error("SetState - Actor not found: " + actorId);
		
		return false;
	}


	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

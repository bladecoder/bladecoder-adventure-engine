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
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Sets the actor state")
public class SetStateAction implements Action {
	@JsonProperty("actor")
	@JsonPropertyDescription("The target actor")
	@ActionPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef sceneActorRef;

	@JsonProperty
	@JsonPropertyDescription("The actor 'state'")
	@ActionPropertyType(Type.STRING)
	private String state;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));
		state = params.get("state");

		// If a == null, called inside a scene
		sceneActorRef = a == null ? new SceneActorRef() : new SceneActorRef(a[0], a[1]);
	}

	@Override
	public boolean run(ActionCallback cb) {			
		final Scene s = sceneActorRef.getScene();

		String actorId = sceneActorRef.getActorId();
		if (actorId == null) {
			// if called in a scene verb and no actor is specified, set the state of the scene
			s.setState(state);
			return false;
		}

		InteractiveActor a = (InteractiveActor)s.getActor(actorId, false);
		
		if(a == null) { // search in inventory
			a = World.getInstance().getInventory().getItem(actorId);
		}
		
		if(a != null)
			a.setState(state);
		else
			EngineLogger.error("SetState - Actor not found: " + actorId);
		
		return false;
	}


}

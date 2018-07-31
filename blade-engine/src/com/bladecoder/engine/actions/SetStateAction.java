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

import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription(name="ActorState", value="Sets the actor state")
public class SetStateAction implements Action {
	@ActionProperty(required = true)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	@ActionProperty
	@ActionPropertyDescription("The actor 'state'")
	private String state;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {			
		final Scene s = actor.getScene(w);

		String actorId = actor.getActorId();
		if (actorId == null) {
			// if called in a scene verb and no actor is specified, set the state of the scene
			s.setState(state);
			return false;
		}

		InteractiveActor a = (InteractiveActor)s.getActor(actorId, false);
		
		if(a == null) { // search in inventory
			a = w.getInventory().get(actorId);
		}
		
		if(a != null)
			a.setState(state);
		else
			EngineLogger.error("SetState - Actor not found: " + actorId);
		
		return false;
	}


}

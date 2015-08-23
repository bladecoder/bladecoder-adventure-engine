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
import com.bladecoder.engine.model.ModelTypeLink;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ModelDescription("Play/Stop a sound")
public class SoundAction implements Action {
	private static final String ACTOR_PROPERTY_ID = "actor";

	@JsonProperty(ACTOR_PROPERTY_ID)
	@JsonPropertyDescription("The target actor")
	@ModelPropertyType(Type.ACTOR)
	private String actorId;

	@JsonProperty
	@JsonPropertyDescription("The 'soundId' to play")
	@ModelPropertyType(Type.SOUND)
	@ModelTypeLink(ACTOR_PROPERTY_ID)
	private String play;

	@JsonProperty
	@JsonPropertyDescription("The 'soundId' to stop")
	@ModelPropertyType(Type.SOUND)
	@ModelTypeLink(ACTOR_PROPERTY_ID)
	private String stop;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		play = params.get("play");
		stop = params.get("stop");
	}

	@Override
	public boolean run(ActionCallback cb) {
		
		InteractiveActor actor = (InteractiveActor)World.getInstance().getCurrentScene().getActor(actorId, true);
		
		if(play!= null)	actor.playSound(play);
		
		if(stop!= null)	actor.stopSound(stop);
		
		return false;
	}


}

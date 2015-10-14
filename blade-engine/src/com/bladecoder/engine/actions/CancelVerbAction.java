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
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Cancels a running verb.
 * 
 * @author rgarcia
 */
@ActionDescription("Stops the named verb if it is in execution.")
public class CancelVerbAction implements Action {
	@JsonProperty
	@JsonPropertyDescription("The target actor. Empty for the current actor.")
	@ActionPropertyType(Type.ACTOR)
	private String actor;

	@JsonProperty
	@JsonPropertyDescription("The verb to stop. Empty for the current verb.")
	@ActionPropertyType(Type.STRING)
	private String verb;

	@JsonProperty
	@JsonPropertyDescription("If the verb is 'use', the target actor")
	@ActionPropertyType(Type.ACTOR)
	private String target;

	@Override
	public void setParams(HashMap<String, String> params) {
		actor = params.get("actor");
		verb = params.get("verb");
		target = params.get("target");
	}

	@Override
	public boolean run(ActionCallback cb) {

		VerbRunner v = null;
		
		if(verb == null) {
			v = (VerbRunner)cb;
		}

		if (v == null && actor != null) {
			BaseActor a = World.getInstance().getCurrentScene()
					.getActor(actor, true);
			v = ((InteractiveActor)a).getVerb(verb, target);
		}

		if (v == null) {
			v = World.getInstance().getCurrentScene().getVerb(verb);
		}

		if (v == null) {
			v = World.getInstance().getVerbManager().getVerb(verb, null, null);
		}

		if (v != null)
			v.cancel();
		else
			EngineLogger.error("Cannot find VERB: " + verb + " for ACTOR: " + actor);

		return false;
	}

}

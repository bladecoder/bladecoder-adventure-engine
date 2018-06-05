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
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

/**
 * Cancels a running verb.
 * 
 * @author rgarcia
 */
@ActionDescription("Stops the named verb if it is in execution.")
public class CancelVerbAction implements Action {
	@ActionPropertyDescription("The target actor. Empty for the current actor.")
	@ActionProperty(type=Type.ACTOR)
	private String actor;

	@ActionProperty
	@ActionPropertyDescription("The verb to stop. Empty for the current verb.")
	private String verb;

	@ActionPropertyDescription("If the verb is 'use', the target actor")
	@ActionProperty(type=Type.ACTOR)
	private String target;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		VerbRunner v = null;
		
		if(verb == null) {
			v = (VerbRunner)cb;
		}

		if (v == null && actor != null) {
			BaseActor a = w.getCurrentScene()
					.getActor(actor, true);
			v = ((InteractiveActor)a).getVerb(verb, target);
		}

		if (v == null) {
			v = w.getCurrentScene().getVerb(verb);
		}

		if (v == null) {
			v = w.getVerbManager().getVerb(verb, null, null);
		}

		if (v != null) {
			v.cancel();
			
			// Cancel possible pending timer
			w.getCurrentScene().getTimers().removeTimerWithCb(v);
		} else
			EngineLogger.error("Cannot find VERB: " + verb + " for ACTOR: " + actor);

		return false;
	}

}

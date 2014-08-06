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
package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.World;

/**
 * Cancels a running verb.
 * 
 * @author rgarcia
 */
public class CancelVerbAction implements Action {
	public static final String INFO = "Stops the named verb if it is in execution.";
	public static final Param[] PARAMS = {
		new Param("verb", "The verb to stop", Type.STRING, true), 
		new Param("target", "If the verb is 'use', the target actor", Type.ACTOR)
		};
	
	String actorId;
	String verb;
	String target;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		verb = params.get("verb");
		target = params.get("target");
	}

	@Override
	public void run() {
		if(actorId != null) {
			Actor actor = World.getInstance().getCurrentScene().getActor(actorId, true);
		
			actor.getVerbManager().cancelVerb(verb, actor.getState(), target);
		} else {
			Scene s = World.getInstance().getCurrentScene();
			s.getVerbManager().cancelVerb(verb, s.getState(), target);
		}
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

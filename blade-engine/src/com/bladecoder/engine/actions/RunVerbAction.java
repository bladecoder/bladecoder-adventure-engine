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

import java.text.MessageFormat;
import java.util.ArrayList;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Runs an actor verb")
public class RunVerbAction implements VerbRunner, Action {
	@ActionPropertyDescription("The actor with the verb. If empty, the verb is searched in the scene and in the world.")
	@ActionProperty(type = Type.INTERACTIVE_ACTOR)
	private String actor;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The 'verbId' to run")
	private String verb;

	@ActionProperty
	@ActionPropertyDescription("Aditional actor for 'use' verb")
	private String target;
	
	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		run(cb.getCurrentTarget(),  wait?cb:null);
		
		return wait;
	}

	private Verb getVerb() {
		Verb v = null;
		
		Scene s = w.getCurrentScene();

		if (actor != null) {
			InteractiveActor a = (InteractiveActor)s.getActor(actor, true);

			v = a.getVerbManager().getVerb(verb, a.getState(), target);
		}

		if (v == null) {
			v = s.getVerbManager().getVerb(verb, s.getState(), target);
		}

		if (v == null) {
			v = w.getVerbManager().getVerb(verb, null, target);
		}

		if (v == null)
			EngineLogger.error("Cannot find VERB: " + verb + " for ACTOR: " + actor);

		return v;
	}

	@Override
	public void resume() {
		getVerb().resume();
	}

	@Override
	public void cancel() {
		
		// check if the actor has been moved during the execution
		if(actor != null) {
			InteractiveActor a = (InteractiveActor)w.getCurrentScene().getActor(actor, true);
			
			if(a == null)
				return;
		}
		
		
		getVerb().cancel();
	}

	@Override
	public String getCurrentTarget() {
		return getVerb().getCurrentTarget();
	}

	@Override
	public ArrayList<Action> getActions() {
		Verb v = getVerb();

		if (v == null) {
			if( actor != null)
				EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for actor ''{1}({3})'' and target ''{2}''.",
					verb, actor, target, ((InteractiveActor)w.getCurrentScene().getActor(actor, true)).getState()));
			else
				EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found.", verb));

			return new ArrayList<Action>(0);
		}

		return v.getActions();
	}

	@Override
	public void run(String currentTarget, ActionCallback cb) {
		Verb v = getVerb();
		
		v.run(currentTarget, cb);
	}

	@Override
	public int getIP() {
		return getVerb().getIP();
	}

	@Override
	public void setIP(int ip) {
		getVerb().setIP(ip);
	}	

}

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

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Runs an actor verb")
public class RunVerbAction extends BaseCallbackAction implements VerbRunner {
	@ActionPropertyDescription("The actor with the verb. If empty, the verb is searched in the scene and in the world.")
	@ActionProperty(type = Type.INTERACTIVE_ACTOR)
	private String actor;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The 'verbId' to run")
	private String verb;

	@ActionProperty
	@ActionPropertyDescription("Aditional actor for 'use' verb")
	private String target;

	private String state;
	private int ip = -1;
	private String currentTarget;

	@Override
	public boolean run(VerbRunner cb) {
		setVerbCb(cb);
		
		run(cb.getCurrentTarget());
		
		return getWait();
	}

	private Verb getVerb(String verb, String target, String state) {
		Verb v = null;

		if (actor != null) {
			InteractiveActor a = (InteractiveActor)World.getInstance().getCurrentScene().getActor(actor, true);

			v = a.getVerbManager().getVerb(verb, state, target);
		}

		if (v == null) {
			v = World.getInstance().getCurrentScene().getVerb(verb);
		}

		if (v == null) {
			v = World.getInstance().getVerbManager().getVerb(verb, null, null);
		}

		if (v == null)
			EngineLogger.error("Cannot find VERB: " + verb + " for ACTOR: " + actor);

		return v;
	}

	private void nextStep() {

		boolean stop = false;

		ArrayList<Action> actions = getActions();

		while (ip < actions.size() && !stop) {
			Action a = actions.get(ip);

			if (EngineLogger.debugMode())
				EngineLogger.debug("RunVerbAction: " + verb + "(" + ip + ") " + a.getClass().getSimpleName());

			try {
				if (a.run(this))
					stop = true;
				else
					ip++;
			} catch (Exception e) {
				EngineLogger.error("EXCEPTION EXECUTING ACTION: " + a.getClass().getSimpleName(), e);
				ip++;
			}
		}

		if (getWait() && !stop) {
			super.resume();
		}
	}

	@Override
	public void resume() {
		ip++;
		nextStep();
	}

	@Override
	public void cancel() {
		
		// check if the actor has been moved during the execution
		if(actor != null) {
			InteractiveActor a = (InteractiveActor)World.getInstance().getCurrentScene().getActor(actor, true);
			
			if(a == null)
				return;
		}
		
		
		ArrayList<Action> actions = getActions();

		for (Action c : actions) {
			if (c instanceof VerbRunner)
				((VerbRunner) c).cancel();
		}

		ip = actions.size();
	}

	@Override
	public String getCurrentTarget() {
		return currentTarget;
	}

	@Override
	public ArrayList<Action> getActions() {
		Verb v = getVerb(verb, target, state);

		if (v == null) {
			if( actor != null)
				EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for actor ''{1}({3})'' and target ''{2}''.",
					verb, actor, target, ((InteractiveActor)World.getInstance().getCurrentScene().getActor(actor, true)).getState()));
			else
				EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found.", verb));

			return new ArrayList<Action>(0);
		}

		return v.getActions();
	}

	@Override
	public void run(String currentTarget) {
		ip = 0;
		this.currentTarget = currentTarget;
		
		Scene s = World.getInstance().getCurrentScene();

		// Gets the actor/scene state.
		if (actor != null
				&& ((InteractiveActor)s.getActor(actor, true)).getVerb(verb, target) != null) {
			state = ((InteractiveActor)s.getActor(actor, true)).getState();
		} else if (s.getVerb(verb) != null) {
			state = s.getState();
		}

		nextStep();
	}

	@Override
	public int getIP() {
		return ip;
	}

	@Override
	public void setIP(int ip) {
		this.ip = ip;
	}	

	@Override
	public void write(Json json) {
		json.writeValue("ip", ip);
		json.writeValue("state", state);
		if(currentTarget != null)
			json.writeValue("currentTarget", currentTarget);
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		ip = json.readValue("ip", int.class, -1, jsonData);
		state = json.readValue("state", String.class, jsonData);
		currentTarget = json.readValue("currentTarget", String.class, (String)null, jsonData);
		super.read(json, jsonData);
	}

}

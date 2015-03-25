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
import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbManager;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class RunVerbAction extends BaseCallbackAction implements VerbRunner {

	public static final String INFO = "Runs an actor verb";
	public static final Param[] PARAMS = {
			new Param("actor", "The target actor", Type.ACTOR, false),
			new Param("verb", "The 'verbId' to run.", Type.STRING, true),
			new Param("target", "Aditional actor for 'use' verb", Type.ACTOR),
			new Param("wait", "If this param is 'false' the text is showed and the action continues inmediatly",
					Type.BOOLEAN, true) };

	String actorId;
	String verb;
	String target;
	String state;
	int ip = -1;;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		verb = params.get("verb");
		target = params.get("target");

		if (params.get("wait") != null) {
			setWait(Boolean.parseBoolean(params.get("wait")));
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		setVerbCb(cb);
		
		run();
		
		return getWait();
	}

	private Verb getVerb(String verb, String target, String state) {
		Verb v = null;

		if (actorId != null) {
			BaseActor a = World.getInstance().getCurrentScene().getActor(actorId, true);

			v = a.getVerbManager().getVerb(verb, state, target);
		}

		if (v == null) {
			v = World.getInstance().getCurrentScene().getVerb(verb);
		}

		if (v == null) {
			v = VerbManager.getWorldVerbs().get(verb);
		}

		if (v == null)
			EngineLogger.error("Cannot find VERB: " + verb + " for ACTOR: " + actorId);

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
		ArrayList<Action> actions = getActions();

		for (Action c : actions) {
			if (c instanceof VerbRunner)
				((VerbRunner) c).cancel();
		}

		ip = actions.size();
	}
	

	@Override
	public ArrayList<Action> getActions() {
		Verb v = getVerb(verb, target, state);

		if (v == null) {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for actor ''{1}({3})'' and target ''{2}''",
					verb, actorId, target, World.getInstance().getCurrentScene().getActor(actorId, true).getState()));

			return new ArrayList<Action>(0);
		}

		return v.getActions();
	}

	@Override
	public void run() {
		ip = 0;

		// Gets the actor/scene state.
		if (actorId != null
				&& World.getInstance().getCurrentScene().getActor(actorId, true).getVerb(verb, target) != null) {
			state = World.getInstance().getCurrentScene().getActor(actorId, true).getState();
		} else if (World.getInstance().getCurrentScene().getVerb(verb) != null) {
			state = World.getInstance().getCurrentScene().getState();
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
		json.writeValue("actorId", actorId);
		json.writeValue("ip", ip);
		json.writeValue("verb", verb);
		json.writeValue("target", target);
		json.writeValue("state", state);
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		actorId = json.readValue("actorId", String.class, jsonData);
		ip = json.readValue("ip", Integer.class, jsonData);
		verb = json.readValue("verb", String.class, jsonData);
		target = json.readValue("target", String.class, jsonData);
		state = json.readValue("state", String.class, jsonData);
		super.read(json, jsonData);
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

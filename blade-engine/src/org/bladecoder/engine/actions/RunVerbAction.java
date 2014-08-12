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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Verb;
import org.bladecoder.engine.model.VerbManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class RunVerbAction extends BaseCallbackAction implements Action {

	public static final String INFO = "Runs an actor verb";
	public static final Param[] PARAMS = {
			new Param("verb", "The 'verbId' to run", Type.STRING, true),
			new Param("target", "Aditional actor for 'use' verb", Type.ACTOR),
			new Param("repeat", "Repeat the verb the specified times. -1 to infinity",
					Type.INTEGER),
			new Param(
					"wait",
					"If this param is 'false' the text is showed and the action continues inmediatly",
					Type.BOOLEAN, true) };

	String actorId;
	String verb;
	String target;
	boolean wait;
	int ip = -1;
	int repeat = 1;
	int currentRepeat = 0;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		verb = params.get("verb");
		target = params.get("target");

		if (params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}

		if (params.get("repeat") != null) {
			repeat = Integer.parseInt(params.get("repeat"));
		}
	}

	@Override
	public void run() {
		currentRepeat = 0;

		if (wait) {
			runVerb(verb, target);
		} else {
			runVerb(verb, target);
		}
	}

	/**
	 * Run Verb. Repeat implementation instead call actor.runVerb because of
	 * Recorder.
	 * 
	 * @param verb
	 *            Verb
	 * @param target
	 *            When one object is used with another object.
	 */
	private void runVerb(String verb, String target) {

		ip = 0;
		nextStep();

		if (!wait)
			super.onEvent();
	}

	private Verb getVerb(String verb, String target) {
		Verb v = null;

		if (actorId != null) {
			Actor a = World.getInstance().getCurrentScene()
					.getActor(actorId, true);
			v = a.getVerb(verb, target);
		} else {
			v = World.getInstance().getCurrentScene().getVerb(verb);
		}

		if (v == null) {
			v = VerbManager.getDefaultVerbs().get(verb);
		}

		return v;
	}

	public void nextStep() {

		boolean stop = false;

		Verb v = getVerb(verb, target);

		if (v == null) {
			EngineLogger
					.error(MessageFormat
							.format("Verb ''{0}'' not found for actor ''{1}'' and target ''{2}''",
									verb, actorId, target));

			return;
		}

		ArrayList<Action> actions = v.getActions();

		while (currentRepeat < repeat) {
			while (ip < actions.size() && !stop) {
				Action a = actions.get(ip);
				if (a instanceof ActionEndTrigger) {
					((ActionEndTrigger) a).setCallback(this);
					stop = true;
				} else {
					ip++;
				}

				try {
					a.run();
				} catch (Exception e) {
					EngineLogger.error("EXCEPTION EXECUTING ACTION: "
							+ a.getClass().getSimpleName(), e);
				}
			}
			
			if(!stop) {
				currentRepeat++;
				if (currentRepeat < repeat && repeat != -1) {
					ip = 0;
				} 
			}
		}

		if (wait && !stop) {
			super.onEvent();
		}
	}

	@Override
	public void onEvent() {
		ip++;
		nextStep();
	}

	public void cancel() {
		Verb v = getVerb(verb, target);

		if (v != null)
			v.cancel();
		else {
			EngineLogger
					.error(MessageFormat
							.format("Cancel: Verb ''{0}'' not found for actor ''{1}'' and target ''{2}''",
									verb, actorId, target));
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("actorId", actorId);
		json.writeValue("ip", ip);
		json.writeValue("repeat", repeat);
		json.writeValue("currentRepeat", currentRepeat);
		json.writeValue("verb", verb);
		json.writeValue("target", target);
		json.writeValue("wait", wait);
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		actorId = json.readValue("actorId", String.class, jsonData);
		ip = json.readValue("ip", Integer.class, jsonData);
		repeat = json.readValue("repeat", Integer.class, jsonData);
		currentRepeat = json
				.readValue("currentRepeat", Integer.class, jsonData);
		verb = json.readValue("verb", String.class, jsonData);
		target = json.readValue("target", String.class, jsonData);
		wait = json.readValue("wait", Boolean.class, jsonData);
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

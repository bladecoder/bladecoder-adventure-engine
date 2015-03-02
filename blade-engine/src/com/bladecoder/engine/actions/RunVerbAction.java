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

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.BaseCallbackAction;
import com.bladecoder.engine.actions.Param;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class RunVerbAction extends BaseCallbackAction {

	public static final String INFO = "Runs an actor verb";
	public static final Param[] PARAMS = {
			new Param("actor", "The target actor", Type.ACTOR, false),
			new Param("verb", "The 'verbId' to run. Can be a comma separated verb list to execute one o them based in the chooseCriteria param", Type.STRING, true),
			new Param("target", "Aditional actor for 'use' verb", Type.ACTOR),
			new Param("repeat", "Repeat the verb the specified times. -1 to infinity",
					Type.INTEGER),
			new Param("chooseCriteria", "If the verb param is a comma separated verb list, one verb will be choose following this criteria",
							Type.STRING, false, "first", new String[]{"first", "iterate", "random", "cycle"}),					
			new Param(
					"wait",
					"If this param is 'false' the text is showed and the action continues inmediatly",
					Type.BOOLEAN, true) };

	String actorId;
	String verbList;
	String verb;
	String target;
	String state;
	int ip = -1;
	int repeat = 1;
	int currentRepeat = 0;
	
	/** 
	 * When the verb is a comma separated verb list, we use chooseCriteria as criteria to choose the verb to execute.
	 */
	String chooseCriteria = "first";
	/** Used when choose_criteria is 'iterate' or 'cycle' */
	int chooseCount = 0;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		verbList = params.get("verb");
		target = params.get("target");

		if (params.get("wait") != null) {
			setWait(Boolean.parseBoolean(params.get("wait")));
		}

		if (params.get("repeat") != null) {
			repeat = Integer.parseInt(params.get("repeat"));
		}
		
		if (params.get("chooseCriteria") != null) {
			chooseCriteria = params.get("chooseCriteria");
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		setVerbCb(cb);
		currentRepeat = 0;

		ip = 0;
		selectVerb();
		nextStep();
		return getWait();
	}

	private Verb getVerb(String verb, String target, String state) {
		Verb v = null;

		if (actorId != null) {
			BaseActor a = World.getInstance().getCurrentScene()
					.getActor(actorId, true);
			
			v = a.getVerbManager().getVerb(verb, state, target);
		}
		
		if (v == null) {
			v = World.getInstance().getCurrentScene().getVerb(verb);
		}

		if (v == null) {
			v = VerbManager.getDefaultVerbs().get(verb);
		}
		
		if (v == null)
			EngineLogger.error("Cannot find VERB: " + verb + " for ACTOR: " + actorId);

		return v;
	}
	
	/**
	 * Select the verb to execute from the verbList
	 */
	private void selectVerb() {
		String verbs[] = verbList.split(",");
		
		String v = verbs[0];
		
		if(chooseCriteria.equals("iterate")) {
			v = verbs[chooseCount];
			
			if(chooseCount < verbs.length - 1)
				chooseCount++;
		} else if(chooseCriteria.equals("random")) {
			v = verbs[MathUtils.random(0, verbs.length -1)];
		} else if(chooseCriteria.equals("cycle")) {
			v = verbs[chooseCount];
			
			chooseCount = (chooseCount + 1) % verbs.length;
		}
		
		verb =  v.trim();
		
		// Gets the actor/scene state.
		
		if (actorId != null && World.getInstance().getCurrentScene()
				.getActor(actorId, true).getVerb(verb, target) != null) {
			 state = World.getInstance().getCurrentScene()
					.getActor(actorId, true).getState();
		} else if(World.getInstance().getCurrentScene().getVerb(verb) != null) {
			 state = World.getInstance().getCurrentScene().getState();
		}
	}

	private void nextStep() {

		boolean stop = false;

		Verb v = getVerb(verb, target, state);

		if (v == null) {
			EngineLogger
					.error(MessageFormat
							.format("Verb ''{0}'' not found for actor ''{1}({3})'' and target ''{2}''",
									verb, actorId, target, World.getInstance().getCurrentScene().getActor(actorId, true).getState()));

			return;
		}

		ArrayList<Action> actions = v.getActions();

		while ((currentRepeat < repeat || repeat == -1) && !stop) {
			while (ip < actions.size() && !stop) {
				Action a = actions.get(ip);
				
				if(EngineLogger.debugMode())
					EngineLogger.debug("RunVerbAction: " + verb + "(" + ip + ") " + a.getClass().getSimpleName());

				try {
					if(a.run(this))
						stop = true;
					else
						ip++;
				} catch (Exception e) {
					EngineLogger.error("EXCEPTION EXECUTING ACTION: "
							+ a.getClass().getSimpleName(), e);
				}
			}
			
			if(!stop) {
				currentRepeat++;
				if (currentRepeat < repeat || repeat == -1) {
					ip = 0;
					selectVerb();
					v = getVerb(verb, target, state);
					
					if (v == null) {
						EngineLogger
								.error(MessageFormat
										.format("Verb ''{0}'' not found for actor ''{1}({3})'' and target ''{2}''",
												verb, actorId, target, World.getInstance().getCurrentScene().getActor(actorId, true).getState()));

						return;
					}

					actions = v.getActions();
				} 
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

	public void cancel() {
		ip = Integer.MAX_VALUE/2;
		currentRepeat++;
	}

	@Override
	public void write(Json json) {
		json.writeValue("actorId", actorId);
		json.writeValue("ip", ip);
		json.writeValue("repeat", repeat);
		json.writeValue("currentRepeat", currentRepeat);
		json.writeValue("verb", verb);
		json.writeValue("target", target);
		json.writeValue("chooseCriteria", chooseCriteria);
		json.writeValue("chooseCount", chooseCount);
		json.writeValue("verbList", verbList);
		json.writeValue("state", state);
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
		chooseCriteria = json.readValue("chooseCriteria", String.class, jsonData);
		chooseCount = json.readValue("chooseCount", Integer.class, jsonData);
		verbList = json.readValue("verbList", String.class, jsonData);
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

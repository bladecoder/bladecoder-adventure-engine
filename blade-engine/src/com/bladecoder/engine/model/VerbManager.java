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
package com.bladecoder.engine.model;

import java.text.MessageFormat;
import java.util.HashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.loader.SerializationHelper;
import com.bladecoder.engine.loader.SerializationHelper.Mode;
import com.bladecoder.engine.util.EngineLogger;

public class VerbManager implements Serializable {
	protected HashMap<String, Verb> verbs = new HashMap<String, Verb>();

	public void addVerb(String id, Verb v) {
		verbs.put(id, v);
	}

	// Used only in getVerb(). It is a class variable to avoid allocations
	private StringBuilder tmpsb = new StringBuilder();

	/**
	 * Returns an actor Verb.
	 * 
	 * Search order: - id.target.state - id.target - id.state - id
	 * 
	 * @param id
	 *            Verb id
	 * @param target
	 *            When an object is used by other object.
	 */
	public Verb getVerb(String id, String state, String target) {
		Verb v = null;

		if (target != null) {
			tmpsb.setLength(0);
			if (state != null) {
				tmpsb.append(id).append(".").append(target).append(".").append(state);
				v = verbs.get(tmpsb.toString()); // id.target.state
			}

			if (v == null) {
				tmpsb.setLength(0);
				tmpsb.append(id).append(".").append(target);
				v = verbs.get(tmpsb.toString()); // id.target
			}
		}

		if (v == null && state != null) {
			tmpsb.setLength(0);
			tmpsb.append(id).append(".").append(state);

			v = verbs.get(tmpsb.toString()); // id.state
		}

		if (v == null)
			v = verbs.get(id); // id

		return v;
	}

	public HashMap<String, Verb> getVerbs() {
		return verbs;
	}

	/**
	 * Run Verb
	 * 
	 * @param verb
	 *            Verb
	 * @param target
	 *            When one object is used with another object.
	 */
	public void runVerb(String verb, String state, String target) {

		Verb v = null;

		EngineLogger.debug(MessageFormat.format("Run Verb:{0} State: {1} Target: {2}", verb, state, target));

		v = getVerb(verb, state, target);

		if (v == null) {
			v = World.getInstance().getVerbManager().getVerb(verb, null, null);
		}

		if (v != null) {
			v.run();
		} else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for target ''{1}''", verb, target));
		}
	}

	/**
	 * Cancels the execution of a running verb
	 * 
	 * @param verb
	 * @param target
	 */
	public void cancelVerb(String verb, String state, String target) {
		Verb v = null;

		v = getVerb(verb, state, target);

		if (v == null) {
			v = World.getInstance().getVerbManager().getVerb(verb, null, null);
		}

		if (v != null)
			v.cancel();
		else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for target ''{1}''", verb, target));
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("verbs", verbs, verbs.getClass(), Verb.class);
		
//		if (SerializationHelper.getInstance().getMode() == Mode.INMUTABLE) {
//			json.writeValue("verbs", verbs, verbs == null ? null : verbs.getClass(), Verb.class);
//		} else {
//			for(Verb v: verbs.values()) {
//				v.write(json);
//			}
//		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		
		if (SerializationHelper.getInstance().getMode() == Mode.INMUTABLE) {
			verbs = json.readValue("verbs", HashMap.class, Verb.class, jsonData);
		} else {
			for(String v: verbs.keySet()) {
				Verb verb = verbs.get(v);
							
				JsonValue jsonValue = jsonData.get("verbs").get(v);
				verb.read(json, jsonValue);
			}
		}
	}

}

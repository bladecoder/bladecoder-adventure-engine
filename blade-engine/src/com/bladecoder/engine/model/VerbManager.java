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
import java.util.LinkedHashMap;
import java.util.Map;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.util.EngineLogger;

public class VerbManager implements Serializable {
	private Map<String, Verb> verbs = new LinkedHashMap<>();

	public void addVerb(Verb v) {
		verbs.put(v.getCompositeId(), v);
	}

	// Used only in getVerb(). It is a class variable to avoid allocations
	private StringBuilder tmpsb = new StringBuilder();

	/**
	 * Returns an actor Verb.
	 * 
	 * Search order:
	 *   - id.target.state
	 *   - id.target
	 *   - id.state
	 *   - id
	 * 
	 * @param id Verb id
	 * @param target When an object is used by other object.
	 */
	public Verb getVerb(String id, String state, String target) {
		Verb v = null;
		
		if(target != null) {
			tmpsb.setLength(0);
			if(state != null) {
				tmpsb.append(id).append(Verb.COMPOSITE_ID_SEPARATOR).append(target).append(Verb.COMPOSITE_ID_SEPARATOR).append(state);
				v = verbs.get(tmpsb.toString()); // id.target.state
			}

			if (v == null) {
				tmpsb.setLength(0);
				tmpsb.append(id).append(Verb.COMPOSITE_ID_SEPARATOR).append(target);
				v = verbs.get(tmpsb.toString()); // id.target
			}
		}
		
		if (v == null && state != null) {
			tmpsb.setLength(0);
			tmpsb.append(id).append(Verb.COMPOSITE_ID_SEPARATOR).append(state);
			
			v = verbs.get(tmpsb.toString()); // id.state
		}

		if (v == null)
			v = verbs.get(id); // id

		return v;
	}

	public Map<String, Verb> getVerbs() {
		return verbs;
	}

	/**
	 * Run Verb
	 * 
	 * @param id Verb
	 * @param target When one object is used with another object.
	 */
	public void runVerb(String id, String state, String target) {
		EngineLogger.debug(MessageFormat.format("Run Verb:{0} State: {1} Target: {2}", id, state, target));

		Verb v = getVerb(id, state, target);

		if (v == null) {
			v = World.getInstance().getVerbManager().getVerb(id, null, null);
		}

		if (v != null) {
			v.run();
		} else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for target ''{1}'' and state ''{2}''", id, target, state));
		}
	}
	
	/**
	 * Cancels the execution of a running verb
	 * 
	 * @param id
	 * @param target
	 */
	public void cancelVerb(String id, String state, String target) {
		Verb v = getVerb(id, state, target);

		if (v == null) {
			v = World.getInstance().getVerbManager().getVerb(id, null, null);
		}

		if (v != null)
			v.cancel();
		else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for target ''{1}'' and state ''{2}''", id, target, state));
		}	
	}	

	@Override
	public void write(Json json) {
		json.writeValue("verbs", verbs, verbs.getClass(), Verb.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read (Json json, JsonValue jsonData) {
		verbs = json.readValue("verbs", HashMap.class, Verb.class, jsonData);
	}
}

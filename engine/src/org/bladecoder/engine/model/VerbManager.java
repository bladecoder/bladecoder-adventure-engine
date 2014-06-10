package org.bladecoder.engine.model;

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class VerbManager implements Serializable {
	protected static HashMap<String, Verb> defaultVerbs = new HashMap<String, Verb>();
	protected HashMap<String, Verb> verbs = new HashMap<String, Verb>();

	public void addVerb(String id, Verb v) {
		verbs.put(id, v);
	}

	public static void addDefaultVerb(String id, Verb v) {
		defaultVerbs.put(id, v);
	}

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
	 * @return
	 */
	public Verb getVerb(String id, String state, String target) {
		StringBuilder sb = new StringBuilder();
		Verb v = null;
		
		if(target != null) {
			if(state != null) {
				sb.append(id).append(".").append(target).append(".").append(state);
				v = verbs.get(sb.toString()); // id.target.state
			}

			if (v == null) {
				sb.setLength(0);
				sb.append(id).append(".").append(target);
				v = verbs.get(sb.toString()); // id.target
			}
		}
		
		if (v == null && state != null) {
			sb.setLength(0);
			sb.append(id).append(".").append(state);
			
			v = verbs.get(sb.toString()); // id.state
		}

		if (v == null)
			v = verbs.get(id); // id

		return v;
	}
	
	public static HashMap<String, Verb> getDefaultVerbs() {
		return defaultVerbs;
	}

	public HashMap<String, Verb> getVerbs() {
		return verbs;
	}

	/**
	 * Run Verb
	 * 
	 * @param verb Verb
	 * @param target When one object is used with another object.
	 */
	public void runVerb(String verb, String state, String target) {

		Verb v = null;
			
		v = getVerb(verb, state, target);

		if (v == null) {
			v = defaultVerbs.get(verb);
		}

		if (v != null) {
			v.run();
		} else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for target ''{1}''",
					verb, target) );
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
			v = defaultVerbs.get(verb);
		}

		if (v != null)
			v.cancel();
		else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for target ''{1}''",
					verb, target) );
		}	
	}	

	@Override
	public void write(Json json) {
		json.writeValue("verbs", verbs, verbs == null ? null : verbs.getClass());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read (Json json, JsonValue jsonData) {
		verbs = json.readValue("verbs", HashMap.class, Verb.class, jsonData);
	}


}

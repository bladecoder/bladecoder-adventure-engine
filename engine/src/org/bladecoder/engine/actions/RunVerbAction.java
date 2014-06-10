package org.bladecoder.engine.actions;

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Verb;
import org.bladecoder.engine.model.VerbManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

public class RunVerbAction implements Action {

	public static final String INFO = "Runs an actor verb";
	public static final Param[] PARAMS = {
		new Param("verb", "The 'verbId' to run", Type.STRING, true),
		new Param("target", "Aditional 'targetid' to locate the verb", Type.STRING)
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
		runVerb(verb, target);
	}
	
	
	/**
	 * Run Verb. Repeat implementation instead call actor.runVerb because of Recorder.
	 * 
	 * @param verb Verb
	 * @param target When one object is used with another object.
	 */
	private void runVerb(String verb, String target) {

		Verb v = getVerb(verb, target);

		if (v != null)
			v.run();
		else {
			EngineLogger.error(MessageFormat.format("Verb ''{0}'' not found for actor ''{1}'' and target ''{2}''",
					verb, actorId, target) );
		}
	}
	
	private Verb getVerb(String verb, String target) {
		Verb v = null;
		
		if(actorId != null) {
			Actor a = World.getInstance().getCurrentScene().getActor(actorId);
			v = a.getVerb(verb, target);
		} else {
			v = World.getInstance().getCurrentScene().getVerb(verb);
		}

		if (v == null) {
			v = VerbManager.getDefaultVerbs().get(verb);
		}
		
		return v;
	}
	
	public void cancel() {
		Verb v = getVerb(verb, target);

		if (v != null)
			v.cancel();
		else {
			EngineLogger.error(MessageFormat.format("Cancel: Verb ''{0}'' not found for actor ''{1}'' and target ''{2}''",
					verb, actorId, target) );
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

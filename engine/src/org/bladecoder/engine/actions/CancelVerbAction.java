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
		new Param("target", "If the verb is 'use', the target actor. Not mandatory", Type.STRING)
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
			Actor actor = World.getInstance().getCurrentScene().getActor(actorId);
		
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

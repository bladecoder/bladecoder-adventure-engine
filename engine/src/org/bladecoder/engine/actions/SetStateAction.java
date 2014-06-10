package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.World;

public class SetStateAction implements Action {
	public static final String INFO = "Sets the actor state";
	public static final Param[] PARAMS = {
		new Param("state", "The actor 'state'", Type.STRING)
		};		
	
	String actorId;
	String state;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		state = params.get("state");
	}

	@Override
	public void run() {
		if(actorId != null) {
			Actor actor = World.getInstance().getCurrentScene().getActor(actorId);
		
			actor.setState(state);
		} else {
			World.getInstance().getCurrentScene().setState(state);
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

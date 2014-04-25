package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.World;

public class SetActiveAction implements Action {
	public static final String INFO = "Change the visible/interaction properties for the selected actor.";
	public static final Param[] PARAMS = {
		new Param("value", "when 'true' sets the actor visible and interactuable, when 'false' sets the actor hide and not interactuable", Type.BOOLEAN),
		new Param("visible", "sets the actor visibility", Type.BOOLEAN), 
		new Param("interaction", "when 'true' the actor responds to the user input", Type.BOOLEAN)
		};		
	
	String actorId;
	String visible;
	String interaction;
	String value;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		
		visible = params.get("visible");
		interaction = params.get("interaction");
		value = params.get("value");
	}

	@Override
	public void run() {
		Actor actor = World.getInstance().getCurrentScene().getActor(actorId);
		
		if(value != null) {
			actor.setActive(Boolean.parseBoolean( value));
		} else {
			if(visible != null) actor.setVisible(Boolean.parseBoolean(visible));
			if(interaction != null) actor.setInteraction(Boolean.parseBoolean( interaction));
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

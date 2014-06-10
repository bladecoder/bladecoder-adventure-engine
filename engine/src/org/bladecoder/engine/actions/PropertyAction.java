package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.World;

public class PropertyAction implements Action {
	public static final String INFO = "Set/Remove a global property of the game";
	public static final Param[] PARAMS = {
		new Param("type", "Property type", Type.STRING, true, "actor", new String[] {"world", "scene", "actor"}),
		new Param("prop", "Property name", Type.STRING, true), 
		new Param("value", "Property value", Type.STRING, true),
		};		
	
	String actorId;
	String op;
	String prop;
	String value;
	String type;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		
		prop = params.get("prop");
		value = params.get("value");
		type = params.get("type");
	}

	@Override
	public void run() {
		
		if(type.equals("world")) {
			World.getInstance().setCustomProperty(prop, value);
		} else if(type.equals("scene")) {
			Scene s = World.getInstance().getCurrentScene();
			s.setCustomProperty(prop, value);
		} else {
			Actor actor = World.getInstance().getCurrentScene().getActor(actorId);
			actor.setCustomProperty(prop, value);
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

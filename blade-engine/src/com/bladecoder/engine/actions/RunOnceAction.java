package com.bladecoder.engine.actions;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Execute the actions inside the RunOnce/EndRunOnce only once.")
public class RunOnceAction extends AbstractControlAction implements Serializable {
	boolean executed = false;
	
	
	@Override
	public void init(World w) {
	}

	@Override
	public boolean run(VerbRunner cb) {
		VerbRunner v = (VerbRunner)cb;
		
		if (executed) {
			final int ip = skipControlIdBlock(v.getActions(), v.getIP());

			v.setIP(ip);
		}
		
		executed=true;
		
		return false;
	}

	
	@Override
	public void write(Json json) {
		json.writeValue("executed", executed);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		executed = json.readValue("executed", boolean.class, false, jsonData);
	}
}

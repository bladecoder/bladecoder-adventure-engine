package com.bladecoder.engine.actions;

import java.io.StringWriter;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;

/**
 * This action wraps an action that has been disabled.
 * 
 * @author rgarcia
 */
@ActionDescription("Helper action to allow disabled actions.")
public class DisableActionAction implements Action {
	@ActionProperty(required = true)
	private String serializedAction;
	
	private Action action;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		return false;
	}

	public void setAction(Action a) {
		action = a;
		Json json = new Json();
		StringWriter buffer = new StringWriter();
		json.setWriter(buffer);
		ActionUtils.writeJson(a, json);
		serializedAction = buffer.toString();
	}
	
	public Action getAction() {
		if(action == null) {
			Json json = new Json();
			JsonValue root = new JsonReader().parse(serializedAction);
			action =  ActionUtils.readJson(w, json, root);
		}
		
		return action;
	}
}

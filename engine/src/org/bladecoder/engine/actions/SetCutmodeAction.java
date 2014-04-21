package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;

public class SetCutmodeAction implements Action {
	public static final String INFO = "Set/Unset the cutmode. Also shows/hide the inventory";
	public static final Param[] PARAMS = {
		new Param("value", "when 'true' sets the scene in 'cutmode'", Type.BOOLEAN, true), 
		new Param("inventory", "show/hide the inventory", Type.BOOLEAN)
		};	
	
	boolean value = true;
	boolean inventory = true;
	boolean setInventory = false;
	
	@Override
	public void setParams(HashMap<String, String> params) {	
		if(params.get("value") != null)
			value = Boolean.parseBoolean(params.get("value"));
		
		if(params.get("inventory") != null) {
			setInventory = true;
			inventory = Boolean.parseBoolean(params.get("inventory"));
		}
	}

	@Override
	public void run() {
		World.getInstance().setCutMode(value);
		
		if(setInventory)
			World.getInstance().showInventory(inventory);
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

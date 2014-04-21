package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;


public class LeaveAction implements Action {
	public static final String INFO = "Change the current scene. The target scene must exists in the current chapter.";
	public static final Param[] PARAMS = {
		new Param("target", "The target scene", Type.STRING, true)				
		};		
	
	String target;

	@Override
	public void run() {
		EngineLogger.debug("LEAVE ACTION");
		
		World.getInstance().setCurrentScene(target);
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		target = params.get("target");		
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

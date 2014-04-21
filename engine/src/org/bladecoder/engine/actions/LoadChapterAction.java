package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;


public class LoadChapterAction implements Action {
	public static final String INFO = "Loads a chapter and changes the current scene";
	public static final Param[] PARAMS = {
		new Param("chapter", "The chapter to load", Type.STRING, true),
		new Param("scene", "Optional scene to load", Type.STRING)
		};		
	
	String chapter;
	String scene;

	@Override
	public void run() {
		EngineLogger.debug("LOAD CHAPTER ACTION");
		
		World.getInstance().loadXML(chapter, scene);
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		chapter = params.get("chapter");
		scene = params.get("scene");		
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

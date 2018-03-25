package com.bladecoder.engineeditor.qa.rules;

import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.qa.EndVisitor;
import com.bladecoder.engineeditor.qa.SceneVisitor;

public class Stats implements SceneVisitor, EndVisitor {
	
	int numScenes = 0;

	@Override
	public void visit(Scene s) {
		numScenes++;
	}

	@Override
	public void end(World w) {
		EditorLogger.msg("Num Scenes: " + numScenes);
	}
	
}

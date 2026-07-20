package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Load the specified Chapter. Scene can be empty to load the default scene.")
public class LoadChapterAction implements Action {
	@ActionPropertyDescription("The target chapter")
	@ActionProperty(type = Type.CHAPTER, required = true)
	private String chapter;

	@ActionPropertyDescription("The target scene")
	@ActionProperty(type=Type.STRING, required = false)
	private String scene;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		try {
			w.loadChapter(chapter, scene, false);
		} catch (Exception e) {
			EngineLogger.error(e.getMessage());
		}
		
		return true;
	}

}

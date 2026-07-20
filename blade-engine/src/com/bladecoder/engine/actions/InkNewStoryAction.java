package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Load an Ink story.")
public class InkNewStoryAction implements Action {
	@ActionPropertyDescription("The story to load")
	@ActionProperty(required = true)
	private String storyName;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		try {
			w.getInkManager().newStory(storyName);
		} catch (Exception e) {
			EngineLogger.error("Cannot load Ink Story: " + storyName + " " + e.getMessage());
		}

		return false;
	}
}

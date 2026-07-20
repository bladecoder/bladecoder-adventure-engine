package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Sets a global game property. Properties are created by the user but the next ones always exists: SAVED_GAME_VERSION, PREVIOUS_SCENE, CURRENT_CHAPTER, PLATFORM")
public class PropertyAction implements Action {
	@ActionProperty(required = true)
	@ActionPropertyDescription("Property name")
	private String prop;

	@ActionProperty
	@ActionPropertyDescription("Property value")
	private String value;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		w.setCustomProperty(prop, value);

		return false;
	}

}

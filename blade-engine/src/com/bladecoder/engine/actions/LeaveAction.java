package com.bladecoder.engine.actions;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Change the current scene.")
public class LeaveAction implements Action {
	@ActionPropertyDescription("The target scene")
	@ActionProperty(type = Type.SCENE, required = true)
	private String scene;

	@ActionPropertyDescription("Inits the scene and run the 'init' verb")
	@ActionProperty(defaultValue = "true", required = true)
	private boolean init = true;

	@ActionPropertyDescription("The verb to run after loading. If null, 'init' verb will be run but only if init=true")
	@ActionProperty
	private String initVerb = null;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		w.setCurrentScene(scene, init, initVerb);

		return true;
	}
}

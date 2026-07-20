package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Pause the action")
public class WaitAction implements Action {
	@ActionProperty(required = true, defaultValue = "1.0")
	@ActionPropertyDescription("The time pause in seconds")

	private float time;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		w.getCurrentScene().addTimer(time, cb);
		return true;
	}
}

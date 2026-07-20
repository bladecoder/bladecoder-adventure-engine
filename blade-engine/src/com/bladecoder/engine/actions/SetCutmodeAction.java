package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription(name = "Cutmode", value="Set/Unset the cutmode.")
public class SetCutmodeAction implements Action {
	@ActionProperty(required = true, defaultValue = "true")
	@ActionPropertyDescription("when 'true' sets the scene in 'cutmode'")
	private boolean value = true;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		w.setCutMode(value);
		
		return false;
	}

}

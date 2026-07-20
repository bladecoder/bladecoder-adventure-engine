package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Shows/Hide the inventory")
public class ShowInventoryAction implements Action {
	@ActionProperty(required = true, defaultValue = "true")
	@ActionPropertyDescription("When 'true' shows the inventory button to show the inventory.")
	private boolean value = true;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		w.showInventory(value);
		
		return false;
	}

}

package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Sets the value of an Ink variable.")
public class InkVariable implements Action {
	@ActionProperty(required = true)
	@ActionPropertyDescription("Variable name")
	private String name;

	@ActionProperty(required = true)
	@ActionPropertyDescription("Value")
	private String value;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		try {
			w.getInkManager().setVariable(name, value);
		} catch (Exception e) {
			EngineLogger.error("Cannot set Ink variable: " + name + " " + e.getMessage());
		}

		return false;
	}

}

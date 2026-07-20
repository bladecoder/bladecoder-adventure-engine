package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Adds an integer value to the selected property.")
public class AddValueToProperty implements Action {
	@ActionProperty(required = true)
	@ActionPropertyDescription("Property name")
	private String prop;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The integer value to add.")
	private int value;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		String p = w.getCustomProperty(prop);
		
		int v = 0;
		
		if(p != null) {
			try {
				v = Integer.parseInt(p);
			} catch(NumberFormatException e) {
			}
		}
		
		v += value;
		
		w.setCustomProperty(prop, Integer.toString(v));
		EngineLogger.debug("AddValueToProperty: " + prop + "=" + w.getCustomProperty(prop));

		return false;
	}

}

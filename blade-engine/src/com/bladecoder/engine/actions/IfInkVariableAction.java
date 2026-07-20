package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Execute the actions inside the If/EndIf if the Ink variable has the specified value.")
public class IfInkVariableAction extends AbstractIfAction {
	@ActionProperty(required = true)
	@ActionPropertyDescription("The variable name")
	private String name;

	@ActionProperty
	@ActionPropertyDescription("The value")
	private String value;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		if (! w.getInkManager().compareVariable(name, value)) {
			gotoElse((VerbRunner) cb);
		}

		return false;
	}

}

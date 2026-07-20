package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.Config;

@ActionDescription("Execute actions inside the If/EndIf if the game property has the specified value. Properties are created with the 'Property' action or set in the 'BladeEngine.properties' file. The next always exists: SAVED_GAME_VERSION, PREVIOUS_SCENE, CURRENT_CHAPTER, PLATFORM.")
public class IfPropertyAction extends AbstractIfAction {
	@ActionProperty(required = true)
	@ActionPropertyDescription("The property name")
	private String name;

	@ActionProperty
	@ActionPropertyDescription("The property value")
	private String value;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		String valDest = w.getCustomProperty(name);

		if (valDest == null)
			valDest = Config.getInstance().getProperty(name, null);

		if (valDest == null)
			valDest = Config.getInstance().getPref(name, null);

		if (!ActionUtils.compareNullStr(value, valDest)) {
			gotoElse(cb);
		}

		return false;
	}

}

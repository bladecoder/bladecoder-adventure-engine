package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Set an achievement.")
public class SetAchievementAction implements Action {
	public static IAchievementAPI achievementAPI;

	@ActionProperty(required = true)
	@ActionPropertyDescription("Achievement name")
	private String name;

	@Override
	public void init(World w) {
	}

	@Override
	public boolean run(VerbRunner cb) {
		if (achievementAPI != null)
			achievementAPI.setAchievement(name);

		return false;
	}

}

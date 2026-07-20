package com.bladecoder.engineeditor.ui.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.WalkZoneActor;
import com.bladecoder.engineeditor.Ctx;

public class ActorInputPanel extends EditableOptionsInputPanel<String> {

	ActorInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, Param.Type type) {
		super(skin, title, desc, mandatory, defaultValue, getValues(mandatory, type));

		if (mandatory)
			if (type == Param.Type.ACTOR)
				setText(Ctx.project.getSelectedActor().getId());
			else
				input.setSelectedIndex(0);
	}

	private static String[] getValues(boolean mandatory, Param.Type type) {

		ArrayList<BaseActor> filteredActors = new ArrayList<>();

		if (Ctx.project.getSelectedScene() != null) {
			Map<String, BaseActor> actors = Ctx.project.getSelectedScene().getActors();

			for (BaseActor a : actors.values()) {
				if (type == Param.Type.CHARACTER_ACTOR) {
					if (a instanceof CharacterActor)
						filteredActors.add(a);
				} else if (type == Param.Type.INTERACTIVE_ACTOR) {
					if (a instanceof InteractiveActor)
						filteredActors.add(a);
				} else if (type == Param.Type.WALKZONE_ACTOR) {
					if (a instanceof WalkZoneActor)
						filteredActors.add(a);
				} else if (type == Param.Type.SPRITE_ACTOR) {
					if (a instanceof SpriteActor)
						filteredActors.add(a);
				} else {
					filteredActors.add(a);
				}
			}
		}

		String[] result = null;

		if (type != Param.Type.WALKZONE_ACTOR) {
			// Add player variable to the list
			result = new String[filteredActors.size() + 1];

			result[0] = Scene.VAR_PLAYER;

			for (int i = 0; i < filteredActors.size(); i++) {
				result[i + 1] = filteredActors.get(i).getId();
			}
		} else {
			result = new String[filteredActors.size()];

			for (int i = 0; i < filteredActors.size(); i++) {
				result[i] = filteredActors.get(i).getId();
			}

		}

		Arrays.sort(result);
		return result;
	}
}

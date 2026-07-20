package com.bladecoder.engineeditor.ui.panels;

import java.util.Arrays;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;

public class SceneInputPanel extends StringOptionsInputPanel {
	SceneInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue, getValues(mandatory));
	}

	private static String[] getValues(boolean mandatory) {
		Map<String, Scene> scenes = Ctx.project.getWorld().getScenes();
		
		String[] result = new String[scenes.size() + 1];
		
		Scene[] v = scenes.values().toArray(new Scene[scenes.size()]);
		
		for(int i = 0; i < scenes.size(); i++) {
			result[i] = v[i].getId();
		}
		
		result[scenes.size()] = "$" + World.WorldProperties.PREVIOUS_SCENE.toString();
		
		Arrays.sort(result);
		
		return result;
	}
}

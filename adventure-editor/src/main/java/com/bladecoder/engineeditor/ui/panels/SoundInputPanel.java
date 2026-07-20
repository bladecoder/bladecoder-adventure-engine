package com.bladecoder.engineeditor.ui.panels;

import java.util.Arrays;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engineeditor.Ctx;

public class SoundInputPanel extends StringOptionsInputPanel {
	SoundInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue, getValues(mandatory));
	}

	private static String[] getValues(boolean mandatory) {
		Map<String, SoundDesc> sounds = Ctx.project.getWorld().getSounds();
		
		String[] result = new String[sounds.size()];
		
		SoundDesc[] v = sounds.values().toArray(new SoundDesc[sounds.size()]);
		
		for(int i = 0; i < sounds.size(); i++) {
			result[i] = v[i].getId();
		}
		
		Arrays.sort(result);
		
		return result;
	}
}

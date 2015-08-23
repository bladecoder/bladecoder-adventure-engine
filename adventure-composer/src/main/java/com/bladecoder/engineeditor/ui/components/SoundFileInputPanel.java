package com.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SoundFileInputPanel extends EditableOptionsInputPanel<String> {
	SoundFileInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue, getValues(mandatory));
	}

	private static String[] getValues(boolean mandatory) {
		String path = Ctx.project.getProjectPath() + Project.SOUND_PATH;

		File f = new File(path);

		String soundFiles[] = f.list((arg0, arg1) -> arg1.endsWith(".ogg") || arg1.endsWith(".wav") || arg1.endsWith(".mp3"));

		if (soundFiles == null) {
			return !mandatory ? new String[]{""} : new String[0];
		}
		Arrays.sort(soundFiles);

		if (mandatory)
			return soundFiles;

		// FIXME: We need to make mandatory behave differently
		final List<String> result = Arrays.asList(soundFiles);
		result.add(0, "");
		return result.toArray(new String[result.size()]);
	}
}

/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SoundFX;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;

public class EditSoundDialog extends EditModelDialog<InteractiveActor, SoundFX> {

	private InputPanel id;
	private InputPanel filename;
	private InputPanel loop;
	private InputPanel volume;

	public EditSoundDialog(Skin skin, InteractiveActor parent, SoundFX e) {
		super(skin);

		id = InputPanelFactory.createInputPanel(skin, "Sound ID", "The id of the sound", true);
		filename = InputPanelFactory.createInputPanel(skin, "Filename", "Filename of the sound", getSoundList(), true);
		loop = InputPanelFactory.createInputPanel(skin, "Loop", "True if the sound is looping", Param.Type.BOOLEAN,
				true, "false");
		volume = InputPanelFactory.createInputPanel(skin, "Volume", "Select the volume", Param.Type.FLOAT, true, "1.0");

		setInfo("Actors can have a list of sounds that can be associated to Sprites or played with the 'sound' action");

		init(parent, e, new InputPanel[] { id, filename, loop, volume });
	}

	@Override
	protected void inputsToModel(boolean create) {
		
		if(create) {
			e = new SoundFX();
		}
		
		e.setFilename(filename.getText());
		e.setLoop(Boolean.parseBoolean(loop.getText()));
		e.setVolume(Float.parseFloat(volume.getText()));
		
		if(create) {
			parent.getSounds().put(e.getFilename(), e);
		}
		
		Ctx.project.getSelectedChapter().setModified(e);
	}

	@Override
	protected void modelToInputs() {
		
		// TODO SEARCH FOR ID
//		parent.getSounds().containsValue(arg0)
		
		id.setText("");
		filename.setText(e.getFilename());
		loop.setText(Boolean.toString(e.getLoop()));
		volume.setText(Float.toString(e.getVolume()));
	}

	private String[] getSoundList() {
		String path = Ctx.project.getProjectPath() + Project.SOUND_PATH;

		File f = new File(path);

		String soundFiles[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".ogg") || arg1.endsWith(".wav") || arg1.endsWith(".mp3"))
					return true;

				return false;
			}
		});

		Arrays.sort(soundFiles);

		return soundFiles;
	}
}

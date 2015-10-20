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

import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SoundFX;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;

public class EditSoundDialog extends EditModelDialog<InteractiveActor, SoundFX> {
	
	private InputPanel[] inputs;


	String attrs[] = { "id", "filename", "loop", "volume"};	

	public EditSoundDialog(Skin skin,  InteractiveActor parent, SoundFX e) {
		super(skin);
		
		inputs = new InputPanel [4];
		inputs[0] = InputPanelFactory.createInputPanel(skin, "Sound ID", "The id of the sound");
		inputs[1] = InputPanelFactory.createInputPanel(skin, "Filename", "Filename of the sound", getSoundList(), true);
		inputs[2] = InputPanelFactory.createInputPanel(skin, "Loop", "True if the sound is looping", Param.Type.BOOLEAN, false);
		inputs[3] = InputPanelFactory.createInputPanel(skin, "Volume", "Select the volume");
		
		inputs[0].setMandatory(true);
		inputs[1].setMandatory(true);

		setInfo("Actors can have a list of sounds that can be associated to Sprites or played with the 'sound' action");

		init(inputs, attrs, parent, e);
	}
	
	@Override
	protected void create(){
		
	}
	
	@Override
	protected void fill()	{
//		for (int j = 0; j < a.length; j++) {
//			InputPanel input = i[j];
//			
//			if (!input.getText().isEmpty() && input.isVisible()) {
//				if(a[j].equals("id")) {
//					doc.setId(e, input.getText());
//				} else {
//					I18NUtils.setI18NAttr(doc, e, a[j], input.getText());
//				}
//			} else {
//				e.removeAttribute(a[j]);
//			}
//			
//			
//		}
//	
//		doc.setModified(e);
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

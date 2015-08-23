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

import com.bladecoder.engine.model.SoundFX;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EditSoundDialog extends EditAnnotatedDialog<SoundFX> {
	public EditSoundDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin, SoundFX.class, doc, parent, "sound", e);
	}

	@Nonnull
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

		if (soundFiles == null) {
			return new String[0];
		}
		Arrays.sort(soundFiles);

		return soundFiles;
	}	
}

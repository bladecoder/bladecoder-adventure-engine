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

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.common.ModelTools;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;
import com.bladecoder.engineeditor.undo.UndoCreateSound;

public class EditSoundDialog extends EditModelDialog<World, SoundDesc> {

	private InputPanel id;
	private InputPanel filename;
	private InputPanel loop;
	private InputPanel volume;
	private InputPanel pan;
	private InputPanel pitch;
	private InputPanel preload;

	private Sound s = null;

	public EditSoundDialog(Skin skin, World parent, SoundDesc e) {
		super(skin);

		id = InputPanelFactory.createInputPanel(skin, "Sound ID", "The id of the sound", true);
		filename = InputPanelFactory.createInputPanel(skin, "Filename", "Filename of the sound",
				ModelTools.getSoundList(), true);
		loop = InputPanelFactory.createInputPanel(skin, "Loop", "True if the sound is looping", Param.Type.BOOLEAN,
				true, "false");
		volume = InputPanelFactory.createInputPanel(skin, "Volume", "Select the volume between 0 and 1",
				Param.Type.FLOAT, true, "1.0");
		pan = InputPanelFactory.createInputPanel(skin, "Pan",
				"Panning in the range -1 (full left) to 1 (full right). 0 is center position", Param.Type.FLOAT, true,
				"0.0");
		pitch = InputPanelFactory.createInputPanel(skin, "Pitch",
				"The pitch multiplier, 1 == default, >1 == faster, <1 == slower, the value has to be between 0.5 and 2.0",
				Param.Type.FLOAT, true, "1.0");
		preload = InputPanelFactory.createInputPanel(skin, "Preload",
				"True if the sound has to be loaded when the scene is loaded.", Param.Type.BOOLEAN, true, "true");

		setInfo("Actors can have a list of sounds that can be associated to Sprites or played with the 'sound' action");

		init(parent, e, new InputPanel[] { id, filename, loop, volume, pan, pitch, preload });

		TextButton playButton = new TextButton("Play", skin, "no-toggled");

		playButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (s != null) {
					s.dispose();
					s = null;
				}

				if (filename.getText() != null && !filename.getText().isEmpty()) {
					s = Gdx.audio.newSound(
							new FileHandle(Ctx.project.getAssetPath() + Project.SOUND_PATH + "/" + filename.getText()));

					s.play(Float.parseFloat(volume.getText()), Float.parseFloat(pitch.getText()),
							Float.parseFloat(pan.getText()));
				}

			}
		});

		Table bt = getButtonTable();
		bt.add(playButton);
	}

	@Override
	protected void inputsToModel(boolean create) {

		if (create) {
			e = new SoundDesc();

			// UNDO OP
			Ctx.project.getUndoStack().add(new UndoCreateSound(e));
		} else {
			HashMap<String, SoundDesc> sounds = parent.getSounds();
			sounds.remove(e.getId());
		}

		String checkedId = parent.getSounds() == null ? id.getText()
				: ElementUtils.getCheckedId(id.getText(),
						parent.getSounds().keySet().toArray(new String[parent.getSounds().size()]));

		e.setId(checkedId);
		e.setFilename(filename.getText());
		e.setLoop(Boolean.parseBoolean(loop.getText()));
		e.setVolume(Float.parseFloat(volume.getText()));
		e.setPan(Float.parseFloat(pan.getText()));
		e.setPitch(Float.parseFloat(pitch.getText()));
		e.setPreload(Boolean.parseBoolean(preload.getText()));

		parent.getSounds().put(e.getId(), e);

		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {
		id.setText(e.getId());
		filename.setText(e.getFilename());
		loop.setText(Boolean.toString(e.getLoop()));
		volume.setText(Float.toString(e.getVolume()));
		pan.setText(Float.toString(e.getPan()));
		pitch.setText(Float.toString(e.getPitch()));
		preload.setText(Boolean.toString(e.isPreload()));
	}

	@Override
	protected void result(Object object) {
		if (s != null) {
			s.dispose();
			s = null;
		}

		super.result(object);
	}
}

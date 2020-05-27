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
import java.io.IOException;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ImageUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class CreateResolutionDialog extends EditDialog {

	private static final String INFO = "Create a new resolution. Scale all atlases and images of the game.";

	private InputPanel scale;

	protected ChangeListener listener;

	String atlasDir = Ctx.project.getAssetPath() + Project.ATLASES_PATH;
	String uiDir = Ctx.project.getAssetPath() + Project.UI_PATH;
	String imageDir = Ctx.project.getAssetPath() + Project.IMAGE_PATH;

	public CreateResolutionDialog(Skin skin) {
		super("CREATE RESOLUTION", skin);

		scale = InputPanelFactory.createInputPanel(skin, "Scale", "Scale relative to the world resolution",
				Param.Type.FLOAT, true);

		addInputPanel(scale);

		setInfo(INFO);
	}

	@Override
	protected void ok() {

		final Stage stage = getStage();

		Message.showMsg(stage, "Creating resolution...", true);

		Timer.schedule(new Task() {
			@Override
			public void run() {
				createResolution();

				String msg = scaleImages();

				if (listener != null)
					listener.changed(new ChangeEvent(), CreateResolutionDialog.this);

				Message.hideMsg();

				if (msg != null)
					Message.showMsgDialog(stage, "Error creating resolution", msg);
			}
		}, 1);
	}

	private void createResolution() {
//		float s = Float.parseFloat(scale.getText());
//		String prefix = (int)(Ctx.project.getWorld().getWidth() * s) + "_" +  (int)(Ctx.project.getWorld().getHeight() * s);
		String prefix = scale.getText().trim();

		new File(atlasDir + "/" + prefix).mkdir();
		new File(uiDir + "/" + prefix).mkdir();
		new File(imageDir + "/" + prefix).mkdir();
	}

	private String scaleImages() {

		float s = Float.parseFloat(scale.getText());
//		String prefix = (int)(Ctx.project.getWorld().getWidth() * s) + "_" +  (int)(Ctx.project.getWorld().getHeight() * s);
		String prefix = scale.getText().trim();

		// COPY ASSETS FROM WORLD RESOLUTION SCALED
		String wPrefix = Ctx.project.getResDir();

		try {
			ImageUtils.scaleDirFiles(new File(uiDir + "/" + wPrefix), new File(uiDir + "/" + prefix), s);
			ImageUtils.scaleDirFiles(new File(imageDir + "/" + wPrefix), new File(imageDir + "/" + prefix), s);

			ImageUtils.scaleDirAtlases(new File(atlasDir + "/" + wPrefix), new File(atlasDir + "/" + prefix), s);
			ImageUtils.scaleDirAtlases(new File(uiDir + "/" + wPrefix), new File(uiDir + "/" + prefix), s);
		} catch (IOException e) {
			return e.getMessage();
		}

		return null;
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!scale.validateField())
			ok = false;

		return ok;
	}

	public void setListener(ChangeListener changeListener) {
		this.listener = changeListener;
	}
}

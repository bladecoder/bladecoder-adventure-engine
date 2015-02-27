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

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.EditDialog;
import com.bladecoder.engineeditor.ui.components.FileInputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.utils.EditorLogger;

public class CreateProjectDialog extends EditDialog {

	public static final String INFO = "A project folder with the proper structure will be created in the selected location.";
	
	private static final String ANDROID_SDK_PROP = "package.SDK";

	private InputPanel projectName;
	private InputPanel pkg;
	private InputPanel spinePlugin;
	private FileInputPanel location;
	private FileInputPanel androidSdk;

	public CreateProjectDialog(Skin skin) {
		super("CREATE PROJECT", skin);

		setInfo(INFO);

		projectName = InputPanelFactory.createInputPanel(skin, "Project Name",
				"Set the name of the project", true);
		
		pkg = InputPanelFactory.createInputPanel(skin, "Package",
				"The package will be used as package ID in Android and IOS. The launchers will be in this package too.", Param.Type.STRING, true, "com.myadv.game");
		
		spinePlugin = InputPanelFactory.createInputPanel(skin, "Spine animation support",
				"The game can use Spine animations, require Spine License to distribute the game. See http://www.esotericsoftware.com for more info.", Param.Type.BOOLEAN, true, "false");

		location = new FileInputPanel(skin, "Location",
				"Select the folder location for the project", true);
		
		androidSdk = new FileInputPanel(skin, "Android SDK",
				"Select the Android SDK folder. If empty, the ANDROID_HOME variable will be used", true);

		addInputPanel(projectName);
		addInputPanel(pkg);
		addInputPanel(location);
		addInputPanel(spinePlugin);
		addInputPanel(androidSdk);
		
		String sdkprop = Ctx.project.getConfig().getProperty(ANDROID_SDK_PROP);
		
		if(sdkprop != null && new File(sdkprop).exists()) {
			androidSdk.setText(sdkprop);
		}
		
//		getStage().setKeyboardFocus(projectName.getField());
	}

	@Override
	protected void ok() {
		try {
			Ctx.project.getConfig().setProperty(ANDROID_SDK_PROP, androidSdk.getText());
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the current project.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			Ctx.msg.show(getStage(), msg, 2);
		}

		new Thread(new Runnable() {
			Stage stage = getStage();
			
			@Override
			public void run() {
				createProject(stage);	
			}
		}).start();
	}

	private void createProject(Stage stage) {
		Ctx.msg.show(stage, "Creating project...", true);

		try {
			Ctx.project.createProject(
					location.getText(), 
					projectName.getText(), pkg.getText(),
					androidSdk.getText(), Boolean.parseBoolean(spinePlugin.getText()));
			
			Ctx.project.loadProject(new File(location.getText() + "/"
					+ projectName.getText()));
			
			Ctx.msg.show(stage, "Project successfully created", 3);
		} catch (Exception e) {
			String msg = "Something went wrong while creating project.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(stage, msg, 3);
			EditorLogger.error(msg);
			e.printStackTrace();
			return;
		}
	}

	@Override
	protected boolean validateFields() {
		boolean isOk = true;
		
		if (!projectName.validateField()) {
			isOk = false;
		}
		
		if (!pkg.validateField()) {
			isOk = false;
		}

		if (location.getFile() != null) {
			location.setError(false);
		} else {
			location.setError(true);
			isOk = false;
		}
		
		if (System.getenv("ANDROID_HOME") == null && androidSdk.getFile() == null) {
			androidSdk.setError(true);
			isOk = false;
		} else {
			androidSdk.setError(false);			
		}

		return isOk;
	}
}

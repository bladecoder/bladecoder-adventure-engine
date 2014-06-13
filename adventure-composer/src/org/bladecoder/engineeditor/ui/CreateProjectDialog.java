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
package org.bladecoder.engineeditor.ui;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.ui.components.EditDialog;
import org.bladecoder.engineeditor.ui.components.FileInputPanel;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.xml.sax.SAXException;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class CreateProjectDialog extends EditDialog {

	public static final String INFO = "A project folder with the proper structure will be created in the selected location.";

	private InputPanel projectName;
	private FileInputPanel location;

	public CreateProjectDialog(Skin skin) {
		super("CREATE PROJECT", skin);

		setInfo(INFO);

		projectName = new InputPanel(skin, "Project Name",
				"Select the name of the project");

		location = new FileInputPanel(skin, "Location",
				"Select the folder location for the project", true);

		addInputPanel(projectName);
		addInputPanel(location);
		
//		getStage().setKeyboardFocus(projectName.getField());
	}

	@Override
	protected void ok() {
		try {
			Ctx.project.saveProject();
		} catch (Exception ex) {
			String msg = "Something went wrong while saving the current project.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			Ctx.msg.show(getStage(), msg, 2);
		}

		createProject();
	}

	private void createProject() {
		Ctx.msg.show(getStage(), "Creating project...");

		try {
			Ctx.project.createProject(new File(location.getText() + "/"
					+ projectName.getText()));
		} catch (Exception e) {
			EditorLogger.error(e.getMessage());
			String msg = "Something went wrong while creating project.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(getStage(), msg, 2);
		}

		try {
			Ctx.project.loadProject(new File(location.getText() + "/"
					+ projectName.getText()));
		} catch (IOException | ParserConfigurationException | SAXException e) {
			EditorLogger.error(e.getMessage());
			String msg = "Something went wrong while creating project.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Ctx.msg.show(getStage(), msg, 2);
		}
		
		Ctx.msg.hide();
	}

	@Override
	protected boolean validateFields() {
		boolean isOk = true;

		if (projectName.getText().trim().isEmpty()) {
			projectName.setError(true);
			isOk = false;
		} else
			projectName.setError(false);

		if (location.getFile() != null) {
			location.setError(false);
		} else {
			location.setError(true);
			isOk = false;
		}

		return isOk;
	}
}

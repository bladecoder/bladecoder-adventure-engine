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
import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.common.ModelTools;
import com.bladecoder.engineeditor.common.RunProccess;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.FileInputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class CompileInkDialog extends EditDialog {

	private static final String FILE_PROP = "compileink.file";
	private static final String INKLECATE_PROP = "compileink.inklecate";
	private static final String LANG_PROP = "compileink.lang";
	private static final String EXTRACT_TEXTS_PROP = "compileink.extractTexts";

	private static final String INFO = "Compile the ink script using Inklecate.\n Inklecate must be installed in your computer, download here: https://github.com/inkle/ink/releases/latest.";

	private InputPanel inklecatePath;
	private InputPanel file;
	private InputPanel extractTexts;
	private InputPanel lang;

	public CompileInkDialog(Skin skin) {
		super("COMPILE INK SCRIPT", skin);

		inklecatePath = new FileInputPanel(skin, "Select the inklecate folder",
				"Select the folder where the inklecate is installed", FileInputPanel.DialogType.DIRECTORY);

		file = new FileInputPanel(skin, "Select the Ink script", "Select the Ink source script for your chapter",
				FileInputPanel.DialogType.OPEN_FILE);

		extractTexts = InputPanelFactory.createInputPanel(skin, "Extract texts",
				"Extracts all texts in a .properties for I18N.", Param.Type.BOOLEAN, true, "true");

		lang = InputPanelFactory.createInputPanel(skin, "Lang code",
				"The languaje code (ex. 'fr') where the texts are extracted. Empty for default.", false);

		addInputPanel(inklecatePath);
		addInputPanel(file);
		addInputPanel(extractTexts);
		addInputPanel(lang);

		setInfo(INFO);

		if (Ctx.project.getEditorConfig().getProperty(FILE_PROP) != null)
			file.setText(Ctx.project.getEditorConfig().getProperty(FILE_PROP));

		if (Ctx.project.getEditorConfig().getProperty(INKLECATE_PROP) != null)
			inklecatePath.setText(Ctx.project.getEditorConfig().getProperty(INKLECATE_PROP));

		lang.setText(Ctx.project.getEditorConfig().getProperty(LANG_PROP));

		if (Ctx.project.getEditorConfig().getProperty(EXTRACT_TEXTS_PROP) != null)
			extractTexts.setText(Ctx.project.getEditorConfig().getProperty(EXTRACT_TEXTS_PROP));
	}

	@Override
	protected void ok() {
		compileInk();

		Ctx.project.getEditorConfig().setProperty(FILE_PROP, file.getText());
		Ctx.project.getEditorConfig().setProperty(INKLECATE_PROP, inklecatePath.getText());

		if (lang.getText() != null)
			Ctx.project.getEditorConfig().setProperty(LANG_PROP, lang.getText());

		Ctx.project.getEditorConfig().setProperty(EXTRACT_TEXTS_PROP, extractTexts.getText());
	}

	@Override
	protected boolean validateFields() {
		boolean ok = true;

		if (!inklecatePath.validateField())
			ok = false;

		if (!file.validateField())
			ok = false;

		return ok;
	}

	private void compileInk() {
		String outfile = Ctx.project.getModelPath() + "/" + new File(file.getText()).getName() + ".json";
		List<String> params = new ArrayList<>();
		params.add("-o");
		params.add(outfile);
		params.add(file.getText());
		boolean ok = RunProccess.runInklecate(new File(inklecatePath.getText()), params);

		if (!ok) {
			Message.showMsgDialog(getStage(), "Error", "Error compiling Ink script.");
			return;
		}

		if (extractTexts.getText().equals("true")) {
			try {
				ModelTools.extractInkTexts(outfile, lang.getText());
			} catch (IOException e) {
				Message.showMsgDialog(getStage(), "Error extracting Ink texts.", e.getMessage());
				return;
			}
		}

		Message.showMsg(getStage(), "Ink script compiled successfully", 2);
	}
}

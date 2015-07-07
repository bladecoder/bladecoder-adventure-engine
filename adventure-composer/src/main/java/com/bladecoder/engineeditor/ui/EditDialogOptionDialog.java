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

import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;

public class EditDialogOptionDialog extends EditElementDialog {

	private InputPanel[] inputs;

	String attrs[] = { "text", "response_text", "verb", "next", "visible", "once" };

	public EditDialogOptionDialog(Skin skin, BaseDocument doc,
			Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel[attrs.length];
		
		inputs[0] = InputPanelFactory.createInputPanel(skin, "Text", "The sentence of the dialog to say by the player", Type.SMALL_TEXT, true);
		inputs[1] = InputPanelFactory.createInputPanel(skin, "Response Text", "The response by the character", Type.TEXT, false);
		inputs[2] = InputPanelFactory.createInputPanel(skin, "Verb", "The verb to execute when choosing this option");
		inputs[3] = InputPanelFactory.createInputPanel(skin, "Next Option",
						"The next option to show when this option is selected");
		inputs[4] = InputPanelFactory.createInputPanel(skin, "Visible", "The visibility", Param.Type.BOOLEAN, false);
		inputs[5] = InputPanelFactory.createInputPanel(skin, "Once", "When true, the option is hidden after selection", Param.Type.BOOLEAN, false);

		setInfo("A dialog is composed of an option tree. Each option is a dialog sentence that the user can choose to say");

		inputs[0].getCell(inputs[0].getField()).fillX();
		inputs[1].getCell(inputs[1].getField()).fillX();
		
		init(inputs, attrs, doc, parent, "option", e);
	}
}

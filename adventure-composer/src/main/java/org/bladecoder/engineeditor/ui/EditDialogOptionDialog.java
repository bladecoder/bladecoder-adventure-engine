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

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditDialogOptionDialog extends EditElementDialog {

	private InputPanel[] inputs;

	String attrs[] = { "text", "response_text", "verb", "next", "visible" };

	public EditDialogOptionDialog(Skin skin, BaseDocument doc,
			Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel[5];
		
		inputs[0] = new InputPanel(skin, "Text", "The sentence of the dialog to say by the player");
		inputs[1] = new InputPanel(skin, "Response Text", "The response by the character");
		inputs[2] = new InputPanel(skin, "Verb", "The verb to execute when choosing this option");
		inputs[3] = new InputPanel(skin, "Next Option",
						"The next option to show when this option is selected");
		inputs[4] = new InputPanel(skin, "Visible", "The visibility", Param.Type.BOOLEAN, false);

		setInfo("A dialog is composed of an option tree. Each option is a dialog sentence that the user can choose to say");

		init(inputs, attrs, doc, parent, "option", e);
	}
}

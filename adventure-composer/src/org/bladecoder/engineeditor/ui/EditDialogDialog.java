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

import org.bladecoder.engineeditor.model.BaseDocument;
import org.bladecoder.engineeditor.ui.components.EditElementDialog;
import org.bladecoder.engineeditor.ui.components.InputPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class EditDialogDialog extends EditElementDialog {
	public static final String INFO = "Actors can have several dialogs defined. Dialogs have a tree of options to choose";
	
	private InputPanel[] inputs; 

	String attrs[] = { "id"};	

	public EditDialogDialog(Skin skin,  BaseDocument doc, Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel[1];
		
		inputs[0] = new InputPanel(skin, "Dialog ID",
				"Select the dialog id to create.", true);

		setInfo(INFO);

		init(inputs, attrs, doc, parent, "dialog", e);
	}
}

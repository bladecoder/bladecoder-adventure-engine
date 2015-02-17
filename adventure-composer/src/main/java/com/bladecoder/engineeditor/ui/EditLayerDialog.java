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
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;

public class EditLayerDialog extends EditElementDialog {
	
	private InputPanel[] inputs;


	String attrs[] = { "id", "visible", "dynamic"};	

	public EditLayerDialog(Skin skin, BaseDocument doc, Element parent, Element e) {
		super(skin);
		
		inputs = new InputPanel [3];
		inputs[0] = new InputPanel(skin, "Layer Name", "The name of the layer");
		inputs[1] = new InputPanel(skin, "Visible", "Layer Visibility", Param.Type.BOOLEAN, true, "true");
		inputs[2] = new InputPanel(skin, "Dynamic", "True for actor reordering based in y position", Param.Type.BOOLEAN, false,"false");
		
		inputs[0].setMandatory(true);

		setInfo("Scenes can have a list of layers. Actors are added to a specific layer to control the draw order");

		init(inputs, attrs, doc, parent, "layer", e);
	}
}

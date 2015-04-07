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
import org.w3c.dom.NodeList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.model.ChapterDocument;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.ui.components.OptionsInputPanel;

public class EditActorDialog extends EditElementDialog {

	public static final String TYPES_INFO[] = {
			"No renderer actor, only define an interactive area",
			"Atlas actor allows 2d image and animations",
			"3d actors allow 3d models and animations",
			"Spine actors allow Spine 2d skeletal animations",
			"Image actors show image files"
			};

	private InputPanel[] inputs = new InputPanel[14];
	InputPanel typePanel;

	String attrs[] = { XMLConstants.TYPE_ATTR, XMLConstants.ID_ATTR, XMLConstants.LAYER_ATTR, XMLConstants.DESC_ATTR, XMLConstants.STATE_ATTR, XMLConstants.INTERACTION_ATTR, XMLConstants.VISIBLE_ATTR,
			XMLConstants.WALKING_SPEED_ATTR, XMLConstants.DEPTH_TYPE_ATTR, XMLConstants.SPRITE_SIZE_ATTR, XMLConstants.CAMERA_NAME_ATTR, XMLConstants.FOV_ATTR, XMLConstants.SCALE_ATTR, XMLConstants.ZINDEX_ATTR };

	@SuppressWarnings("unchecked")
	public EditActorDialog(Skin skin, BaseDocument doc, Element parent,
			Element e) {
		super(skin);

		inputs[0] = InputPanelFactory.createInputPanel(skin, "Actor Type",
				"Actors can be from different types",
				ChapterDocument.ACTOR_TYPES, true);

		inputs[1] = InputPanelFactory.createInputPanel(skin, "Actor ID",
				"IDs can not contain '.' or '_' characters.", true);

		inputs[2] = InputPanelFactory.createInputPanel(skin, "Actor Layer",
				"The layer for drawing order", getLayers(parent), true);
		
		inputs[3] = InputPanelFactory.createInputPanel(skin, "Description",
				"The text showed when the cursor is over the actor.");
		inputs[4] = InputPanelFactory.createInputPanel(
				skin,
				"State",
				"Initial state of the actor. Actors can be in differentes states during the game.");
		inputs[5] = InputPanelFactory.createInputPanel(skin, "Interaction",
				"True when the actor reacts to the user input.",
				Param.Type.BOOLEAN, false);
		inputs[6] = InputPanelFactory.createInputPanel(skin, "Visible", "The actor visibility.",
				Param.Type.BOOLEAN, false);
		inputs[7] = InputPanelFactory.createInputPanel(skin, "Walking Speed",
				"The walking speed in pix/sec. Default 700.", Param.Type.FLOAT,
				false);
		inputs[8] = InputPanelFactory.createInputPanel(skin, "Depth Type",
				"Scene fake depth for scaling", new String[] { "none",
						"vector"}, true);
		inputs[9] = InputPanelFactory.createInputPanel(skin, "Sprite Dimensions",
				"The size of the 3d sprite", Param.Type.DIMENSION, true);
		inputs[10] = InputPanelFactory.createInputPanel(skin, "Camera Name",
				"The name of the camera in the model", Param.Type.STRING, true,
				"Camera", null);
		inputs[11] = InputPanelFactory.createInputPanel(skin, "Camera FOV",
				"The camera field of view", Param.Type.FLOAT, true, "49.3",
				null);
		
		inputs[12] = InputPanelFactory.createInputPanel(skin, "Scale",
				"The sprite scale", Param.Type.FLOAT, false, "1",
				null);
		
		inputs[13] = InputPanelFactory.createInputPanel(skin, "zIndex",
				"The order to draw.", Param.Type.FLOAT, false, "0",
				null);

		setInfo(TYPES_INFO[0]);

		typePanel = inputs[0];

		((SelectBox<String>) typePanel.getField())
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						typeChanged();
					}
				});

		init(inputs, attrs, doc, parent, XMLConstants.ACTOR_TAG, e);

		typeChanged();

	}

	private String[] getLayers(Element parent) {
		NodeList layerList = parent.getElementsByTagName(XMLConstants.LAYER_TAG);
		
		String[] layers = new String[layerList.getLength()];
		
		for(int i = 0; i < layerList.getLength(); i++) {
			layers[i] = ((Element)(layerList.item(i))).getAttribute(XMLConstants.ID_ATTR);
		}
		
		return layers;
	}

	private void typeChanged() {
		int i = ((OptionsInputPanel)typePanel).getSelectedIndex();

		setInfo(TYPES_INFO[i]);

		setVisible(inputs[9],false);
		setVisible(inputs[10],false);
		setVisible(inputs[11],false);
		setVisible(inputs[12],false);

		if (ChapterDocument.ACTOR_TYPES[i]
				.equals(XMLConstants.S3D_VALUE)) {
			setVisible(inputs[9],true);
			setVisible(inputs[10],true);
			setVisible(inputs[11],true);
		}
		
		if (!ChapterDocument.ACTOR_TYPES[i]
				.equals(XMLConstants.NO_RENDERER_VALUE)) {
			setVisible(inputs[12],true);
		}
	}

	@Override
	protected void fill() {
		int i = ((OptionsInputPanel)typePanel).getSelectedIndex();
		if (((ChapterDocument)doc).getBBox(e) == null && ChapterDocument.ACTOR_TYPES[i]
				.equals(XMLConstants.NO_RENDERER_VALUE)) {
			((ChapterDocument) doc).setBbox(e, null);
		}
		
		if(((ChapterDocument)doc).getPos(e) == null)
			((ChapterDocument) doc).setPos(e, new Vector2(0, 0));

		super.fill();
	}
}

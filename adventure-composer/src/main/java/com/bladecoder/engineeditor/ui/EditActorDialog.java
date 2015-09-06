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

import com.bladecoder.engineeditor.ui.components.OptionsInputPanel;
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

public class EditActorDialog extends EditElementDialog {

	public static final String TYPES_INFO[] = {
			"Background actors don't have sprites or animations. The are used to use objects drawed in the background",
			"Sprite actors have one or several sprites or animations",
			"Character actors have dialogs and stand, walk and talk animations",
			"Obstacle actors forbids zones for walking actors"
			};
	
	public static final String RENDERERS_INFO[] = {
			"Atlas actor allows 2d image and animations",
			"Spine actors allow Spine 2d skeletal animations",
			"3d actors allow 3d models and animations",
			"Image actors show image files"
			};

	private InputPanel typePanel;
	private InputPanel rendererPanel;

	String attrs[] = { XMLConstants.TYPE_ATTR, XMLConstants.ID_ATTR, XMLConstants.LAYER_ATTR, XMLConstants.VISIBLE_ATTR, XMLConstants.INTERACTION_ATTR, XMLConstants.DESC_ATTR, XMLConstants.STATE_ATTR,
			 XMLConstants.RENDERER_ATTR, XMLConstants.DEPTH_TYPE_ATTR, XMLConstants.SCALE_ATTR, XMLConstants.ZINDEX_ATTR, XMLConstants.WALKING_SPEED_ATTR, XMLConstants.SPRITE_SIZE_ATTR, XMLConstants.CAMERA_NAME_ATTR, XMLConstants.FOV_ATTR,
			 XMLConstants.TEXT_COLOR_ATTR};
	
	private InputPanel[] inputs = new InputPanel[attrs.length];

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
		
		inputs[3] = InputPanelFactory.createInputPanel(skin, "Visible", "The actor visibility.",
				Param.Type.BOOLEAN, false);
		
		
		inputs[4] = InputPanelFactory.createInputPanel(skin, "Interaction",
				"True when the actor reacts to the user input.",
				Param.Type.BOOLEAN, false);

		inputs[5] = InputPanelFactory.createInputPanel(skin, "Description",
				"The text showed when the cursor is over the actor.");
		inputs[6] = InputPanelFactory.createInputPanel(
				skin,
				"State",
				"Initial state of the actor. Actors can be in differentes states during the game.");		
		
		inputs[7] = InputPanelFactory.createInputPanel(skin, "Actor Renderer",
				"Actors can be renderer from several sources",
				ChapterDocument.ACTOR_RENDERERS, true);

		inputs[8] = InputPanelFactory.createInputPanel(skin, "Depth Type",
				"Scene fake depth for scaling", new String[] { "none",
						"vector"}, true);
		
		inputs[9] = InputPanelFactory.createInputPanel(skin, "Scale",
				"The sprite scale", Param.Type.FLOAT, false, "1");
		
		inputs[10] = InputPanelFactory.createInputPanel(skin, "zIndex",
				"The order to draw.", Param.Type.FLOAT, false, "0");
		
		inputs[11] = InputPanelFactory.createInputPanel(skin, "Walking Speed",
				"The walking speed in pix/sec. Default 700.", Param.Type.FLOAT,
				false);
		
		inputs[12] = InputPanelFactory.createInputPanel(skin, "Sprite Dimensions",
				"The size of the 3d sprite", Param.Type.DIMENSION, true);
		inputs[13] = InputPanelFactory.createInputPanel(skin, "Camera Name",
				"The name of the camera in the model", Param.Type.STRING, true,
				"Camera");
		inputs[14] = InputPanelFactory.createInputPanel(skin, "Camera FOV",
				"The camera field of view", Param.Type.FLOAT, true, "49.3");
		
		inputs[15] = InputPanelFactory.createInputPanel(skin, "Text Color",
				"The text color when the actor talks", Param.Type.COLOR, false);

		setInfo(TYPES_INFO[0]);

		typePanel = inputs[0];
		rendererPanel = inputs[7];

		((SelectBox<String>) typePanel.getField())
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						typeChanged();
					}
				});
		
		((SelectBox<String>) rendererPanel.getField())
			.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				rendererChanged();
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
		
		hideAllInputs();
		
		if (!ChapterDocument.ACTOR_TYPES[i]
				.equals(XMLConstants.OBSTACLE_VALUE)) {
			setVisible(inputs[4],true);
			setVisible(inputs[5],true);
			setVisible(inputs[6],true);
			setVisible(inputs[10],true);
		}

		if (ChapterDocument.ACTOR_TYPES[i]
				.equals(XMLConstants.SPRITE_VALUE) || 
				ChapterDocument.ACTOR_TYPES[i]
						.equals(XMLConstants.CHARACTER_VALUE)) {
			setVisible(inputs[7],true);
			setVisible(inputs[8],true);
			setVisible(inputs[9],true);
		}
		
		if (ChapterDocument.ACTOR_TYPES[i]
						.equals(XMLConstants.CHARACTER_VALUE)) {
			setVisible(inputs[11],true);
			setVisible(inputs[15],true);
		}
		
		rendererChanged();
	}
	
	private void rendererChanged() {
		int i = ((OptionsInputPanel)rendererPanel).getSelectedIndex();

//		setInfo(RENDERERS_INFO[i]);

		setVisible(inputs[12],false);
		setVisible(inputs[13],false);
		setVisible(inputs[14],false);

		if (rendererPanel.isVisible() &&
				ChapterDocument.ACTOR_RENDERERS[i]
				.equals(XMLConstants.S3D_VALUE)) {
			setVisible(inputs[12],true);
			setVisible(inputs[13],true);
			setVisible(inputs[14],true);
		}
	}
	
	private void hideAllInputs() {
				
		for(int idx = 4; idx < inputs.length; idx ++) {
			InputPanel i = inputs[idx];
			
			setVisible(i, false);
		}
	}

	@Override
	protected void fill() {
		int i = ((OptionsInputPanel)typePanel).getSelectedIndex();
		if (e.getAttribute(XMLConstants.BBOX_ATTR).isEmpty() && ChapterDocument.ACTOR_TYPES[i]
				.equals(XMLConstants.BACKGROUND_VALUE) || ChapterDocument.ACTOR_TYPES[i]
						.equals(XMLConstants.OBSTACLE_VALUE)) {
			((ChapterDocument) doc).setBbox(e, null);
		}
		
		if(((ChapterDocument)doc).getPos(e) == null)
			((ChapterDocument) doc).setPos(e, new Vector2(0, 0));

		super.fill();
	}
}

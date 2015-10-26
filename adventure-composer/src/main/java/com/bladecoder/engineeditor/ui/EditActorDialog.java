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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.SpriteActor.DepthType;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.ui.components.OptionsInputPanel;

public class EditActorDialog extends EditModelDialog<Scene, BaseActor> {
	
	public static final String ACTOR_TYPES[] = { XMLConstants.BACKGROUND_VALUE, XMLConstants.SPRITE_VALUE,
			XMLConstants.CHARACTER_VALUE, XMLConstants.OBSTACLE_VALUE, XMLConstants.ANCHOR_VALUE };

	public static final String ACTOR_RENDERERS[] = { XMLConstants.ATLAS_VALUE, XMLConstants.SPINE_VALUE,
			XMLConstants.S3D_VALUE, XMLConstants.IMAGE_VALUE };

	public static final String TYPES_INFO[] = {
			"Background actors don't have sprites or animations. The are used to use objects drawed in the background",
			"Sprite actors have one or several sprites or animations",
			"Character actors have dialogs and stand, walk and talk animations",
			"Obstacle actors forbids zones for walking actors",
			"Anchor actors are used por positioning other actors"
			};
	
	public static final String RENDERERS_INFO[] = {
			"Atlas actor allows 2d image and animations",
			"Spine actors allow Spine 2d skeletal animations",
			"3d actors allow 3d models and animations",
			"Image actors show image files"
			};

	private InputPanel typePanel;
	private InputPanel id;
	private InputPanel layer;
	private InputPanel visible;
	private InputPanel interaction;
	private InputPanel desc;
	private InputPanel state;
	private InputPanel renderer;
	private InputPanel depthType;
	private InputPanel scale;
	private InputPanel zIndex;
	private InputPanel walkingSpeed;
	private InputPanel spriteSize;
	private InputPanel cameraName;
	private InputPanel fov;
	private InputPanel textColor;

	InputPanel inputs[] = { typePanel, id, layer, visible, interaction, desc, state,
			 renderer, depthType, scale, zIndex, walkingSpeed, spriteSize, cameraName, fov,
			 textColor};

	@SuppressWarnings("unchecked")
	public EditActorDialog(Skin skin, Scene parent,	BaseActor e) {
		super(skin);

		typePanel = InputPanelFactory.createInputPanel(skin, "Actor Type",
				"Actors can be from different types",
				ACTOR_TYPES, true);

		id = InputPanelFactory.createInputPanel(skin, "Actor ID",
				"IDs can not contain '.' or '_' characters.", true);
		

		layer = InputPanelFactory.createInputPanel(skin, "Actor Layer",
				"The layer for drawing order", getLayers(parent), true);
		
		visible = InputPanelFactory.createInputPanel(skin, "Visible", "The actor visibility.",
				Param.Type.BOOLEAN, false);
		
		
		interaction = InputPanelFactory.createInputPanel(skin, "Interaction",
				"True when the actor reacts to the user input.",
				Param.Type.BOOLEAN, false);

		desc = InputPanelFactory.createInputPanel(skin, "Description",
				"The text showed when the cursor is over the actor.");
		state = InputPanelFactory.createInputPanel(
				skin,
				"State",
				"Initial state of the actor. Actors can be in differentes states during the game.");		
		
		renderer = InputPanelFactory.createInputPanel(skin, "Actor Renderer",
				"Actors can be renderer from several sources",
				ACTOR_RENDERERS, true);

		depthType = InputPanelFactory.createInputPanel(skin, "Depth Type",
				"Scene fake depth for scaling", new String[] { "none",
						"vector"}, true);
		
		scale = InputPanelFactory.createInputPanel(skin, "Scale",
				"The sprite scale", Param.Type.FLOAT, false, "1");
		
		zIndex = InputPanelFactory.createInputPanel(skin, "zIndex",
				"The order to draw.", Param.Type.FLOAT, false, "0");
		
		walkingSpeed = InputPanelFactory.createInputPanel(skin, "Walking Speed",
				"The walking speed in pix/sec. Default 700.", Param.Type.FLOAT,
				false);
		
		spriteSize = InputPanelFactory.createInputPanel(skin, "Sprite Dimensions",
				"The size of the 3d sprite", Param.Type.DIMENSION, true);
		cameraName = InputPanelFactory.createInputPanel(skin, "Camera Name",
				"The name of the camera in the model", Param.Type.STRING, true,
				"Camera");
		fov = InputPanelFactory.createInputPanel(skin, "Camera FOV",
				"The camera field of view", Param.Type.FLOAT, true, "49.3");
		
		textColor = InputPanelFactory.createInputPanel(skin, "Text Color",
				"The text color when the actor talks", Param.Type.COLOR, false);

		setInfo(TYPES_INFO[0]);

		((SelectBox<String>) typePanel.getField())
				.addListener(new ChangeListener() {

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						typeChanged();
					}
				});
		
		((SelectBox<String>) renderer.getField())
			.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				rendererChanged();
			}
		});

		init(parent, e, inputs);

		typeChanged();

	}

	private String[] getLayers(Scene parent) {
		String[] result = new String[parent.getLayers().size()];
		
		for(int i = 0; i < parent.getLayers().size(); i++) {
			result[i] = parent.getLayers().get(i).getName();
		}
		
		return result;
	}

	private void typeChanged() {
		int i = ((OptionsInputPanel)typePanel).getSelectedIndex();

		setInfo(TYPES_INFO[i]);
		
		hideAllInputs();
		
		if (!ACTOR_TYPES[i]
				.equals(XMLConstants.ANCHOR_VALUE)) {
			setVisible(inputs[3], true);
		}
		
		if (!ACTOR_TYPES[i]
				.equals(XMLConstants.OBSTACLE_VALUE) && 
				!ACTOR_TYPES[i]
						.equals(XMLConstants.ANCHOR_VALUE)
				) {
			setVisible(inputs[2], true);			
			setVisible(inputs[4],true);
			setVisible(inputs[5],true);
			setVisible(inputs[6],true);
			setVisible(inputs[10],true);
		}

		if (ACTOR_TYPES[i]
				.equals(XMLConstants.SPRITE_VALUE) || 
				ACTOR_TYPES[i]
						.equals(XMLConstants.CHARACTER_VALUE)) {
			setVisible(inputs[7],true);
			setVisible(inputs[8],true);
			setVisible(inputs[9],true);
		}
		
		if (ACTOR_TYPES[i]
						.equals(XMLConstants.CHARACTER_VALUE)) {
			setVisible(inputs[11],true);
			setVisible(inputs[15],true);
		}
		
		rendererChanged();
	}
	
	private void rendererChanged() {
		int i = ((OptionsInputPanel)renderer).getSelectedIndex();

//		setInfo(RENDERERS_INFO[i]);

		setVisible(inputs[12],false);
		setVisible(inputs[13],false);
		setVisible(inputs[14],false);

		if (renderer.isVisible() &&
				ACTOR_RENDERERS[i]
				.equals(XMLConstants.S3D_VALUE)) {
			setVisible(inputs[12],true);
			setVisible(inputs[13],true);
			setVisible(inputs[14],true);
		}
	}
	
	private void hideAllInputs() {
		
		for(int idx = 2; idx < inputs.length; idx ++) {
			InputPanel i = inputs[idx];
			
			setVisible(i, false);
		}
	}
	
	@Override
	protected void inputsToModel(boolean create) {
		
		if(create) {
			String type = typePanel.getText();
			
			if(type.equals(XMLConstants.BACKGROUND_VALUE)) {
				e = new InteractiveActor();
			} else if(type.equals(XMLConstants.SPRITE_VALUE)) {
				e = new SpriteActor();
			} else if(type.equals(XMLConstants.CHARACTER_VALUE)) {
				e = new CharacterActor();
			} else if(type.equals(XMLConstants.OBSTACLE_VALUE)) {
				e = new ObstacleActor();
			} else if(type.equals(XMLConstants.ANCHOR_VALUE)) {
				e = new AnchorActor();
			}
		}
	 
		e.setId(id.getText());
		e.setVisible(Boolean.parseBoolean(visible.getText()));
		
		if(e instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) e;
			
			ia.setLayer(layer.getText());
			ia.setInteraction(Boolean.parseBoolean(interaction.getText()));
			ia.setDesc(desc.getText());
			ia.setState(state.getText());
			
			if(e instanceof SpriteActor) {
				SpriteActor sa = (SpriteActor) e;
				
				String rendererType = renderer.getText();
				
				if(XMLConstants.ATLAS_VALUE.equals(rendererType)) {
					sa.setRenderer(new AtlasRenderer());
				} else if(XMLConstants.IMAGE_VALUE.equals(rendererType)) {
					sa.setRenderer(new ImageRenderer());
				} else if(XMLConstants.S3D_VALUE.equals(rendererType)) {
					Sprite3DRenderer r = new Sprite3DRenderer();
					sa.setRenderer(r);
					r.setCameraFOV(Float.parseFloat(fov.getText()));
					r.setCameraName(cameraName.getText());
					r.setSpriteSize(Param.parseVector2(spriteSize.getText()));
				} else if(XMLConstants.SPINE_VALUE.equals(rendererType)) {
					sa.setRenderer(new SpineRenderer());
				}
				
				sa.setDepthType(DepthType.valueOf(depthType.getText()));
				sa.setScale(Float.parseFloat(scale.getText()));
				sa.setZIndex(Float.parseFloat(zIndex.getText()));
				
				if(e instanceof CharacterActor) {
					CharacterActor ca = (CharacterActor) e;
					
					ca.setWalkingSpeed(Float.parseFloat(walkingSpeed.getText()));
					ca.setTextColor(Param.parseColor(textColor.getText()));
				}
			}
		}
		
		if(create) {
			parent.addActor(e);
		}

		// TODO UNDO OP
//		UndoOp undoOp = new UndoAddElement(doc, e);
//		Ctx.project.getUndoStack().add(undoOp);
		
		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {
		
		id.setText(e.getId());
		visible.setText(Boolean.toString(e.isVisible()));
		
		if(e instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) e;
			layer.setText(ia.getLayer());
			interaction.setText(Boolean.toString(ia.hasInteraction()));
			desc.setText(ia.getDesc());
			state.setText(ia.getState());
			
			if(e instanceof SpriteActor) {
				SpriteActor sa = (SpriteActor) e;
				
				ActorRenderer r = sa.getRenderer();
				
				if(r instanceof AtlasRenderer) {
					renderer.setText(XMLConstants.ATLAS_VALUE);
				} else if(r instanceof ImageRenderer) {
					renderer.setText(XMLConstants.IMAGE_VALUE);
				} else if(r instanceof Sprite3DRenderer) {
					renderer.setText(XMLConstants.S3D_VALUE);
					Sprite3DRenderer s3d = (Sprite3DRenderer)r;
					
					fov.setText(Float.toString(s3d.getCameraFOV()));
					cameraName.setText(s3d.getCameraName());
					spriteSize.setText(Param.toStringParam(s3d.getSpriteSize()));
				} else if(r instanceof SpineRenderer) {
					renderer.setText(XMLConstants.SPINE_VALUE);
				}
				
				depthType.setText(sa.getDepthType().toString());
				scale.setText(Float.toString(sa.getScale()));
				zIndex.setText(Float.toString(sa.getZIndex()));
				
				if(e instanceof CharacterActor) {
					CharacterActor ca = (CharacterActor) e;
					
					walkingSpeed.setText(Float.toString(ca.getWalkingSpeed()));
					textColor.setText(ca.getTextColor().toString());
				}
			}
		} else if(e instanceof AnchorActor) {
			typePanel.setText(XMLConstants.ANCHOR_VALUE);
		} else if(e instanceof ObstacleActor) {
			typePanel.setText(XMLConstants.OBSTACLE_VALUE);
		}

	}
}

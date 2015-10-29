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
import com.bladecoder.engine.i18n.I18N;
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
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.ui.components.OptionsInputPanel;

public class EditActorDialog extends EditModelDialog<Scene, BaseActor> {
	
	private final static String BACKGROUND_TYPE_STR = "background";
	private final static String SPRITE_TYPE_STR = "sprite";
	private final static String CHARACTER_TYPE_STR = "character";
	private final static String OBSTACLE_TYPE_STR = "obstacle";
	private final static String ANCHOR_TYPE_STR = "anchor";
	
	public static final String ACTOR_TYPES[] = { BACKGROUND_TYPE_STR, SPRITE_TYPE_STR,
			CHARACTER_TYPE_STR, OBSTACLE_TYPE_STR, ANCHOR_TYPE_STR };

	public static final String ACTOR_RENDERERS[] = { Project.ATLAS_RENDERER_STRING, Project.SPINE_RENDERER_STRING,
			Project.IMAGE_RENDERER_STRING, Project.S3D_RENDERER_STRING };

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

		init(parent, e, new InputPanel[] { typePanel, id, layer, visible, interaction, desc, state,
			 renderer, depthType, scale, zIndex, walkingSpeed, spriteSize, cameraName, fov,
			 textColor});

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
				.equals(ANCHOR_TYPE_STR)) {
			setVisible(visible, true);
		}
		
		if (!ACTOR_TYPES[i]
				.equals(OBSTACLE_TYPE_STR) && 
				!ACTOR_TYPES[i]
						.equals(ANCHOR_TYPE_STR)
				) {
			setVisible(layer, true);			
			setVisible(interaction,true);
			setVisible(desc,true);
			setVisible(state,true);
			setVisible(zIndex,true);
		}

		if (ACTOR_TYPES[i]
				.equals(SPRITE_TYPE_STR) || 
				ACTOR_TYPES[i]
						.equals(CHARACTER_TYPE_STR)) {
			setVisible(renderer,true);
			setVisible(depthType,true);
			setVisible(scale,true);
		}
		
		if (ACTOR_TYPES[i]
						.equals(CHARACTER_TYPE_STR)) {
			setVisible(walkingSpeed,true);
			setVisible(textColor,true);
		}
		
		rendererChanged();
	}
	
	private void rendererChanged() {
		int i = ((OptionsInputPanel)renderer).getSelectedIndex();

//		setInfo(RENDERERS_INFO[i]);

		setVisible(spriteSize,false);
		setVisible(cameraName,false);
		setVisible(fov,false);

		if (renderer.isVisible() &&
				ACTOR_RENDERERS[i]
				.equals(Project.S3D_RENDERER_STRING)) {
			setVisible(spriteSize,true);
			setVisible(cameraName,true);
			setVisible(fov,true);
		}
	}
	
	private void hideAllInputs() {
		
		for(int idx = 2; idx < i.length; idx ++) {
			InputPanel ip = i[idx];
			
			setVisible(ip, false);
		}
	}
	
	@Override
	protected void inputsToModel(boolean create) {
		
		if(create) {
			String type = typePanel.getText();
			
			if(type.equals(BACKGROUND_TYPE_STR)) {
				e = new InteractiveActor();
			} else if(type.equals(SPRITE_TYPE_STR)) {
				e = new SpriteActor();
			} else if(type.equals(CHARACTER_TYPE_STR)) {
				e = new CharacterActor();
			} else if(type.equals(OBSTACLE_TYPE_STR)) {
				e = new ObstacleActor();
			} else if(type.equals(ANCHOR_TYPE_STR)) {
				e = new AnchorActor();
			}
		}
	 
		e.setId(id.getText());
		e.setVisible(Boolean.parseBoolean(visible.getText()));
		
		if(e instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) e;
			
			ia.setLayer(layer.getText());
			ia.setInteraction(Boolean.parseBoolean(interaction.getText()));
			
			String key = I18N.PREFIX + parent.getId() + "." + e.getId() + ".desc";
			Ctx.project.getI18N().setTranslation(key, desc.getText());
			ia.setDesc(key);
			ia.setState(state.getText());
			
			if(e instanceof SpriteActor) {
				SpriteActor sa = (SpriteActor) e;
				
				String rendererType = renderer.getText();
				
				if(Project.ATLAS_RENDERER_STRING.equals(rendererType)) {
					sa.setRenderer(new AtlasRenderer());
				} else if(Project.IMAGE_RENDERER_STRING.equals(rendererType)) {
					sa.setRenderer(new ImageRenderer());
				} else if(Project.S3D_RENDERER_STRING.equals(rendererType)) {
					Sprite3DRenderer r = new Sprite3DRenderer();
					sa.setRenderer(r);
					r.setCameraFOV(Float.parseFloat(fov.getText()));
					r.setCameraName(cameraName.getText());
					r.setSpriteSize(Param.parseVector2(spriteSize.getText()));
				} else if(Project.SPINE_RENDERER_STRING.equals(rendererType)) {
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
			desc.setText(Ctx.project.translate(ia.getDesc()));
			state.setText(ia.getState());
			
			if(e instanceof SpriteActor) {
				SpriteActor sa = (SpriteActor) e;
				
				ActorRenderer r = sa.getRenderer();
				
				if(r instanceof AtlasRenderer) {
					renderer.setText(Project.ATLAS_RENDERER_STRING);
				} else if(r instanceof ImageRenderer) {
					renderer.setText(Project.IMAGE_RENDERER_STRING);
				} else if(r instanceof Sprite3DRenderer) {
					renderer.setText(Project.S3D_RENDERER_STRING);
					Sprite3DRenderer s3d = (Sprite3DRenderer)r;
					
					fov.setText(Float.toString(s3d.getCameraFOV()));
					cameraName.setText(s3d.getCameraName());
					spriteSize.setText(Param.toStringParam(s3d.getSpriteSize()));
				} else if(r instanceof SpineRenderer) {
					renderer.setText(Project.SPINE_RENDERER_STRING);
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
			typePanel.setText(ANCHOR_TYPE_STR);
		} else if(e instanceof ObstacleActor) {
			typePanel.setText(OBSTACLE_TYPE_STR);
		}

	}
}

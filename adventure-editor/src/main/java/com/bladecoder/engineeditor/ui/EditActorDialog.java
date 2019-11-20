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

import java.util.HashMap;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.AtlasRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.ImageRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.ParticleRenderer;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.Sprite3DRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.TextRenderer;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.WalkZoneActor;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.AlignUtils;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.FilteredSelectBox;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;
import com.bladecoder.engineeditor.ui.panels.OptionsInputPanel;

public class EditActorDialog extends EditModelDialog<Scene, BaseActor> {

	private final static float DEFAULT_DIM = 200;

	private final static String BACKGROUND_TYPE_STR = "background";
	private final static String SPRITE_TYPE_STR = "sprite";
	private final static String CHARACTER_TYPE_STR = "character";
	private final static String OBSTACLE_TYPE_STR = "obstacle";
	private final static String ANCHOR_TYPE_STR = "anchor";
	private final static String WALKZONE_TYPE_STR = "walkzone";

	private static final String ACTOR_TYPES[] = { BACKGROUND_TYPE_STR, SPRITE_TYPE_STR, CHARACTER_TYPE_STR,
			ANCHOR_TYPE_STR, WALKZONE_TYPE_STR, OBSTACLE_TYPE_STR };

	private static final String ACTOR_RENDERERS[] = { Project.ATLAS_RENDERER_STRING, Project.SPINE_RENDERER_STRING,
			Project.IMAGE_RENDERER_STRING, Project.S3D_RENDERER_STRING, Project.PARTICLE_RENDERER_STRING,
			Project.TEXT_RENDERER_STRING };

	private static final String TYPES_INFO[] = {
			"Background actors don't have sprites or animations. They are used to interact with objects drawn in the background",
			"Sprite actors have one or several sprites or animations",
			"Character actors have dialogs and stand, walk and talk animations",
			"Anchor actors are used as reference for positioning other actors",
			"Define the walkable area for characters", "Obstacle actors forbids zones for walking actors" };

	private static final String TEXT_ALIGN[] = { "left", "center", "right" };
	private static final String ORG_ALIGN[] = { "bottom", "center", "left", "right", "top", "botton-right",
			"botton-left", "top-right", "top-left" };

	private InputPanel typePanel;
	private InputPanel id;
	private InputPanel layer;
	private InputPanel visible;
	private InputPanel interaction;
	private InputPanel desc;
	private InputPanel state;
	private InputPanel renderer;
	private InputPanel fakeDepth;
	private InputPanel pos;
	private InputPanel refPoint;
	private InputPanel scale;
	private InputPanel rot;
	private InputPanel tint;
	private InputPanel bboxFromRenderer;
	private InputPanel zIndex;
	private InputPanel walkingSpeed;
	private InputPanel talkingTextPos;

	private InputPanel orgAlign;

	private InputPanel textColor;
	private InputPanel textStyle;

	// Spine Renderer
	private InputPanel spineSkin;

	// 3d Renderer
	private InputPanel spriteSize;
	private InputPanel cameraName;
	private InputPanel fov;

	// Particle Renderer
	private InputPanel particleName;
	private InputPanel particleAtlas;

	// Text Renderer
	private InputPanel text;
	private InputPanel font;
	private InputPanel size;
	private InputPanel textAlign;
	private InputPanel borderWidth;
	private InputPanel borderColor;
	private InputPanel borderStraight;
	private InputPanel shadowOffsetX;
	private InputPanel shadowOffsetY;
	private InputPanel shadowColor;

	@SuppressWarnings("unchecked")
	public EditActorDialog(Skin skin, Scene parent, BaseActor e) {
		super(skin);

		typePanel = InputPanelFactory.createInputPanel(skin, "Actor Type", "Actors can be from different types",
				ACTOR_TYPES, true);

		id = InputPanelFactory.createInputPanel(skin, "Actor ID", "IDs can not contain '.' character.", true);

		layer = InputPanelFactory.createInputPanel(skin, "Actor Layer", "The layer for drawing order",
				getLayers(parent), true);

		visible = InputPanelFactory.createInputPanel(skin, "Visible", "The actor visibility.", Param.Type.BOOLEAN, true,
				"true");

		interaction = InputPanelFactory.createInputPanel(skin, "Interaction",
				"True when the actor reacts to the user input.", Param.Type.BOOLEAN, true, "true");

		desc = InputPanelFactory.createInputPanel(skin, "Description",
				"The text showed when the cursor is over the actor.");
		state = InputPanelFactory.createInputPanel(skin, "State",
				"Initial state of the actor. Actors can be in several states along the game.");

		renderer = InputPanelFactory.createInputPanel(skin, "Actor Renderer",
				"Actors can be renderer from several sources", ACTOR_RENDERERS, true);

		fakeDepth = InputPanelFactory.createInputPanel(skin, "Fake Depth", "Scene fake depth for scaling",
				Param.Type.BOOLEAN, true, "false");

		pos = InputPanelFactory.createInputPanel(skin, "Position", "The sprite position.", Param.Type.VECTOR2, true,
				"0,0");
		refPoint = InputPanelFactory.createInputPanel(skin, "Ref. Point",
				"Point of reference to relative position other actors.", Param.Type.VECTOR2, true, "0,0");
		scale = InputPanelFactory.createInputPanel(skin, "Scale", "The sprite scale.", Param.Type.VECTOR2, true, "1,1");

		rot = InputPanelFactory.createInputPanel(skin, "Rotation", "The sprite rotation.", Param.Type.FLOAT, true, "0");

		tint = InputPanelFactory.createInputPanel(skin, "Tint", "Draw the actor with the specified color (RRGGBBAA).",
				Param.Type.COLOR, false);

		bboxFromRenderer = InputPanelFactory.createInputPanel(skin, "BBox From Renderer",
				"Sets the actor bounding box automatically from the sprite dimensions.", Param.Type.BOOLEAN, true,
				"true");

		zIndex = InputPanelFactory.createInputPanel(skin, "zIndex", "The order to draw.", Param.Type.FLOAT, false, "0");

		orgAlign = InputPanelFactory.createInputPanel(skin, "orgAlign",
				"Alignment of the origin for rotation and scale.", ORG_ALIGN, true);

		walkingSpeed = InputPanelFactory.createInputPanel(skin, "Walking Speed",
				"The walking speed in pix/sec. Default 700.", Param.Type.FLOAT, true,
				Float.toString(CharacterActor.DEFAULT_WALKING_SPEED));

		spineSkin = InputPanelFactory.createInputPanel(skin, "Skin", "The initial skin.");

		spriteSize = InputPanelFactory.createInputPanel(skin, "Sprite Dimensions", "The size of the 3d sprite.",
				Param.Type.DIMENSION, true);
		cameraName = InputPanelFactory.createInputPanel(skin, "Camera Name", "The name of the camera in the model.",
				Param.Type.STRING, true, "Camera");
		fov = InputPanelFactory.createInputPanel(skin, "Camera FOV", "The camera field of view.", Param.Type.FLOAT,
				true, "49.3");

		textColor = InputPanelFactory.createInputPanel(skin, "Text Color",
				"The text color (RRGGBBAA) when the actor talks.", Param.Type.COLOR, false);

		textStyle = InputPanelFactory.createInputPanel(skin, "Text Style",
				"The style to use (an entry in your `ui.json` in the `com.bladecoder.engine.ui.TextManagerUI$TextManagerUIStyle` section).",
				Param.Type.STRING, false);

		talkingTextPos = InputPanelFactory.createInputPanel(skin, "Talking Text Pos",
				"Position of the text when talking. Relative to the character position.", Param.Type.VECTOR2, false);

		particleName = InputPanelFactory.createInputPanel(skin, "Particle Name", "The name of the particle system.",
				Type.PARTICLE_ASSET, true);

		particleAtlas = InputPanelFactory.createInputPanel(skin, "Particle Atlas",
				"The atlas used by the particle system.", Type.ATLAS_ASSET, true);

		text = InputPanelFactory.createInputPanel(skin, "Text", "The text to draw.", Type.SMALL_TEXT, true);
		text.getCell(text.getField()).fillX();

		font = InputPanelFactory.createInputPanel(skin, "Font", "Select the font name.", Type.FONT_ASSET, true);
		size = InputPanelFactory.createInputPanel(skin, "Size", "The size of the text.", Type.INTEGER, true, "20");
		textAlign = InputPanelFactory.createInputPanel(skin, "Text Align", "The alignment of the text.", TEXT_ALIGN,
				true);
		borderWidth = InputPanelFactory.createInputPanel(skin, "Border Width", "Zero for no border.", Type.INTEGER,
				true, "0");
		borderColor = InputPanelFactory.createInputPanel(skin, "Border Color", "The Border Color.", Type.COLOR, true,
				"black");
		borderStraight = InputPanelFactory.createInputPanel(skin, "Border Straigh", "Is the border straight?",
				Type.BOOLEAN, true);
		shadowOffsetX = InputPanelFactory.createInputPanel(skin, "Shadow Offset X", "The Shadow X offset.",
				Type.INTEGER, true, "0");
		shadowOffsetY = InputPanelFactory.createInputPanel(skin, "Shadow Offset Y", "The Shadow Y offset.",
				Type.INTEGER, true, "0");
		shadowColor = InputPanelFactory.createInputPanel(skin, "Shadow Color", "The shadow Color.", Type.COLOR, true,
				"black");

		setInfo(TYPES_INFO[0]);

		((FilteredSelectBox<String>) typePanel.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				typeChanged();
			}
		});

		((FilteredSelectBox<String>) renderer.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				rendererChanged();
			}
		});

		init(parent, e,
				new InputPanel[] { typePanel, id, renderer, particleName, particleAtlas, layer, visible, interaction,
						desc, state, fakeDepth, pos, refPoint, scale, rot, tint, text, font, size, textAlign,
						borderWidth, borderColor, borderStraight, shadowOffsetX, shadowOffsetY, shadowColor,
						bboxFromRenderer, zIndex, orgAlign, walkingSpeed, talkingTextPos, spineSkin, spriteSize,
						cameraName, fov, textColor, textStyle });

		typeChanged();

	}

	private String[] getLayers(Scene parent) {
		String[] result = new String[parent.getLayers().size()];

		for (int i = 0; i < parent.getLayers().size(); i++) {
			result[i] = parent.getLayers().get(i).getName();
		}

		return result;
	}

	private void typeChanged() {
		int i = ((OptionsInputPanel) typePanel).getSelectedIndex();

		setInfo(TYPES_INFO[i]);

		hideAllInputs();

		setVisible(pos, true);

		if (!ACTOR_TYPES[i].equals(ANCHOR_TYPE_STR) && !ACTOR_TYPES[i].equals(WALKZONE_TYPE_STR)) {
			setVisible(visible, true);
		}

		if (!ACTOR_TYPES[i].equals(OBSTACLE_TYPE_STR) && !ACTOR_TYPES[i].equals(ANCHOR_TYPE_STR)
				&& !ACTOR_TYPES[i].equals(WALKZONE_TYPE_STR)) {
			setVisible(layer, true);
			setVisible(interaction, true);
			setVisible(desc, true);
			setVisible(state, true);
			setVisible(zIndex, true);
			setVisible(refPoint, true);
		}

		if (ACTOR_TYPES[i].equals(SPRITE_TYPE_STR) || ACTOR_TYPES[i].equals(CHARACTER_TYPE_STR)) {
			setVisible(renderer, true);
			setVisible(fakeDepth, true);
			setVisible(scale, true);
			setVisible(rot, true);
			setVisible(tint, true);
			setVisible(bboxFromRenderer, true);
			setVisible(orgAlign, true);
		}

		if (ACTOR_TYPES[i].equals(CHARACTER_TYPE_STR)) {
			setVisible(walkingSpeed, true);
			setVisible(textColor, true);
			setVisible(textStyle, true);
			setVisible(talkingTextPos, true);
		}

		rendererChanged();
	}

	private void rendererChanged() {
		int i = ((OptionsInputPanel) renderer).getSelectedIndex();

		// setInfo(RENDERERS_INFO[i]);

		setVisible(spineSkin, false);

		setVisible(spriteSize, false);
		setVisible(cameraName, false);
		setVisible(fov, false);

		setVisible(particleName, false);
		setVisible(particleAtlas, false);

		setVisible(text, false);
		setVisible(font, false);
		setVisible(size, false);
		setVisible(textAlign, false);
		setVisible(borderWidth, false);
		setVisible(borderColor, false);
		setVisible(borderStraight, false);
		setVisible(shadowOffsetX, false);
		setVisible(shadowOffsetY, false);
		setVisible(shadowColor, false);

		if (renderer.isVisible()) {
			if (ACTOR_RENDERERS[i].equals(Project.SPINE_RENDERER_STRING)) {
				setVisible(spineSkin, true);
			} else if (ACTOR_RENDERERS[i].equals(Project.S3D_RENDERER_STRING)) {
				setVisible(spriteSize, true);
				setVisible(cameraName, true);
				setVisible(fov, true);
			} else if (ACTOR_RENDERERS[i].equals(Project.PARTICLE_RENDERER_STRING)) {
				setVisible(particleName, true);
				setVisible(particleAtlas, true);
			} else if (ACTOR_RENDERERS[i].equals(Project.TEXT_RENDERER_STRING)) {
				setVisible(text, true);
				setVisible(font, true);
				setVisible(size, true);
				setVisible(textAlign, true);
				setVisible(borderWidth, true);
				setVisible(borderColor, true);
				setVisible(borderStraight, true);
				setVisible(shadowOffsetX, true);
				setVisible(shadowOffsetY, true);
				setVisible(shadowColor, true);
			}
		}
	}

	private void hideAllInputs() {

		for (int idx = 2; idx < i.length; idx++) {
			InputPanel ip = i[idx];

			setVisible(ip, false);
		}
	}

	@Override
	protected void inputsToModel(boolean create) {

		String type = typePanel.getText();
		boolean typeChanged = false;
		BaseActor oldElement = e;
		String oldId = null;

		boolean isPlayer = false;

		if (!create) {

			typeChanged = (type.equals(CHARACTER_TYPE_STR) && !(e instanceof CharacterActor))
					|| (type.equals(SPRITE_TYPE_STR) && (!(e instanceof SpriteActor) || e instanceof CharacterActor))
					|| (type.equals(BACKGROUND_TYPE_STR)
							&& (!(e instanceof InteractiveActor) || e instanceof SpriteActor))
					|| (type.equals(OBSTACLE_TYPE_STR) && !(e instanceof ObstacleActor))
					|| (type.equals(ANCHOR_TYPE_STR) && !(e instanceof AnchorActor))
					|| (type.equals(WALKZONE_TYPE_STR) && !(e instanceof WalkZoneActor));

			isPlayer = parent.getPlayer() == e;

			// remove to allow id, zindex and layer change
			parent.removeActor(e);

			oldId = e.getId();
		}

		if (create || typeChanged) {
			if (type.equals(BACKGROUND_TYPE_STR)) {
				e = new InteractiveActor();
			} else if (type.equals(SPRITE_TYPE_STR)) {
				e = new SpriteActor();
			} else if (type.equals(CHARACTER_TYPE_STR)) {
				e = new CharacterActor();
			} else if (type.equals(OBSTACLE_TYPE_STR)) {
				e = new ObstacleActor();
			} else if (type.equals(WALKZONE_TYPE_STR)) {
				e = new WalkZoneActor();
			} else if (type.equals(ANCHOR_TYPE_STR)) {
				e = new AnchorActor();
			}

			if (!(e instanceof SpriteActor) && !(e instanceof AnchorActor)) {
				Polygon bbox = e.getBBox();

				bbox.setVertices(new float[8]);

				float[] verts = bbox.getVertices();

				verts[0] = -DEFAULT_DIM / 2;
				verts[1] = 0f;
				verts[2] = -DEFAULT_DIM / 2;
				verts[3] = DEFAULT_DIM;
				verts[4] = DEFAULT_DIM / 2;
				verts[5] = DEFAULT_DIM;
				verts[6] = DEFAULT_DIM / 2;
				verts[7] = 0f;
				bbox.dirty();
			}
		}

		if (typeChanged) {
			// Put sounds, verbs and animations in the new element

			if (oldElement instanceof InteractiveActor && e instanceof InteractiveActor) {
				HashMap<String, Verb> verbs = ((InteractiveActor) e).getVerbManager().getVerbs();
				HashMap<String, Verb> oldVerbs = ((InteractiveActor) oldElement).getVerbManager().getVerbs();

				for (String k : oldVerbs.keySet()) {
					Verb v = oldVerbs.get(k);
					verbs.put(k, v);
				}
			}

			if (oldElement instanceof SpriteActor && e instanceof SpriteActor) {
				((SpriteActor) e).setRenderer(((SpriteActor) oldElement).getRenderer());
			}
		}

		e.setId(ElementUtils.getCheckedId(id.getText(), parent.getActors().keySet().toArray(new String[0])));
		e.setVisible(Boolean.parseBoolean(visible.getText()));

		Vector2 p = Param.parseVector2(pos.getText());

		e.setPosition(p.x, p.y);

		if (e instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) e;

			ia.setLayer(layer.getText());
			ia.setInteraction(Boolean.parseBoolean(interaction.getText()));

			Vector2 rp = Param.parseVector2(refPoint.getText());

			ia.setRefPoint(rp.x, rp.y);

			String key = ia.getDesc();

			if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX || !e.getId().equals(oldId))
				key = Ctx.project.getI18N().genKey(parent.getId(), e.getId(), "desc");

			Ctx.project.getI18N().setTranslation(key, desc.getText());

			if (desc.getText() != null)
				ia.setDesc(key);
			else
				ia.setDesc(null);

			ia.setState(state.getText());
			ia.setZIndex(Float.parseFloat(zIndex.getText()));

			if (e instanceof SpriteActor) {
				SpriteActor sa = (SpriteActor) e;

				String rendererType = renderer.getText();

				if (Project.ATLAS_RENDERER_STRING.equals(rendererType)) {
					if (sa.getRenderer() == null || !(sa.getRenderer() instanceof AtlasRenderer))
						sa.setRenderer(new AtlasRenderer());
				} else if (Project.IMAGE_RENDERER_STRING.equals(rendererType)) {
					if (sa.getRenderer() == null || !(sa.getRenderer() instanceof ImageRenderer))
						sa.setRenderer(new ImageRenderer());
				} else if (Project.S3D_RENDERER_STRING.equals(rendererType)) {
					Sprite3DRenderer r;

					if (sa.getRenderer() == null || !(sa.getRenderer() instanceof Sprite3DRenderer)) {
						r = new Sprite3DRenderer();
						sa.setRenderer(r);
					} else {
						r = (Sprite3DRenderer) sa.getRenderer();
					}

					r.setCameraFOV(Float.parseFloat(fov.getText()));
					r.setCameraName(cameraName.getText());
					r.setSpriteSize(Param.parseVector2(spriteSize.getText()));
				} else if (Project.PARTICLE_RENDERER_STRING.equals(rendererType)) {
					ParticleRenderer r;

					if (sa.getRenderer() == null || !(sa.getRenderer() instanceof ParticleRenderer)) {
						r = new ParticleRenderer();
						sa.setRenderer(r);
					} else {
						r = (ParticleRenderer) sa.getRenderer();
					}

					r.setParticleName(particleName.getText());
					r.setAtlasName(particleAtlas.getText());

				} else if (Project.TEXT_RENDERER_STRING.equals(rendererType)) {
					TextRenderer r;

					if (sa.getRenderer() == null || !(sa.getRenderer() instanceof TextRenderer)) {
						r = new TextRenderer();
						sa.setRenderer(r);
					} else {
						r = (TextRenderer) sa.getRenderer();
					}

					key = text.getText();

					if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX || !e.getId().equals(oldId))
						key = Ctx.project.getI18N().genKey(parent.getId(), e.getId(), "text");

					Ctx.project.getI18N().setTranslation(key, text.getText());

					if (text.getText() != null)
						r.setText(key, text.getText());
					else
						r.setText(null, null);

					r.setFontSize(Integer.parseInt(size.getText()));
					r.setFontName(font.getText());
					r.setAlign(AlignUtils.getAlign(textAlign.getText()));
					r.setBorderWidth(Integer.parseInt(borderWidth.getText()));
					r.setBorderColor(Param.parseColor(borderColor.getText()));
					r.setBorderStraight(Boolean.parseBoolean(borderStraight.getText()));
					r.setShadowOffsetX(Integer.parseInt(shadowOffsetX.getText()));
					r.setShadowOffsetY(Integer.parseInt(shadowOffsetY.getText()));
					r.setShadowColor(Param.parseColor(shadowColor.getText()));

					// dispose to force reload the text attributes
					sa.dispose();
				} else if (Project.SPINE_RENDERER_STRING.equals(rendererType)) {
					SpineRenderer r;

					if (sa.getRenderer() == null || !(sa.getRenderer() instanceof SpineRenderer)) {
						r = new SpineRenderer();
						sa.setRenderer(r);
					} else {
						r = (SpineRenderer) sa.getRenderer();
					}

					try {
						r.setSkin(spineSkin.getText());
					} catch (Exception e) {
						r.setSkin(null);
						Message.showMsgDialog(getStage(), "Error setting skin.", e.getMessage());
					}
				}

				sa.getRenderer().setWorld(parent.getWorld());
				boolean bbfr = Boolean.parseBoolean(bboxFromRenderer.getText());

				if (bbfr != sa.isBboxFromRenderer())
					sa.setBboxFromRenderer(bbfr);

				// Bbox always has to be valid
				if (sa.getBBox().getVertices().length < 4) {
					sa.getRenderer().updateBboxFromRenderer(sa.getBBox());

					if (!sa.isBboxFromRenderer())
						sa.getRenderer().updateBboxFromRenderer(null);
				}

				sa.setFakeDepth(Boolean.parseBoolean(fakeDepth.getText()));

				Vector2 ps = Param.parseVector2(scale.getText());
				sa.setScale(ps.x, ps.y);
				sa.setRot(Float.parseFloat(rot.getText()));
				sa.getRenderer().setOrgAlign(AlignUtils.getAlign(orgAlign.getText()));
				sa.setTint(Param.parseColor(tint.getText()));

				if (e instanceof CharacterActor) {
					CharacterActor ca = (CharacterActor) e;

					ca.setWalkingSpeed(Float.parseFloat(walkingSpeed.getText()));
					ca.setTextColor(Param.parseColor(textColor.getText()));
					ca.setTextStyle(textStyle.getText());
					ca.setTalkingTextPos(Param.parseVector2(talkingTextPos.getText()));
				}
			}
		}

		parent.addActor(e);

		if (isPlayer && !typeChanged)
			parent.setPlayer((CharacterActor) e);

		if (e instanceof InteractiveActor) {
			SceneLayer l = parent.getLayer(((InteractiveActor) e).getLayer());
			l.orderByZIndex();
		}

		if (e instanceof SpriteActor)
			((SpriteActor) e).retrieveAssets();

		if (create && e instanceof WalkZoneActor && parent.getWalkZone() == null)
			parent.setWalkZone(e.getId());

		// TODO UNDO OP
		// UndoOp undoOp = new UndoAddElement(doc, e);
		// Ctx.project.getUndoStack().add(undoOp);

		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {

		id.setText(e.getId());
		visible.setText(Boolean.toString(e.isVisible()));
		pos.setText(Param.toStringParam(new Vector2(e.getX(), e.getY())));

		if (e instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) e;
			layer.setText(ia.getLayer());
			interaction.setText(Boolean.toString(ia.getInteraction()));
			refPoint.setText(Param.toStringParam(ia.getRefPoint()));
			desc.setText(Ctx.project.translate(ia.getDesc()));
			state.setText(ia.getState());
			zIndex.setText(Float.toString(ia.getZIndex()));

			if (e instanceof SpriteActor) {
				SpriteActor sa = (SpriteActor) e;

				ActorRenderer r = sa.getRenderer();

				if (r instanceof AtlasRenderer) {
					renderer.setText(Project.ATLAS_RENDERER_STRING);
				} else if (r instanceof ImageRenderer) {
					renderer.setText(Project.IMAGE_RENDERER_STRING);
				} else if (r instanceof Sprite3DRenderer) {
					renderer.setText(Project.S3D_RENDERER_STRING);
					Sprite3DRenderer s3d = (Sprite3DRenderer) r;

					fov.setText(Float.toString(s3d.getCameraFOV()));
					cameraName.setText(s3d.getCameraName());
					spriteSize.setText(Param.toStringParam(s3d.getSpriteSize()));
				} else if (r instanceof ParticleRenderer) {
					renderer.setText(Project.PARTICLE_RENDERER_STRING);
					ParticleRenderer pr = (ParticleRenderer) r;

					particleName.setText(pr.getParticleName());
					particleAtlas.setText(pr.getAtlasName());
				} else if (r instanceof TextRenderer) {
					renderer.setText(Project.TEXT_RENDERER_STRING);
					TextRenderer tr = (TextRenderer) r;

					text.setText(Ctx.project.translate(tr.getText()));
					size.setText(Integer.toString(tr.getFontSize()));
					font.setText(tr.getFontName());
					borderWidth.setText(Integer.toString(tr.getBorderWidth()));
					textAlign.setText(AlignUtils.getAlign(tr.getAlign()));
					borderColor.setText(tr.getBorderColor().toString());
					borderStraight.setText(Boolean.toString(tr.isBorderStraight()));
					shadowOffsetX.setText(Integer.toString(tr.getShadowOffsetX()));
					shadowOffsetY.setText(Integer.toString(tr.getShadowOffsetY()));
					shadowColor.setText(tr.getShadowColor().toString());

				} else if (r instanceof SpineRenderer) {
					renderer.setText(Project.SPINE_RENDERER_STRING);
					spineSkin.setText(((SpineRenderer) r).getSkin());
				}

				fakeDepth.setText(Boolean.toString(sa.getFakeDepth()));
				scale.setText(Param.toStringParam(new Vector2(sa.getScaleX(), sa.getScaleY())));
				rot.setText(Float.toString(sa.getRot()));
				tint.setText(sa.getTint() == null ? null : sa.getTint().toString());
				bboxFromRenderer.setText(Boolean.toString(sa.isBboxFromRenderer()));
				orgAlign.setText(AlignUtils.getAlign(sa.getRenderer().getOrgAlign()));

				if (e instanceof CharacterActor) {
					CharacterActor ca = (CharacterActor) e;

					walkingSpeed.setText(Float.toString(ca.getWalkingSpeed()));
					textColor.setText(ca.getTextColor() == null ? null : ca.getTextColor().toString());
					textStyle.setText(ca.getTextStyle());
					talkingTextPos.setText(Param.toStringParam(ca.getTalkingTextPos()));
					typePanel.setText(CHARACTER_TYPE_STR);
				} else {
					typePanel.setText(SPRITE_TYPE_STR);
				}
			} else {
				typePanel.setText(BACKGROUND_TYPE_STR);
			}
		} else if (e instanceof AnchorActor) {
			typePanel.setText(ANCHOR_TYPE_STR);
		} else if (e instanceof WalkZoneActor) {
			typePanel.setText(WALKZONE_TYPE_STR);
		} else if (e instanceof ObstacleActor) {
			typePanel.setText(OBSTACLE_TYPE_STR);
		}

	}
}

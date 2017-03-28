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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.MusicDesc;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;
import com.bladecoder.engineeditor.undo.UndoCreateScene;
import com.bladecoder.engineeditor.undo.UndoEditScene;

public class EditSceneDialog extends EditModelDialog<World, Scene> {

	public static final String INFO = "An adventure is composed of many scenes (rooms/screens).\n"
			+ "Inside a scene, there are actors and a 'player'.\nThe player/user can interact with actors throught 'verbs'.";

	private Image bgImage;
	private Container<Image> infoContainer;
	private TextureAtlas atlas;

	private InputPanel id;
	private InputPanel backgroundAtlas;
	private InputPanel backgroundRegion;
	private InputPanel depthVector;
	private InputPanel state;
	private InputPanel music;
	private InputPanel loopMusic;
	private InputPanel volumeMusic;
	private InputPanel initialMusicDelay;
	private InputPanel repeatMusicDelay;
	private InputPanel stopWhenLeaving;
	private InputPanel sceneSize;

	@SuppressWarnings("unchecked")
	public EditSceneDialog(Skin skin, World parent, Scene e) {

		super(skin);

		id = InputPanelFactory.createInputPanel(skin, "Scene ID",
				"The ID is mandatory for scenes.", true);
		backgroundAtlas = InputPanelFactory.createInputPanel(skin, "Background Atlas",
				"The atlas where the background for the scene is located", Type.ATLAS_ASSET, false);
		backgroundRegion = InputPanelFactory.createInputPanel(skin, "Background Region Id",
				"The region id for the background.", new String[0], false);
//		depthVector = InputPanelFactory.createInputPanel(skin, "Depth Vector",
//				"X: the actor 'y' position for a 0.0 scale, Y: the actor 'y' position for a 1.0 scale.",
//				Param.Type.VECTOR2, false);
		
		depthVector = InputPanelFactory.createInputPanel(skin, "Fake depth", "Change actor scale based in the 'y' axis position.", Param.Type.BOOLEAN, true,
				"false");
		
		state = InputPanelFactory.createInputPanel(skin, "State", "The initial state for the scene.", false);
		music = InputPanelFactory.createInputPanel(skin, "Music Filename", "The music for the scene", Type.MUSIC_ASSET, false);
		loopMusic = InputPanelFactory.createInputPanel(skin, "Loop Music", "If the music is playing in looping",
				Param.Type.BOOLEAN, true, "true");
		volumeMusic = InputPanelFactory.createInputPanel(skin, "Music Volume", "The volume of the music. Value is between 0 and 1.",
				Param.Type.FLOAT, true, "1");
		initialMusicDelay = InputPanelFactory.createInputPanel(skin, "Initial music delay",
				"The time to wait before playing", Param.Type.FLOAT, true, "0");
		repeatMusicDelay = InputPanelFactory.createInputPanel(skin, "Repeat music delay",
				"The time to wait before repetitions", Param.Type.FLOAT, true, "-1");
		stopWhenLeaving = InputPanelFactory.createInputPanel(skin, "Stop music when leaving",
				"Stops the music when leaving the current scene.", Param.Type.BOOLEAN, true, "true");

		sceneSize = InputPanelFactory.createInputPanel(skin, "Scene Dimension",
				"Sets the size of the scene. If empty, the background image size is used as the scene dimension.",
				Param.Type.DIMENSION, false);

		bgImage = new Image();
		bgImage.setScaling(Scaling.fit);
		infoContainer = new Container<Image>(bgImage);
		setInfo(INFO);

		((SelectBox<String>) backgroundAtlas.getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					fillBGRegions(backgroundAtlas, backgroundRegion);
				} catch (Exception e) {
					Message.showMsg(getStage(), "Error loading regions from selected atlas", 4);
				}
			}
		});

		((SelectBox<String>) backgroundRegion.getField()).addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				showBgImage(backgroundRegion.getText());
			}
		});

		try {
			fillBGRegions(backgroundAtlas, backgroundRegion);
		} catch (Exception e2) {
			EditorLogger.error("Error loading regions from selected atlas");
		}

		init(parent, e, new InputPanel[] { id, backgroundAtlas, backgroundRegion, depthVector, state, sceneSize, music,
				loopMusic, volumeMusic, initialMusicDelay, repeatMusicDelay, stopWhenLeaving });
	}

	private void showBgImage(String r) {
		if (atlas == null || r == null)
			return;

		bgImage.setDrawable(new TextureRegionDrawable(atlas.findRegion(r)));

		infoContainer.prefWidth(250);
		infoContainer.prefHeight(250);
		setInfoWidget(infoContainer);
	}

	private void fillBGRegions(InputPanel atlasInput, InputPanel regionInput) {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) regionInput.getField();

		// cb.clearItems();
		cb.getItems().clear();

		if (atlas != null) {
			atlas.dispose();
			atlas = null;
		}

		if (backgroundAtlas.getText().isEmpty()) {
			setInfoWidget(new Label(INFO, getSkin()));
			return;
		}

		atlas = new TextureAtlas(Gdx.files.absolute(Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir() + "/" + atlasInput.getText() + ".atlas"));

		Array<AtlasRegion> regions = atlas.getRegions();

		for (AtlasRegion r : regions)
			if (cb.getItems().indexOf(r.name, false) == -1)
				cb.getItems().add(r.name);

		cb.getList().setItems(cb.getItems());
		if (cb.getItems().size > 0)
			cb.setSelectedIndex(0);

		cb.invalidateHierarchy();

		showBgImage(regionInput.getText());
	}

	@Override
	protected void inputsToModel(boolean create) {

		if (create) {
			e = new Scene();

			// CREATE DEFAULT LAYERS: BG, DYNAMIC, FG
			SceneLayer l = new SceneLayer();
			l.setName("foreground");
			l.setVisible(true);
			l.setDynamic(false);
			e.addLayer(l);

			l = new SceneLayer();
			l.setName("dynamic");
			l.setVisible(true);
			l.setDynamic(true);
			e.addLayer(l);

			l = new SceneLayer();
			l.setName("background");
			l.setVisible(true);
			l.setDynamic(false);
			e.addLayer(l);
		} else {
			parent.getScenes().remove(e.getId());
		}

		e.setId(ElementUtils.getCheckedId(id.getText(), World.getInstance().getScenes().keySet().toArray(new String[0])));

		e.setBackgroundAtlas(backgroundAtlas.getText());
		e.setBackgroundRegionId(backgroundRegion.getText());
		
		boolean dv = Boolean.parseBoolean(depthVector.getText());
		
		if(dv == true && e.getDepthVector() == null) { // create depth vector
			e.setDepthVector(new Vector2(World.getInstance().getHeight(), 0));
		} else if(dv == false && e.getDepthVector() != null) { // Remove depth vector
			e.setDepthVector(null);
		}
		
		e.setState(state.getText());

		MusicDesc md = null;

		if (music.getText() != null) {
			md = new MusicDesc();

			md.setFilename(music.getText());
			md.setLoop(Boolean.parseBoolean(loopMusic.getText()));
			md.setVolume(Float.parseFloat(volumeMusic.getText()));
			md.setInitialDelay(Float.parseFloat(initialMusicDelay.getText()));
			md.setRepeatDelay(Float.parseFloat(repeatMusicDelay.getText()));
			md.setStopWhenLeaving(Boolean.parseBoolean(stopWhenLeaving.getText()));
		}

		e.setMusicDesc(md);

		e.setSceneSize(Param.parseVector2(sceneSize.getText()));

		parent.addScene(e);

		if (parent.getScenes().size() == 1)
			parent.setInitScene(e.getId());

		// UNDO OP
		if (create) {
			Ctx.project.getUndoStack().add(new UndoCreateScene(e));
		} else {
			Ctx.project.getUndoStack().add(new UndoEditScene(e));
		}

		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, e);
	}

	@Override
	protected void modelToInputs() {

		id.setText(e.getId());
		backgroundAtlas.setText(e.getBackgroundAtlas());
		backgroundRegion.setText(e.getBackgroundRegionId());
		
		if (e.getDepthVector() != null)
			depthVector.setText("true");
		else
			depthVector.setText("false");
		
		state.setText(e.getState());

		MusicDesc md = e.getMusicDesc();

		if (md != null) {
			music.setText(md.getFilename());
			loopMusic.setText(Boolean.toString(md.isLoop()));
			volumeMusic.setText(Float.toString(md.getVolume()));
			initialMusicDelay.setText(Float.toString(md.getInitialDelay()));
			repeatMusicDelay.setText(Float.toString(md.getRepeatDelay()));
			stopWhenLeaving.setText(Boolean.toString(md.isStopWhenLeaving()));
		}

		if (e.getSceneSize() != null)
			sceneSize.setText(Param.toStringParam(e.getSceneSize()));
	}

	@Override
	protected void result(Object object) {
		if (atlas != null) {
			atlas.dispose();
		}

		super.result(object);
	}
}

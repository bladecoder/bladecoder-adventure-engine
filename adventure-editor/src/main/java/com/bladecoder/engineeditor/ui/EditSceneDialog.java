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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SceneLayer;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;
import com.bladecoder.engineeditor.undo.UndoCreateScene;
import com.bladecoder.engineeditor.undo.UndoEditScene;
import com.bladecoder.engineeditor.utils.EditorLogger;
import com.bladecoder.engineeditor.utils.ElementUtils;
import com.bladecoder.engineeditor.utils.Message;

public class EditSceneDialog extends EditModelDialog<World, Scene> {

	public static final String INFO = "An adventure is composed of many scenes (screens).\n"
			+ "Inside a scene there are actors and a 'player'.\nThe player/user can interact with the actors throught 'verbs'.";

	private String atlasList[] = getAtlasList();
	private String musicList[] = getMusicList();

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
	private InputPanel initialMusicDelay;
	private InputPanel repeatMusicDelay;
	private InputPanel sceneSize;

	@SuppressWarnings("unchecked")
	public EditSceneDialog(Skin skin, World parent, Scene e) {

		super(skin);

		id = InputPanelFactory.createInputPanel(skin, "Scene ID",
				"The ID is mandatory for scenes. \nIDs can not contain '.' or '_' characters.", true);
		backgroundAtlas = InputPanelFactory.createInputPanel(skin, "Background Atlas",
				"The atlas where the background for the scene is located", atlasList, false);
		backgroundRegion = InputPanelFactory.createInputPanel(skin, "Background Region Id",
				"The region id for the background.", new String[0], false);
		depthVector = InputPanelFactory.createInputPanel(skin, "Depth Vector",
				"X: the actor 'y' position for a 0.0 scale, Y: the actor 'y' position for a 1.0 scale.",
				Param.Type.VECTOR2, false);
		state = InputPanelFactory.createInputPanel(skin, "State", "The initial state for the scene.", false);
		music = InputPanelFactory.createInputPanel(skin, "Music Filename", "The music for the scene", musicList, false);
		loopMusic = InputPanelFactory.createInputPanel(skin, "Loop Music", "If the music is playing in looping",
				Param.Type.BOOLEAN, false);
		initialMusicDelay = InputPanelFactory.createInputPanel(skin, "Initial music delay",
				"The time to wait before playing", Param.Type.FLOAT, true, "0");
		repeatMusicDelay = InputPanelFactory.createInputPanel(skin, "Repeat music delay",
				"The time to wait before repetitions", Param.Type.FLOAT, true, "0");
		
		sceneSize = InputPanelFactory.createInputPanel(skin, "Scene Dimension",
				"Sets the size of the scene. If empty, the background image size is used as the scene dimension.", Param.Type.DIMENSION, false);

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

		init(parent, e, new InputPanel[] { id, backgroundAtlas, backgroundRegion,
				depthVector, state, sceneSize, music, loopMusic, initialMusicDelay, repeatMusicDelay });
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
		e.setDepthVector(Param.parseVector2(depthVector.getText()));
		e.setState(state.getText());

		e.setMusic(music.getText(), Boolean.parseBoolean(loopMusic.getText()),
				Float.parseFloat(initialMusicDelay.getText()), Float.parseFloat(repeatMusicDelay.getText()));
		
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
			depthVector.setText(Param.toStringParam(e.getDepthVector()));
		state.setText(e.getState());
		music.setText(e.getMusicFilename());
		loopMusic.setText(Boolean.toString(e.isLoopMusic()));
		initialMusicDelay.setText(Float.toString(e.getInitialMusicDelay()));
		repeatMusicDelay.setText(Float.toString(e.getRepeatMusicDelay()));
		
		if (e.getSceneSize() != null)
			sceneSize.setText(Param.toStringParam(e.getSceneSize()));
	}

	private String[] getAtlasList() {
		String bgPath = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/" + Ctx.project.getResDir();

		File f = new File(bgPath);

		String bgs[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".atlas"))
					return true;

				return false;
			}
		});

		Arrays.sort(bgs);

		for (int i = 0; i < bgs.length; i++) {
			int idx = bgs[i].lastIndexOf('.');
			if (idx != -1)
				bgs[i] = bgs[i].substring(0, idx);
		}

		return bgs;
	}

	private String[] getMusicList() {
		String path = Ctx.project.getProjectPath() + Project.MUSIC_PATH;

		File f = new File(path);

		String musicFiles[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if (arg1.endsWith(".ogg") || arg1.endsWith(".mp3"))
					return true;

				return false;
			}
		});

		if (musicFiles == null)
			return new String[0];

		Arrays.sort(musicFiles);

		String musicFiles2[] = new String[musicFiles.length + 1];
		musicFiles2[0] = "";

		for (int i = 0; i < musicFiles.length; i++)
			musicFiles2[i + 1] = musicFiles[i];

		return musicFiles2;
	}

	@Override
	protected void result(Object object) {
		if (atlas != null) {
			atlas.dispose();
		}

		super.result(object);
	}
}

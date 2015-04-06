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

import org.w3c.dom.Element;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.BaseDocument;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;

public class EditSceneDialog extends EditElementDialog {

	public static final String INFO = "An adventure is composed of many scenes (screens).\n" +
			"Inside a scene there are actors and a 'player'.\nThe player/user can interact with the actors throught 'verbs'.";
	
	private String atlasList[] = getAtlasList();
	private String musicList[] = getMusicList();
	
	private InputPanel[] inputs = new InputPanel[11];
	
	private Image bgImage;
	private TextureAtlas atlas;
	
	String attrs[] = {"id", "background_atlas", "background_region", "lightmap_atlas", "lightmap_region", "depth_vector", "state", "music", "loop_music", "initial_music_delay", "repeat_music_delay"};

	@SuppressWarnings("unchecked")
	public EditSceneDialog(Skin skin, BaseDocument doc, Element parent,
				Element e) {
		
		super(skin);
		
		inputs[0] = InputPanelFactory.createInputPanel(skin, "Scene ID",
				"The ID is mandatory for scenes. \nIDs can not contain '.' or '_' characters.");
		inputs[1] = InputPanelFactory.createInputPanel(skin, "Background Atlas",
				"The atlas where the background for the scene is located", atlasList, false);
		inputs[2] = InputPanelFactory.createInputPanel(skin, "Background Region Id",
				"The region id for the background.", new String[0], false);
		inputs[3] = InputPanelFactory.createInputPanel(skin, "Lightmap Atlas",
						"The atlas where the lightmap for the scene is located", atlasList, false);	
		inputs[4] = InputPanelFactory.createInputPanel(skin, "Lightmap Region Id",
				"The region id for the lightmap", new String[0], false);
		inputs[5] = InputPanelFactory.createInputPanel(skin, "Depth Vector",
						"X: the actor 'y' position for a 0.0 scale, Y: the actor 'y' position for a 1.0 scale.", Param.Type.VECTOR2, false);
		inputs[6] = InputPanelFactory.createInputPanel(skin, "State",
				"The initial state for the scene.", false);
		inputs[7] = InputPanelFactory.createInputPanel(skin, "Music Filename",
				"The music for the scene", musicList, false);
		inputs[8] = InputPanelFactory.createInputPanel(skin, "Loop Music",
				"If the music is playing in looping", Param.Type.BOOLEAN, false);
		inputs[9] = InputPanelFactory.createInputPanel(skin, "Initial music delay",
				"The time to wait before playing", Param.Type.FLOAT, false);
		inputs[10] = InputPanelFactory.createInputPanel(skin, "Repeat music delay",
				"The time to wait before repetitions", Param.Type.FLOAT, false);		
		
		bgImage = new Image();
		bgImage.setScaling(Scaling.fit);
		setInfo(INFO);
		
		inputs[0].setMandatory(true);

		init(inputs, attrs, doc, parent, "scene", e);
		
		((SelectBox<String>) inputs[1].getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					fillBGRegions(inputs[1], inputs[2]);
				} catch(Exception e) {
					Ctx.msg.show(getStage(), "Error loading regions from selected atlas", 4);
				}
			}
		});
		

		((SelectBox<String>) inputs[2].getField())
			.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				showBgImage(inputs[2].getText());
			}
		});
		
		((SelectBox<String>) inputs[3].getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				try {
					fillLightmapRegions(inputs[3], inputs[4]);
				} catch(Exception e) {
					Ctx.msg.show(getStage(), "Error loading regions from selected atlas", 4);
				}
			}
		});		
		
//		try {
//			fillAnimations();
//		} catch(Exception e2) {
//			EditorLogger.error("Error loading regions from selected atlas");
//		}
	}
	
	

	private void showBgImage(String r) {
		if(atlas == null)
			return;

		bgImage.setDrawable(new TextureRegionDrawable(atlas.findRegion(r)));
		
		setInfoWidget(bgImage);
	}

	private void fillBGRegions(InputPanel atlasInput, InputPanel regionInput) {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) regionInput.getField();
		
//		cb.clearItems();
		cb.getItems().clear();
		
		if(atlas != null) {
			atlas.dispose();
			atlas = null;
		}
		
		if(inputs[1].getText().isEmpty()) {
			setInfoWidget(new Label(INFO, getSkin()));
			return;
		}
		
		atlas = new TextureAtlas(Gdx.files.absolute(Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir() + "/" + atlasInput.getText() + ".atlas"));

		Array<AtlasRegion> regions = atlas.getRegions();
		
		for (AtlasRegion r : regions)
			cb.getItems().add(r.name);

		cb.getList().setItems(cb.getItems());
		if (cb.getItems().size > 0)
			cb.setSelectedIndex(0);

		cb.invalidateHierarchy();

		showBgImage(regionInput.getText());
	}
	
	private void fillLightmapRegions(InputPanel atlasInput, InputPanel regionInput) {
		@SuppressWarnings("unchecked")
		SelectBox<String> cb = (SelectBox<String>) regionInput.getField();
		
//		cb.clearItems();
		cb.getItems().clear();
		
		TextureAtlas atlas = new TextureAtlas(Gdx.files.absolute(Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir() + "/" + atlasInput.getText() + ".atlas"));

		Array<AtlasRegion> regions = atlas.getRegions();
		
		for (AtlasRegion r : regions)
			cb.getItems().add(r.name);

		cb.getList().setItems(cb.getItems());
		if (cb.getItems().size > 0)
			cb.setSelectedIndex(0);

		cb.invalidateHierarchy();

		atlas.dispose();
	}		
	
	@Override
	protected void create() {
		super.create();
		
		// CREATE DEFAULT LAYERS: BG, DYNAMIC, FG
		Element layer = doc.createElement(getElement(), "layer");
		layer.setAttribute("id", "foreground");
		layer.setAttribute("visible", "true");
		layer.setAttribute("dynamic", "false");
		getElement().appendChild(layer);
		
		layer = doc.createElement(getElement(), "layer");
		layer.setAttribute("id", "dynamic");
		layer.setAttribute("visible", "true");
		layer.setAttribute("dynamic", "true");
		getElement().appendChild(layer);
		
		layer = doc.createElement(getElement(), "layer");
		layer.setAttribute("id", "background");
		layer.setAttribute("visible", "true");
		layer.setAttribute("dynamic", "false");
		getElement().appendChild(layer);
	}

	private String[] getAtlasList() {
		String bgPath = Ctx.project.getProjectPath() + Project.ATLASES_PATH + "/"
				+ Ctx.project.getResDir();

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

		for(int i = 0; i < bgs.length; i++) {
			int idx = bgs[i].lastIndexOf('.');
			if(idx != -1)
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

		Arrays.sort(musicFiles);
		
		String musicFiles2[] = new String[musicFiles.length + 1];
		musicFiles2[0] = "";
		
		for(int i=0; i < musicFiles.length; i++)
			musicFiles2[i + 1] = musicFiles[i];

		return musicFiles2;
	}
	
	@Override
	protected void result(Object object) {
		if(atlas != null)
			atlas.dispose();
		
		super.result(object);
	}
}

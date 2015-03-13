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
import java.util.ArrayList;
import java.util.Arrays;

import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
	
	private String bgList[] = getBgList();
	private String musicList[] = getMusicList();
	
	private InputPanel[] inputs = new InputPanel[9];
	
	private Image bgImage;
					
	
	String attrs[] = {"id", "background", "lightmap", "depth_vector", "state", "music", "loop_music", "initial_music_delay", "repeat_music_delay"};

	@SuppressWarnings("unchecked")
	public EditSceneDialog(Skin skin, BaseDocument doc, Element parent,
				Element e) {
		
		super(skin);
		
		inputs[0] = InputPanelFactory.createInputPanel(skin, "Scene ID",
				"The ID is mandatory for scenes. \nIDs can not contain '.' or '_' characters.");
		inputs[1] = InputPanelFactory.createInputPanel(skin, "Background",
				"The background for the scene", bgList, true);
		inputs[2] = InputPanelFactory.createInputPanel(skin, "Lightmap",
						"The lightmap for the scene", bgList, true);					
		inputs[3] = InputPanelFactory.createInputPanel(skin, "Depth Vector",
						"X: the actor 'y' position for a 0.0 scale, Y: the actor 'y' position for a 1.0 scale.", Param.Type.VECTOR2, false);
		inputs[4] = InputPanelFactory.createInputPanel(skin, "State",
				"The initial state for the scene.", true);
		inputs[5] = InputPanelFactory.createInputPanel(skin, "Music Filename",
				"The music for the scene", musicList, true);
		inputs[6] = InputPanelFactory.createInputPanel(skin, "Loop Music",
				"If the music is playing in looping", Param.Type.BOOLEAN, false);
		inputs[7] = InputPanelFactory.createInputPanel(skin, "Initial music delay",
				"The time to wait before playing", Param.Type.FLOAT, false);
		inputs[8] = InputPanelFactory.createInputPanel(skin, "Repeat music delay",
				"The time to wait before repetitions", Param.Type.FLOAT, false);		
		
		bgImage = new Image();
		bgImage.setScaling(Scaling.fit);
		setInfo(INFO);
		
		inputs[0].setMandatory(true);

		init(inputs, attrs, doc, parent, "scene", e);
		
		((SelectBox<String>) inputs[1].getField()).addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String bg = inputs[1].getText();

				if(!bg.isEmpty())
					bgImage.setDrawable(new TextureRegionDrawable(Ctx.project.getBgIcon(bg)));
				
				setInfoWidget(bgImage);
			}
		});		
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

	private String[] getBgList() {
		String bgPath = Ctx.project.getProjectPath() + Project.BACKGROUNDS_PATH + "/"
				+ Ctx.project.getResDir();

		File f = new File(bgPath);

		String bgs[] = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				if ((arg1.matches("_[1-9]\\.")))
					return false;

				return true;
			}
		});

		Arrays.sort(bgs);
		
		ArrayList<String> l = new ArrayList<String>(Arrays.asList(bgs));
		l.add(0,"");

		return l.toArray(new String[bgs.length + 1]);
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
}

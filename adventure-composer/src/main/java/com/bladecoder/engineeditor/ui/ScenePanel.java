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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.ChapterDocument;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.HeaderPanel;
import com.bladecoder.engineeditor.ui.components.TabPanel;

public class ScenePanel extends HeaderPanel {
	
	private TabPanel tabPanel;
//	private VerbList verbList;	
	private ActorList actorList;
	private LayerList layerList;
	private SceneProps sceneProps;
	
	public ScenePanel(Skin skin) {
		super(skin, "SCENE");		
		tabPanel = new TabPanel(skin);
//		verbList = new VerbList(skin);
		actorList = new ActorList(skin);
		layerList = new LayerList(skin);
//		sceneProps = new SceneProps(skin);
				
		setContent(tabPanel);		
		
		tabPanel.addTab("Actors", actorList);
//		tabPanel.addTab("Verbs", verbList);
		tabPanel.addTab("Layers", layerList);
//		tabPanel.addTab("Scene Props", sceneProps);
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_SCENE_SELECTED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				ChapterDocument doc = Ctx.project.getSelectedChapter();
				Scene s = Ctx.project.getSelectedScene();
				
				if(s != null) {								
					setTile("SCENE " + s.getId());
				} else {
					setTile("SCENE");
				}
				
				
				actorList.addElements(Arrays.asList(s.getActors().values().toArray(new BaseActor[0])));
//				verbList.addElements(doc, scn, "verb");	
				layerList.addElements(s.getLayers());		
//				sceneProps.setSceneDocument(doc, scn);
				
			}
		});	
	}
}

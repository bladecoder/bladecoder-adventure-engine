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
package org.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.components.HeaderPanel;
import org.bladecoder.engineeditor.ui.components.TabPanel;
import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ScenePanel extends HeaderPanel {
	
	private TabPanel tabPanel;
	private VerbList verbList;	
	private ActorList actorList;
	private SceneProps sceneProps;
	
	public ScenePanel(Skin skin) {
		super(skin, "SCENE");		
		tabPanel = new TabPanel(skin);
		verbList = new VerbList(skin);
		actorList = new ActorList(skin);
		sceneProps = new SceneProps(skin);
				
		setContent(tabPanel);		
		
		tabPanel.addTab("Actors", actorList);
		tabPanel.addTab("Verbs", verbList);
		tabPanel.addTab("Properties", sceneProps);
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_SCENE_SELECTED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				ChapterDocument doc = Ctx.project.getSelectedChapter();
				Element scn = Ctx.project.getSelectedScene();
				
				if(scn != null) {								
					setTile("SCENE " + doc.getId(scn));
				} else {
					setTile("SCENE");
				}
				
				actorList.addElements(doc, scn, "actor");
				verbList.addElements(doc, scn, "verb");		
				sceneProps.setSceneDocument(doc, scn);
				
			}
		});	
	}
}

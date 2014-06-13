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
import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.model.Project;
import org.bladecoder.engineeditor.model.WorldDocument;
import org.bladecoder.engineeditor.ui.components.HeaderPanel;
import org.bladecoder.engineeditor.ui.components.TabPanel;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ProjectPanel extends HeaderPanel  {

	private TabPanel tabPanel;
	private SceneList sceneList;
	private VerbList verbList;
	private ChapterList chapterList;
	
	
	public ProjectPanel(Skin skin) {
		super(skin, "ADVENTURE");	
		
		tabPanel = new TabPanel(skin);
		sceneList = new SceneList(skin);
		verbList = new VerbList(skin);
		chapterList = new ChapterList(skin);
	
		setContent(tabPanel);
		
		tabPanel.addTab("Scenes", sceneList);
		tabPanel.addTab("Chapters", chapterList);
		tabPanel.addTab("Def. Verbs", verbList);
		tabPanel.addTab("Properties", new WorldProps(skin));


		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				WorldDocument w = Ctx.project.getWorld();
				ChapterDocument selectedChapter = Ctx.project.getSelectedChapter();
				
				sceneList.addElements(selectedChapter, selectedChapter.getElement(), "scene");
				verbList.addElements(w, w.getElement(), "verb");
				chapterList.addElements(w);
				setTile("ADVENTURE - " + (Ctx.project.getTitle() != null? Ctx.project.getTitle():""));
			}
		});		
	}
}

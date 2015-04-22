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

import org.w3c.dom.Element;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.ChapterDocument;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.HeaderPanel;
import com.bladecoder.engineeditor.ui.components.TabPanel;

public class ActorPanel extends HeaderPanel {

	private TabPanel tabPanel;
	private VerbList verbList;
	private DialogList dialogList;
	private SpriteList faList;
	private SoundList soundList;
	private ActorProps props;

	public ActorPanel(Skin skin) {
		super(skin, "ACTOR");
		tabPanel = new TabPanel(skin);
		verbList = new VerbList(skin);
		dialogList = new DialogList(skin);
		faList = new SpriteList(skin);
		props = new ActorProps(skin);
		soundList = new SoundList(skin);
		
		setContent(tabPanel);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ACTOR_SELECTED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						Element a = (Element) e.getNewValue();
						ChapterDocument doc = Ctx.project.getSelectedChapter();

						String selTitle = tabPanel.getSelectedIndex() == -1? null: tabPanel.getTitleAt(tabPanel.getSelectedIndex());
						tabPanel.clear();

						if (a != null) {

							String type = doc.getType(a);

							if (!type.equals(XMLConstants.NO_RENDERER_VALUE))
								tabPanel.addTab("Animations", faList);

							tabPanel.addTab("Verbs", verbList);
							tabPanel.addTab("Sounds", soundList);

							tabPanel.addTab("Dialogs", dialogList);
							tabPanel.addTab("Properties", props);
							setTile("ACTOR " + doc.getId(a));

							// select previous selected tab
							if (selTitle != null) {
								for (int i = 0; i < tabPanel.getTabCount(); i++) {
									if (tabPanel.getTitleAt(i).equals(selTitle)) {
										tabPanel.setTab(i);
									}
								}
							}
						} else {
							setTile("ACTOR");
						}
						
						faList.addElements(doc, a, "animation");
						verbList.changeActor(doc, a);
						dialogList.addElements(doc, a, "dialog");
						soundList.addElements(doc, a, "sound");
						props.setActorDocument(doc, a);
						
					}

				});
		
	}
}

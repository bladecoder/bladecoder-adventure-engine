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
import java.util.HashMap;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.model.AnchorActor;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.ObstacleActor;
import com.bladecoder.engine.model.SoundFX;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.components.HeaderPanel;
import com.bladecoder.engineeditor.ui.components.TabPanel;

public class ActorPanel extends HeaderPanel {

	private TabPanel tabPanel;
	private VerbList verbList;
	private DialogList dialogList;
	private SpriteList faList;
	private SoundList soundList;
//	private ActorProps props;

	public ActorPanel(Skin skin) {
		super(skin, "ACTOR");
		tabPanel = new TabPanel(skin);
		verbList = new VerbList(skin);
		dialogList = new DialogList(skin);
		faList = new SpriteList(skin);
//		props = new ActorProps(skin);
		soundList = new SoundList(skin);
		
		setContent(tabPanel);

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ACTOR_SELECTED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						BaseActor a = (BaseActor) e.getNewValue();

						String selTitle = tabPanel.getSelectedIndex() == -1? null: tabPanel.getTitleAt(tabPanel.getSelectedIndex());
						tabPanel.clear();

						if (a != null) {

							tabPanel.addTab("Verbs", verbList);
							
							if (a instanceof SpriteActor)
								tabPanel.addTab("Animations", faList);

							if (!(a instanceof ObstacleActor) &&
									!(a instanceof AnchorActor))
								tabPanel.addTab("Sounds", soundList);

							if (a instanceof CharacterActor) {
								tabPanel.addTab("Dialogs", dialogList);
							}
							
							
//							tabPanel.addTab("Actor Props", props);
							setTile("ACTOR " + a.getId());

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
						
						if(a instanceof SpriteActor) {
							HashMap<String, AnimationDesc> anims = ((SpriteActor) a).getRenderer().getAnimations();
							if(anims != null)
								faList.addElements((SpriteActor)a, Arrays.asList(anims.values().toArray(new AnimationDesc[0])));
							else
								faList.addElements((SpriteActor)a, null);
						} else {
							faList.addElements(null, null);
						}
						
						if(a instanceof InteractiveActor) {
							HashMap<String, SoundFX> sounds = ((InteractiveActor) a).getSounds();
							if(sounds != null)
								soundList.addElements((InteractiveActor)a, Arrays.asList(sounds.values().toArray(new SoundFX[0])));
							else
								soundList.addElements((InteractiveActor)a, null);
						} else {
							soundList.addElements(null, null);
						}
						
						verbList.changeActor();
						
						if(a instanceof CharacterActor) {
							
							HashMap<String, Dialog> dialogs = ((CharacterActor) a).getDialogs();
							if(dialogs != null)
								dialogList.addElements((CharacterActor)a, Arrays.asList(dialogs.values().toArray(new Dialog[0])));
							else
								dialogList.addElements((CharacterActor)a, null);
						} else {
							dialogList.addElements(null, null);
						}
						
//						props.setActorDocument(a);
						
					}

				});
		
	}
}

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
package com.bladecoder.engineeditor.ui.components;

import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;

public class SceneActorInputPanel extends InputPanel {
	SelectBox<String> scene;
	EditableSelectBox<String> actor;
	Table panel;

	SceneActorInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		panel = new Table(skin);
		scene = new SelectBox<>(skin);
		actor = new EditableSelectBox<>(skin);

		panel.add(new Label(" Scene ", skin));
		panel.add(scene);
		panel.add(new Label("  Actor ", skin));
		panel.add(actor);

		Scene[] scenes = World.getInstance().getScenes().values().toArray(new Scene[0]);
		
		int l = scenes.length + 1;
		
		String values[] = new String[l];

		values[0] = "";

		for (int i = 0; i < scenes.length; i++) {
			values[i + 1] = scenes[i].getId();
		}
		
		Arrays.sort(values);
		
		scene.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				sceneSelected();
			}
		});		

		init(skin, title, desc, panel, mandatory, defaultValue);
		
		scene.setItems(values);

		if (values.length > 0) {
			if (defaultValue != null)
				setText(defaultValue);
			else
				scene.setSelectedIndex(0);
		}
		
		
	}
	
	private void sceneSelected() {
		String s = scene.getSelected();
		
		if(s == null || s.isEmpty()) {
			s = Ctx.project.getSelectedScene().getId();
		}
		
		Scene scn = World.getInstance().getScene(s);
		
		HashMap<String, BaseActor> actors = scn.getActors();
		BaseActor[] v = actors.values().toArray(new BaseActor[actors.size()]);
		
		
		int l = actors.size();
		if(!isMandatory()) l++;
		String values[] = new String[l];
		
		if(!isMandatory()) {
			values[0] = "";
		}
		
		for(int i = 0; i < actors.size(); i++) {
			if(isMandatory())
				values[i] = v[i].getId();
			else
				values[i+1] = v[i].getId();
		}
		
		Arrays.sort(values);
		actor.setItems(values);	
	}
	
	public String getText() {
		return (new SceneActorRef(scene.getSelected(), actor.getSelected())).toString();
	}

	public void setText(String s) {	
		SceneActorRef aa = new SceneActorRef(s);
		
		scene.setSelected(aa.getSceneId() == null?"":aa.getSceneId());
		sceneSelected();
		actor.setSelected(aa.getActorId());
	}
}

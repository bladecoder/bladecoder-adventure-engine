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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;

public class SceneActorInputPanel extends InputPanel {
	SelectBox<String> scene;
	EditableSelectBox<String> actor;
	Table panel;
	
	private final Param.Type type; 
	

	SceneActorInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, Param.Type type) {
		
		this.type = type;
		
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
		
		actor.setItems(getActorValues(scn));	
	}
	
	public String getText() {
		if(actor.getSelected().isEmpty())
			return null;
		
		return (new SceneActorRef(scene.getSelected(), actor.getSelected())).toString();
	}

	public void setText(String s) {	
		SceneActorRef aa = new SceneActorRef(s);
		
		scene.setSelected(aa.getSceneId() == null?"":aa.getSceneId());
		sceneSelected();
		actor.setSelected(aa.getActorId());
	}
	
	private String[] getActorValues(Scene scn) {
		HashMap<String, BaseActor> actors = scn.getActors();
		
		ArrayList<BaseActor> filteredActors = new ArrayList<BaseActor>();
		
		for(BaseActor a: actors.values()) {
			if(type == Param.Type.SCENE_CHARACTER_ACTOR) {
				if(a instanceof CharacterActor)
					filteredActors.add(a);
			} else if(type == Param.Type.SCENE_INTERACTIVE_ACTOR) {
				if(a instanceof InteractiveActor)
					filteredActors.add(a);
			} else if(type == Param.Type.SCENE_SPRITE_ACTOR) {
				if(a instanceof SpriteActor)
					filteredActors.add(a);				
			} else {
				filteredActors.add(a);
			}
		}
		
		String[] result = new String[isMandatory()?filteredActors.size() + 1:filteredActors.size() + 2];
		
		// Add player variable to the list
		result[0] = Scene.VAR_PLAYER;
		
		if(!isMandatory())
			result[filteredActors.size() + 1] = "";
		
		for(int i = 0; i < filteredActors.size(); i++) {
			result[i+1] = filteredActors.get(i).getId();
		}
		
		Arrays.sort(result);
		
		return result;
	}
}

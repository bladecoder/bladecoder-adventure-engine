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

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;

public class ActorInputPanel extends EditableOptionsInputPanel<String> {
	
	ActorInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue, Param.Type type) {
		super(skin, title, desc, mandatory, defaultValue, getValues(mandatory, type));
		
		if(mandatory)
			if(type == Param.Type.ACTOR)
				setText(Ctx.project.getSelectedActor().getId());
			else
				input.setSelectedIndex(0);
	}

	private static String[] getValues(boolean mandatory, Param.Type type) {
		HashMap<String, BaseActor> actors = Ctx.project.getSelectedScene().getActors();
		
		ArrayList<BaseActor> filteredActors = new ArrayList<BaseActor>();
		
		for(BaseActor a: actors.values()) {
			if(type == Param.Type.CHARACTER_ACTOR) {
				if(a instanceof CharacterActor)
					filteredActors.add(a);
			} else if(type == Param.Type.INTERACTIVE_ACTOR) {
				if(a instanceof InteractiveActor)
					filteredActors.add(a);
			} else if(type == Param.Type.SPRITE_ACTOR) {
				if(a instanceof SpriteActor)
					filteredActors.add(a);				
			} else {
				filteredActors.add(a);
			}
		}
			
		String[] result = new String[filteredActors.size() + 1];
		
		// Add player variable to the list
		result[0] = Scene.VAR_PLAYER;
		
		for(int i = 0; i < filteredActors.size(); i++) {
			result[i+1] = filteredActors.get(i).getId();
		}
		
		Arrays.sort(result);
		return result;
	}
}

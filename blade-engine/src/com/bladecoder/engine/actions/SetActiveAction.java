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
package com.bladecoder.engine.actions;

import java.util.HashMap;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;

public class SetActiveAction implements Action {
	public static final String INFO = "Change the visible/interaction properties for the selected actor.";
	public static final Param[] PARAMS = {
		new Param("actor", "The target actor", Type.SCENE_ACTOR, false),
		new Param("visible", "sets the actor visibility", Type.BOOLEAN), 
		new Param("interaction", "when 'true' the actor responds to the user input", Type.BOOLEAN)
		};		
	
	String actorId;
	String sceneId;
	String visible;
	String interaction;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));
		
		sceneId = a[0];
		actorId = a[1];
		
		visible = params.get("visible");
		interaction = params.get("interaction");
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s;
		
		if(sceneId != null && !sceneId.isEmpty()) {
			s = World.getInstance().getScene(sceneId);
		} else {
			s = World.getInstance().getCurrentScene();
		}
		
		BaseActor actor = s.getActor(actorId, true);
		
		if(visible != null) actor.setVisible(Boolean.parseBoolean(visible));
		if(interaction != null) actor.setInteraction(Boolean.parseBoolean( interaction));
		
		return false;
	}

	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

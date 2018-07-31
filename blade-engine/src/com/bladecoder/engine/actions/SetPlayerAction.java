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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Sets the scene player")
public class SetPlayerAction implements Action {

	@ActionProperty(type = Type.SCENE_CHARACTER_ACTOR, required = true)
	@ActionPropertyDescription("The scene player")	
	private SceneActorRef actor;
	
	@ActionProperty
	@ActionPropertyDescription("The inventory 'id' for the player. If empty, the inventory will not change.")	
	private String inventory;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {		
		Scene s = actor.getScene(w);

		BaseActor a = s.getActor(actor.getActorId(), true);
		
		s.setPlayer((CharacterActor)a);
		
		if(inventory != null) {
			w.getInventory().dispose();
			w.setInventory(inventory);
			w.getInventory().loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			w.getInventory().retrieveAssets();
		}

		return false;
	}

}

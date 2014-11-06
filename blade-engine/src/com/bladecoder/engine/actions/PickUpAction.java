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
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;

public class PickUpAction implements Action {
	public static final String INFO = "Puts the selected actor in the inventory.";
	public static final Param[] PARAMS = {
		new Param("scene", "If not empty, pickup the actor from the selected scene", Type.SCENE),
		new Param("animation", "The animation/sprite to show while in inventory. If empty, the animation will be 'actorid.inventory'", Type.STRING)
		};
	
	String actorId;
	String fa;
	String scene;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		fa = params.get("animation");
		scene = params.get("scene");
	}

	@Override
	public boolean run(ActionCallback cb) {

		BaseActor actor = null;
		
		Scene scn;
		
		if(scene != null) {
			scn = World.getInstance().getScene(scene);
			actor = scn.getActor(actorId, false);
			actor.loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			actor.retrieveAssets();
		} else {
			scn = World.getInstance().getCurrentScene();
			actor = scn.getActor(actorId, false);
		}
		
		scn.removeActor(actor);
		
		if (actor instanceof SpriteActor) {
			SpriteActor a = (SpriteActor) actor;

			if(fa != null)
				a.startAnimation(fa, null);
			else
				a.startAnimation(a.getId() + ".inventory", null);
			
			World.getInstance().getInventory().addItem(a);
		}
		
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

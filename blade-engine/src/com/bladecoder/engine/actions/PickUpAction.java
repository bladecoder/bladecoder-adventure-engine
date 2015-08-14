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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;

public class PickUpAction implements Action {
	public static final String INFO = "Puts the selected actor in the inventory.";
	public static final Param[] PARAMS = {
		new Param("actor", "The target actor", Type.SCENE_ACTOR, false),
		new Param("animation", "The animation/sprite to show while in inventory. If empty, the animation will be 'actorid.inventory'", Type.STRING)
		};
	
	private String actorId;
	private String animation;
	private String sceneId;

	@Override
	public void setParams(HashMap<String, String> params) {
		animation = params.get("animation");
		
		String[] a = Param.parseString2(params.get("actor"));
		
		actorId = a[1];
		sceneId = a[0];
	}

	@Override
	public boolean run(ActionCallback cb) {

		InteractiveActor actor = null;
		
		Scene scn;
		
		if(sceneId != null) {
			scn = World.getInstance().getScene(sceneId);
			actor = (InteractiveActor)scn.getActor(actorId, false);
			actor.loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			actor.retrieveAssets();
		} else {
			scn = World.getInstance().getCurrentScene();
			actor = (InteractiveActor)scn.getActor(actorId, false);
		}
		
		scn.removeActor(actor);
		
		if (actor instanceof SpriteActor) {
			SpriteActor a = (SpriteActor) actor;

			if(animation != null)
				a.startAnimation(animation, null);
			else if(a.getRenderer().getAnimations().get(a.getId() + ".inventory") != null)
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

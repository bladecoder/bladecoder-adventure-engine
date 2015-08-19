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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Puts the selected actor in the inventory.")
public class PickUpAction implements Action {
	@JsonProperty("actor")
	@JsonPropertyDescription("The target actor")
	@ActionPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef sceneActorRef;

	@JsonProperty
	@JsonPropertyDescription("The animation/sprite to show while in inventory. If empty, the animation will be 'actorid.inventory'")
	@ActionPropertyType(Type.STRING)
	private String animation;

	@Override
	public void setParams(HashMap<String, String> params) {
		animation = params.get("animation");
		
		String[] a = Param.parseString2(params.get("actor"));
		
		sceneActorRef = new SceneActorRef(a[0], a[1]);
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene scn = this.sceneActorRef.getScene();
		InteractiveActor actor = (InteractiveActor)scn.getActor(this.sceneActorRef.getActorId(), false);

		if (this.sceneActorRef.getSceneId() != null) {
			actor.loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			actor.retrieveAssets();
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
	public Param[] getParams() {
		return null;
	}

}

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
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Puts the selected actor in the inventory.")
public class PickUpAction implements Action {
	@ActionProperty(type = Type.SCENE_SPRITE_ACTOR, required = true)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	@ActionProperty
	@ActionPropertyDescription("The animation/sprite to show while in inventory. If empty, the animation will be 'actorid.inventory'")
	private String animation;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene scn = this.actor.getScene(w);
		InteractiveActor actor = (InteractiveActor) scn.getActor(this.actor.getActorId(), false);

		if (actor == null) {
			EngineLogger.error("PickUpAction - Actor not found:" + this.actor.getActorId());

			return false;
		}

		scn.removeActor(actor);

		if (actor instanceof SpriteActor) {
			SpriteActor a = (SpriteActor) actor;
			
			if (scn != w.getCurrentScene()  &&
					w.getCachedScene(scn.getId()) == null
					) {
				a.loadAssets();
				EngineAssetManager.getInstance().finishLoading();
				a.retrieveAssets();
			}

			if (a.getRenderer() instanceof AnimationRenderer) {
				if (animation != null)
					a.startAnimation(animation, null);
				else if (((AnimationRenderer) a.getRenderer()).getAnimations().get(a.getId() + ".inventory") != null)
					a.startAnimation(a.getId() + ".inventory", null);
			}

			w.getInventory().addItem(a);
		}

		return false;
	}

}

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

import com.badlogic.gdx.utils.Disposable;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Move the actor to the selected scene")
public class MoveToSceneAction implements Action {
	@ActionProperty(required=true)
	@ActionPropertyDescription("The selected actor")	
	private SceneActorRef actor;

	@ActionPropertyDescription("The target scene. The current scene if empty.")
	@ActionProperty(type = Type.SCENE)
	private String scene;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {			
		final Scene s = actor.getScene(w);

		final String actorId = actor.getActorId();		
		
		if (actorId == null) {
			// if called in a scene verb and no actor is specified, we do nothing
			EngineLogger.error(getClass() + ": No actor specified");
			return false;
		}

		InteractiveActor a = (InteractiveActor)s.getActor(actorId, false);

		s.removeActor(a);
		
		if(s == w.getCurrentScene() && a instanceof Disposable)
			((Disposable) a).dispose();
		
		Scene ts =  null;
		
		if(scene == null)
			ts = w.getCurrentScene();
		else
			ts = w.getScene(scene);
		
		// We must load assets when the target scene is the current scene or when
		// the scene is cached.
		if((ts == w.getCurrentScene() || 
				w.getCachedScene(ts.getId()) != null) && a instanceof AssetConsumer) {
			((AssetConsumer) a).loadAssets();
			EngineAssetManager.getInstance().finishLoading();
			((AssetConsumer) a).retrieveAssets();
		}
		
		ts.addActor(a);
		
		return false;
	}


}

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
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription(name = "SetWalkzone", value = "Set the scene walkzone.")
public class SetWalkzoneAction implements Action {
	@ActionProperty(type = Type.SCENE_WALKZONE_ACTOR, required = false)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		if (actor == null) {
			w.getCurrentScene().setWalkZone(null);
		} else {
			Scene s = actor.getScene(w);

			s.setWalkZone(actor.getActorId());
			
			// We must recalc the walkzone when the target scene is the current scene or when
			// the scene is cached.
			if(s == w.getCurrentScene() || 
					w.getCachedScene(s.getId()) != null) {
				s.calcWalkzone();
			}
		}

		return false;
	}

}

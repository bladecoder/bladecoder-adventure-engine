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

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionDescription;
import com.bladecoder.engine.actions.ActionProperty;
import com.bladecoder.engine.actions.ActionPropertyDescription;
import com.bladecoder.engine.actions.ActorAnimationRef;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.spine.SpineRenderer;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("ONLY FOR SPINE ACTORS: Sets a secondary animation")
public class SpineSecondaryAnimationAction implements Action {

	@ActionProperty(required = false)
	@ActionPropertyDescription("The Animation to set. Empty to clear the secondary animation")
	private ActorAnimationRef animation;

	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}
	
	@Override
	public boolean run(VerbRunner cb) {
		
		String actorId = animation.getActorId();
		
		SpriteActor a = (SpriteActor) w.getCurrentScene().getActor(actorId, true);
		
		if(a.getRenderer() instanceof SpineRenderer) {
			SpineRenderer r = (SpineRenderer) a.getRenderer();
			String anim = animation.getAnimationId();
			
			if(anim.isEmpty())
				anim = null;
			
			r.setSecondaryAnimation(anim);
		} else {
			EngineLogger.error("SpineSecondaryAnimation: The actor renderer has to be of Spine type.");
		}

		return false;
	}

}

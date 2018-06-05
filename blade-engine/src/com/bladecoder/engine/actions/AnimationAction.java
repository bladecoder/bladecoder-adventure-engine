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

import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Sets the animation for an actor")
public class AnimationAction implements Action {

	@ActionProperty(required = true)
	@ActionPropertyDescription("The Animation to set")
	private ActorAnimationRef animation;

	@ActionProperty(required = true, defaultValue = "-1")
	@ActionPropertyDescription("The times to repeat. -1 to infinity repeat")
	private int count = -1;

	@ActionProperty(required = true)
	@ActionPropertyDescription("Waits to finish the animation.")
	private boolean wait = true;

	@ActionProperty(required = true, defaultValue = "SPRITE_DEFINED")
	@ActionPropertyDescription("The repeat mode")
	private Tween.Type repeat = Tween.Type.SPRITE_DEFINED;
	
	@ActionProperty(required = true, defaultValue = "false")
	@ActionPropertyDescription("Keeps the current actor animation direction.")
	private boolean keepDirection = false;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
//		EngineLogger.debug(MessageFormat.format("ANIMATION_ACTION: {0}.{1}", animation.getActorId(), animation.getAnimationId()));
		
		String actorId = animation.getActorId();
		
		SpriteActor a = (SpriteActor) w.getCurrentScene().getActor(actorId, true);
		
		String anim = animation.getAnimationId();
		
		if(keepDirection) {
			String c = ((AnimationRenderer)a.getRenderer()).getCurrentAnimationId();
			
			if(anim.endsWith(AnimationRenderer.LEFT) && c.endsWith(AnimationRenderer.RIGHT) || 
					anim.endsWith(AnimationRenderer.RIGHT) && c.endsWith(AnimationRenderer.LEFT)) {
				anim = AnimationRenderer.getFlipId(anim);
			}
		}

		a.startAnimation(anim, repeat, count, wait?cb:null);

		return wait;
	}

}

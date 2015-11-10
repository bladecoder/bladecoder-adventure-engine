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

import java.text.MessageFormat;

import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Sets the animation for an actor")
public class AnimationAction implements Action {

	@ActionProperty(required = true)
	@ActionPropertyDescription("The Animation to set")
	private ActorAnimationRef animation;

	@ActionProperty
	@ActionPropertyDescription("The times to repeat. -1 to infinity repeat")
	private int count = 1;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	@ActionProperty(required = true, defaultValue = "SPRITE_DEFINED")
	@ActionPropertyDescription("The repeat mode")
	private Tween.Type repeat = Tween.Type.SPRITE_DEFINED;

	@Override
	public boolean run(VerbRunner cb) {
		EngineLogger.debug(MessageFormat.format("ANIMATION_ACTION: {0}", animation.getAnimationId()));
		
		String actorId = animation.getActorId();
		
		SpriteActor a = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId, true);

		a.startAnimation(animation.getAnimationId(), repeat, count, wait?cb:null);

		return wait;
	}

}

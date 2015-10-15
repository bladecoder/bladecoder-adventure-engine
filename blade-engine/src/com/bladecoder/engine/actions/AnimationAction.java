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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Sets the animation for an actor")
public class AnimationAction implements Action {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The Animation to set")
	@ActionPropertyType(Type.ACTOR_ANIMATION)
	private ActorAnimationRef animation;

	@JsonProperty
	@JsonPropertyDescription("The times to repeat. -1 to infinity repeat")
	@ActionPropertyType(Type.INTEGER)
	private int count = 1;

	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean wait = true;

	@JsonProperty(required = true, defaultValue = "SPRITE_DEFINED")
	@JsonPropertyDescription("The repeat mode")
	@ActionPropertyType(Type.STRING)
	private Tween.Type repeat = Tween.Type.SPRITE_DEFINED;

	private String actor;

	@Override
	public boolean run(ActionCallback cb) {
		EngineLogger.debug(MessageFormat.format("ANIMATION_ACTION: {0}", animation));
		
		String actorId = animation.getActorId();
		
		if(actorId == null)
			actorId = actor;

		SpriteActor a = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId, true);

		a.startAnimation(animation.getAnimationId(), repeat, count, wait?cb:null);

		return wait;
	}

}

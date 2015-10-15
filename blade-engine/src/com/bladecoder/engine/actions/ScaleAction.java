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
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.InterpolationMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Sets an actor Scale animation")
public class ScaleAction implements Action {
	@JsonProperty
	@JsonPropertyDescription("The target actor")
	@ActionPropertyType(Type.ACTOR)
	private String actor;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The target scale")
	@ActionPropertyType(Type.FLOAT)
	private float scale;

	@JsonProperty(required = true, defaultValue = "1.0")
	@JsonPropertyDescription("Duration of the animation in seconds")
	@ActionPropertyType(Type.FLOAT)
	private float speed;

	@JsonProperty
	@JsonPropertyDescription("The The times to repeat")
	@ActionPropertyType(Type.INTEGER)
	private int count = 1;

	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the transition is showed and the action continues inmediatly")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean wait = true;

	@JsonProperty(required = true, defaultValue = "REPEAT")
	@JsonPropertyDescription("The repeat mode")
	@ActionPropertyType(Type.STRING)
	private Tween.Type repeat = Tween.Type.REPEAT;

	@JsonProperty
	@JsonPropertyDescription("The interpolation mode")
	@ActionPropertyType(Type.OPTION)
	private InterpolationMode interpolation;

	@Override
	public boolean run(ActionCallback cb) {				
		SpriteActor a = (SpriteActor) World.getInstance().getCurrentScene().getActor(actor, false);

		a.startScaleAnimation(repeat, count, speed, scale, interpolation, wait?cb:null);
		
		return wait;
	}

}

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
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.InterpolationMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ModelDescription("Sets an actor Scale animation")
public class ScaleAction implements Action {
	@JsonProperty
	@JsonPropertyDescription("The target actor")
	@ModelPropertyType(Type.ACTOR)
	private String actorId;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The target scale")
	@ModelPropertyType(Type.FLOAT)
	private float scale;

	@JsonProperty(required = true, defaultValue = "1.0")
	@JsonPropertyDescription("Duration of the animation in seconds")
	@ModelPropertyType(Type.FLOAT)
	private float speed;

	@JsonProperty
	@JsonPropertyDescription("The The times to repeat")
	@ModelPropertyType(Type.INTEGER)
	private int count = 1;

	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the transition is showed and the action continues inmediatly")
	@ModelPropertyType(Type.BOOLEAN)
	private boolean wait = true;

	@JsonProperty(required = true, defaultValue = "REPEAT")
	@JsonPropertyDescription("The repeat mode")
	@ModelPropertyType(Type.STRING)
	private Tween.Type repeat = Tween.Type.REPEAT;

	@JsonProperty
	@JsonPropertyDescription("The interpolation mode")
	@ModelPropertyType(Type.OPTION)
	private InterpolationMode interpolation;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		// get final position. We need to scale the coordinates to the current resolution
		scale = Float.parseFloat(params.get("scale"));
		
		speed = Float.parseFloat(params.get("speed"));
		
		if(params.get("count") != null) {
			count = Integer.parseInt(params.get("count"));
		}
		
		if(params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}
		
		if(params.get("repeat") != null) {
			String repeatStr = params.get("repeat");
			repeat = Tween.Type.valueOf(repeatStr.trim().toUpperCase());    // FIXME: Check that this is the value being stored
		}

		interpolation = InterpolationMode.valueOf(params.get("interpolation").trim().toUpperCase());
	}

	@Override
	public boolean run(ActionCallback cb) {				
		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId, false);

		actor.startScaleAnimation(repeat, count, speed, scale, interpolation, wait?cb:null);
		
		return wait;
	}

}

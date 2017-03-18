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
package com.bladecoder.engine.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.util.InterpolationMode;

/**
 * Tween for SpriteActor alpha animation
 */
public class SpriteAlphaTween extends Tween<SpriteActor> {
	
	private float startAlpha;
	private float targetAlpha;
	
	public SpriteAlphaTween() {
	}

	public void start(SpriteActor target, Type repeatType, int count, float tAlpha, float duration, InterpolationMode interpolation, ActionCallback cb) {
		
		setTarget(target);
		
		if(target.getTint() == null)
			target.setTint(Color.WHITE.cpy());
		
		startAlpha = target.getTint().a;
		targetAlpha = tAlpha;
		
		setDuration(duration);
		setType(repeatType);
		setCount(count);
		setInterpolation(interpolation);

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}

	@Override
	public void updateTarget() {
		target.getTint().a = startAlpha + getPercent() * (targetAlpha - startAlpha);
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startAlpha", startAlpha);
		json.writeValue("targetAlpha", targetAlpha);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		startAlpha = json.readValue("startAlpha", float.class, 1.0f, jsonData);
		targetAlpha = json.readValue("targetAlpha", float.class, 1.0f, jsonData);
	}
}

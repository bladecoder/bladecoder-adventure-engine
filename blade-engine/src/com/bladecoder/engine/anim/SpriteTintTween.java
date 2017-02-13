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
 * Tween for SpriteActor tint animation
 */
public class SpriteTintTween extends Tween<SpriteActor> {
	
	private Color startColor;
	private Color targetColor;
	
	public SpriteTintTween() {
	}

	public void start(SpriteActor target, Type repeatType, int count, Color tColor, float duration, InterpolationMode interpolation, ActionCallback cb) {
		
		setTarget(target);
		
		startColor = target.getTint().cpy();
		targetColor = tColor.cpy();
		
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
		
		target.getTint().a = startColor.a + getPercent() * (targetColor.a - startColor.a);
		target.getTint().r = startColor.r + getPercent() * (targetColor.r - startColor.r);
		target.getTint().g = startColor.g + getPercent() * (targetColor.g - startColor.g);
		target.getTint().b = startColor.b + getPercent() * (targetColor.b - startColor.b);
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startColor", startColor);
		json.writeValue("targetScl", targetColor);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		startColor = json.readValue("startColor", Color.class, jsonData);
		targetColor = json.readValue("targetColor", Color.class, jsonData);
	}
}

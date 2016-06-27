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

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.common.InterpolationMode;
import com.bladecoder.engine.model.SpriteActor;

/**
 * Tween for spriteactor position animation
 * 
 * TODO: Y speed depends on scale if fake depth is used
 */
public class SpritePosTween extends Tween {
	
	private float startX, startY;
	private float targetX, targetY;
	private InterpolationMode interpolationX;
	private InterpolationMode interpolationY;
	
	public SpritePosTween() {
	}

	public void start(SpriteActor target, Tween.Type repeatType, int count, float tx, float ty, float duration, InterpolationMode interpolation, ActionCallback cb) {
		start(target, repeatType, count, tx, ty, duration, interpolation, interpolation, cb);
	}
	
	public void start(SpriteActor target, Tween.Type repeatType, int count, float tx, float ty, float duration, 
			InterpolationMode interpolationX, InterpolationMode interpolationY, ActionCallback cb) {
		
		startX = target.getX();
		startY = target.getY();
		targetX = tx;
		targetY = ty;
		
		setDuration(duration);
		setType(repeatType);
		setCount(count);
		
		this.interpolationX = interpolationX;
		this.interpolationY = interpolationY;

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}	
	
	public void update(SpriteActor a, float delta) {
		update(delta);
		
		float percentX = getPercent(interpolationX);
		float percentY = getPercent(interpolationY);
		
		a.setPosition(startX + percentX * (targetX - startX),
				startY + percentY * (targetY - startY));
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startX", startX);
		json.writeValue("startY", startY);
		json.writeValue("targetX", targetX);
		json.writeValue("targetY", targetY);
		json.writeValue("interpolationX", interpolationX);
		json.writeValue("interpolationY", interpolationY);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		startX = json.readValue("startX", Float.class, jsonData);
		startY = json.readValue("startY", Float.class, jsonData);
		targetX = json.readValue("targetX", Float.class, jsonData);
		targetY = json.readValue("targetY", Float.class, jsonData);
		interpolationX = json.readValue("interpolationX", InterpolationMode.class, jsonData);
		interpolationY = json.readValue("interpolationY", InterpolationMode.class, jsonData);

	}
}

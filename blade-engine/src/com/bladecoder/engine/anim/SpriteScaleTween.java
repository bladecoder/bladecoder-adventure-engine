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

import com.bladecoder.engine.anim.Tween;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.SpriteActor;

/**
 * Tween for spriteactor scale animation
 */
public class SpriteScaleTween extends Tween {
	
	private float startScl;
	private float targetScl;
	
	public SpriteScaleTween() {
	}

	public void start(SpriteActor target, int repeatType, int count, float tScl, float duration, ActionCallback cb) {
		
		startScl = target.getScale();
		targetScl = tScl;
		
		setDuration(duration);
		setType(repeatType);
		setCount(count);

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}
	
	public void update(SpriteActor a, float delta) {
		update(delta);
		
		a.setScale(startScl + getPercent() * (targetScl - startScl));
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startScl", startScl);
		json.writeValue("targetScl", targetScl);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		startScl = json.readValue("startScl", Float.class, jsonData);
		targetScl = json.readValue("targetScl", Float.class, jsonData);
	}
}

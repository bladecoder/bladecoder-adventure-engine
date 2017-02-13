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

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.model.SceneCamera;
import com.bladecoder.engine.util.InterpolationMode;

/**
 * Tween for camera position and zoom animation
 */
public class CameraTween extends Tween<SceneCamera> {

	private float startX, startY, startZoom;
	private float targetX, targetY, targetZoom;

	public CameraTween() {
	}

	public void start(SceneCamera camera, Tween.Type repeatType, int count, float targetX, float targetY,
			float targetZoom, float duration, InterpolationMode interpolation, ActionCallback cb) {

		this.target = camera;
		
		Vector2 currentPos = camera.getPosition();

		startX = currentPos.x;
		startY = currentPos.y;
		startZoom = camera.getZoom();
		this.targetX = targetX;
		this.targetY = targetY;
		this.targetZoom = targetZoom;

		setDuration(duration);
		setType(repeatType);
		setCount(count);
		setInterpolation(interpolation);

		if (cb != null) {
			setCb(cb);
		}
	}

	@Override
	public void updateTarget() {
		target.setZoom(startZoom + getPercent() * (targetZoom - startZoom));
		target.setPosition(startX + getPercent() * (targetX - startX), startY + getPercent() * (targetY - startY));
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startX", startX);
		json.writeValue("startY", startY);
		json.writeValue("startZoom", startZoom);
		json.writeValue("targetX", targetX);
		json.writeValue("targetY", targetY);
		json.writeValue("targetZoom", targetZoom);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		startX = json.readValue("startX", Float.class, jsonData);
		startY = json.readValue("startY", Float.class, jsonData);
		startZoom = json.readValue("startZoom", Float.class, jsonData);
		targetX = json.readValue("targetX", Float.class, jsonData);
		targetY = json.readValue("targetY", Float.class, jsonData);
		targetZoom = json.readValue("targetZoom", Float.class, jsonData);
	}
}

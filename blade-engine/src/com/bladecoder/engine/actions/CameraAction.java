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

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.SceneCamera;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ModelDescription("Set/Animates the camera position and zoom. Also can stablish the follow character parameter")
public class CameraAction implements Action {
	@JsonProperty
	@JsonPropertyDescription("The target position")
	@ModelPropertyType(Type.VECTOR2)
	private Vector2 pos;

	@JsonProperty
	@JsonPropertyDescription("The target 'zoom'")
	@ModelPropertyType(Type.FLOAT)
	private float zoom = -1;

	@JsonProperty
	@JsonPropertyDescription("Duration of the animation in seconds. If not '0' and animation is triggered")
	@ModelPropertyType(Type.FLOAT)
	private float duration;

	@JsonProperty
	@JsonPropertyDescription("Sets the actor to follow. 'none' puts no actor to follow")
	@ModelPropertyType(Type.ACTOR)
	private String followActor;

	@JsonProperty(defaultValue = "true", required = true)
	@JsonPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	@ModelPropertyType(Type.BOOLEAN)
	private boolean wait = true;

	@Override
	public void setParams(HashMap<String, String> params) {
		followActor = params.get("followActor");

		if (params.get("pos") != null)
			pos = Param.parseVector2(params.get("pos"));

		if (params.get("zoom") != null)
			zoom = Float.parseFloat(params.get("zoom"));

		if (params.get("duration") != null)
			duration = Float.parseFloat(params.get("duration"));
		else
			duration = 0;

		if (params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}
		
		if(duration == 0) wait = false;
	}

	@Override
	public boolean run(ActionCallback cb) {

		float scale = EngineAssetManager.getInstance().getScale();

		SceneCamera camera = World.getInstance().getSceneCamera();
		
		if(zoom == -1)
			zoom = camera.getZoom();
		
		if(pos == null) {
			pos = camera.getPosition();
			pos.x /= scale;
			pos.y /= scale;
		}

		if (followActor != null) {
			if (followActor.equals("none"))
				World.getInstance().getCurrentScene().setCameraFollowActor(null);
			else
				camera.updatePos((SpriteActor) World.getInstance().getCurrentScene()
						.getActor(followActor, false));
		}

		if (duration == 0) {
			camera.setZoom(zoom);
			camera.setPosition(pos.x * scale, pos.y * scale);
		} else {
			camera.startAnimation(pos.x * scale, pos.y * scale, zoom, duration, wait?cb:null);
		}
		
		return wait;
	}

}

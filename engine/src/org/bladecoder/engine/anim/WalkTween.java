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
package org.bladecoder.engine.anim;

import java.util.ArrayList;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.util.ActionCallbackSerialization;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for frame animation
 */
public class WalkTween extends SpritePosTween implements Serializable {

	private ArrayList<Vector2> walkingPath;
	private int currentStep = 0;
	private float speed = 0;
	
	private ActionCallback walkCb;

	public WalkTween() {
	}

	public void start(SpriteActor target, ArrayList<Vector2> walkingPath,
			float speed, ActionCallback cb) {
		this.walkingPath = walkingPath;
		this.speed = speed;
		this.currentStep = 0;

		if (cb != null) {
			walkCb = cb;
		}

		restart();
		walkToNextStep(target);
	}
	
	private void walkToNextStep(SpriteActor target) {
		Vector2 p0 = walkingPath.get(currentStep);
		Vector2 pf = walkingPath.get(currentStep + 1);

		target.startWalkFA(p0, pf);

		float segmentDuration = p0.dst(pf)
				/ (EngineAssetManager.getInstance().getScale() * speed);
		
		start(target, NO_REPEAT, 1, pf.x, pf.y, segmentDuration, currentStep < walkingPath.size() - 2?null:walkCb);
	}

	private void segmentEnded(SpriteActor target) {

		currentStep++;

		if (currentStep < walkingPath.size() - 1) {
			walkToNextStep(target);
		} else { // WALK ENDED
			target.stand();
		}
	}

	@Override
	public void update(SpriteActor a, float delta) {
		super.update(a, delta);
		
		if(isComplete())
			segmentEnded(a);
	}


	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("path", walkingPath);
		json.writeValue("currentStep", currentStep);
		json.writeValue("speed", speed);
		
		json.writeValue("walkCb", ActionCallbackSerialization.find(walkCb),
				walkCb == null ? null : String.class);		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);
		
		walkingPath = json.readValue("path", ArrayList.class, Vector2.class, jsonData);
		currentStep = json.readValue("currentStep", Integer.class, jsonData);
		speed = json.readValue("speed", Float.class, jsonData);
		
		String cbSer = json.readValue("walkCb", String.class, jsonData);
		walkCb = ActionCallbackSerialization.find(cbSer);
	}
}

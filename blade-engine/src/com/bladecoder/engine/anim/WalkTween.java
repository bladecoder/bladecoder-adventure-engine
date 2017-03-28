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

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.util.ActionCallbackSerialization;
import com.bladecoder.engine.util.InterpolationMode;

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

	public void start(CharacterActor target, ArrayList<Vector2> walkingPath,
			float speed, ActionCallback cb) {
		this.target = target;
		this.walkingPath = walkingPath;
		this.speed = speed;
		this.currentStep = 0;

		if (cb != null) {
			walkCb = cb;
		}

		restart();
		walkToNextStep(target);
	}
	
	private void walkToNextStep(CharacterActor target) {
		Vector2 p0 = walkingPath.get(currentStep);
		Vector2 pf = walkingPath.get(currentStep + 1);

		target.startWalkAnim(p0, pf);
		
		float s0 = target.getScene().getFakeDepthScale(p0.y);
		float sf = target.getScene().getFakeDepthScale(pf.y);

//		float segmentDuration = p0.dst(pf)
//				/ (EngineAssetManager.getInstance().getScale() * speed);
		
		// t = dst/((vf+v0)/2)
		float segmentDuration = p0.dst(pf)
				/ (EngineAssetManager.getInstance().getScale() * speed * (s0+sf) / 2);
				
		segmentDuration *=  (s0 > sf ?s0 / sf:sf/s0);
		
		InterpolationMode i =InterpolationMode.LINEAR;
		
		if(Math.abs(s0-sf) > .25) 
			i = s0 > sf?InterpolationMode.POW2OUT:InterpolationMode.POW2IN;
		
		if(currentStep == walkingPath.size() - 2 && walkCb != null) {			
			start(target, Type.NO_REPEAT, 1, pf.x, pf.y, segmentDuration, 
					InterpolationMode.LINEAR, i, walkCb);
		} else {
			start(target, Type.NO_REPEAT, 1, pf.x, pf.y, segmentDuration, 
					InterpolationMode.LINEAR, i, null);
		}
	}

	private void segmentEnded(CharacterActor target) {

		currentStep++;

		if (currentStep < walkingPath.size() - 1) {
			walkToNextStep(target);
		} else { // WALK ENDED
			target.stand();
		}
	}
	
	public void completeNow(CharacterActor target) {
		currentStep = walkingPath.size();
		
		Vector2 p = walkingPath.get(currentStep - 1);
		
		target.setPosition(p.x, p.y);
		target.stand();
		
		if(walkCb != null)
			ActionCallbackQueue.add(walkCb);
	}

	@Override
	public void updateTarget() {
		super.updateTarget();
		
		if(isComplete())
			segmentEnded((CharacterActor)target);
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
		
		String walkCbSer = json.readValue("walkCb", String.class, jsonData);
		walkCb = ActionCallbackSerialization.find(walkCbSer);
	}
}

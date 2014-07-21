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

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.AtlasRenderer;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for spriteactor position animation
 */
public class FATween extends Tween {
	
	public FATween() {
	}

	public void start(AtlasRenderer target, int repeatType, int count, float duration, ActionCallback cb) {		
		setDuration(duration);
		setType(repeatType);
		setCount(count);

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}
	
	public void update(AtlasRenderer a, float delta) {
		update(delta);
		
		if(!isComplete())
			a.setFrame((int)(getPercent() * a.getNumFrames()));
	}
	
	@Override
	public void write(Json json) {
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	

	}
}

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

public class AnimationDesc {
	public String id;
	public String source;
	public float duration;
	public Vector2 inD;
	public Vector2 outD;
	public Tween.Type animationType;
	public int count;

	public String sound;

	public boolean preload;
	public boolean disposeWhenPlayed;

	public AnimationDesc() {

	}

	public void set(String id, String source, float duration, int count, Tween.Type animationType,
			String sound, Vector2 inD, Vector2 outD, boolean preload, boolean disposeWhenPlayed) {
		this.id = id;
		this.duration = duration;
		this.animationType = animationType;
		this.count = count;

		this.source = source;
		this.sound = sound;

		this.inD = inD;
		this.outD = outD;

		this.preload = preload;
		this.disposeWhenPlayed = disposeWhenPlayed;
	}
}

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
	public final static String BACK = "back";
	public final static String FRONT = "front";
	public final static String RIGHT = "right";
	public final static String LEFT = "left";
	public final static String BACKRIGHT = "backright";
	public final static String BACKLEFT = "backleft";
	public final static String FRONTRIGHT = "frontright";
	public final static String FRONTLEFT = "frontleft";
	public final static String STAND_ANIM = "stand";
	public final static String WALK_ANIM = "walk";
	public final static String TALK_ANIM = "talk";	
		
	
	public	String id;
	public  String source;
	public	float duration;
	public	float delay;
	public  Vector2 inD;
	public  Vector2 outD;
	public	int animationType;
	public	int count;
	
	public String sound;
	
	public boolean preload;
	public boolean disposeWhenPlayed;
	
	public AnimationDesc() {
		
	}
	
	public void set(String id, String source, float duration, 
			float delay, int count, int animationType, String sound, 
			Vector2 inD, Vector2 outD, boolean preload, boolean disposeWhenPlayed) {
		this.id = id;
		this.duration = duration;
		this.delay = delay;
		this.animationType = animationType;
		this.count = count;
		
		this.source = source;
		this.sound = sound;
		
		this.inD = inD;
		this.outD = outD;
		
		this.preload = preload;
		this.disposeWhenPlayed = disposeWhenPlayed;
	}
	
	public static String getFlipId(String id) {
		StringBuilder sb = new StringBuilder();

		if (id.endsWith(LEFT)) {
			sb.append(id.substring(0, id.length() - LEFT.length()));
			sb.append(RIGHT);
		} else if (id.endsWith(RIGHT)) {
			sb.append(id.substring(0, id.length() - RIGHT.length()));
			sb.append(LEFT);
		}

		return sb.toString();
	}
	
	private final static float DIRECTION_ASPECT_TOLERANCE = 2.5f;

	public static String getFrameDirection(float x, float y, Vector2 pf) {
		float dx = pf.x - x;
		float dy = pf.y - y;
		float ratio = Math.abs(dx / dy);

		if (ratio < 1.0)
			ratio = 1.0f / ratio;

		// EngineLogger.debug("P0: " + p0 + " PF: " + pf + " dx: " + dx +
		// " dy: "
		// + dy + " RATIO: " + ratio);

		if (ratio < DIRECTION_ASPECT_TOLERANCE) { // DIAGONAL MOVEMENT
			if (dy > 0) { // UP. MOVEMENT
				if (dx > 0) { // TO THE RIGHT
					return BACKRIGHT;
				} else { // TO THE LEFT
					return BACKLEFT;
				}

			} else { // DOWN. MOVEMENT
				if (dx > 0) { // TO THE RIGHT
					return FRONTRIGHT;
				} else { // TO THE LEFT
					return FRONTLEFT;
				}
			}
		} else { // HOR OR VERT MOVEMENT
			if (Math.abs(dx) > Math.abs(dy)) { // HOR. MOVEMENT
				if (dx > 0) { // TO THE RIGHT
					return RIGHT;
				} else { // TO THE LEFT
					return LEFT;
				}

			} else { // VERT. MOVEMENT
				if (dy > 0) { // TO THE TOP
					return BACK;
				} else { // TO THE BOTTOM
					return FRONT;
				}
			}
		}
	}	
}

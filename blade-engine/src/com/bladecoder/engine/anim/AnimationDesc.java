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

import java.util.HashMap;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.util.EngineLogger;

public class AnimationDesc {
	public final static String BACK = "back";
	public final static String FRONT = "front";
	public final static String RIGHT = "right";
	public final static String LEFT = "left";
	public final static String BACKRIGHT = "backright";
	public final static String BACKLEFT = "backleft";
	public final static String FRONTRIGHT = "frontright";
	public final static String FRONTLEFT = "frontleft";

	public String id;
	public String source;
	public float duration;
	public float delay;
	public Vector2 inD;
	public Vector2 outD;
	public Tween.Type animationType;
	public int count;

	public String sound;

	public boolean preload;
	public boolean disposeWhenPlayed;

	public AnimationDesc() {

	}

	public void set(String id, String source, float duration, float delay, int count, Tween.Type animationType,
			String sound, Vector2 inD, Vector2 outD, boolean preload, boolean disposeWhenPlayed) {
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

	private final static float DIRECTION_ASPECT_TOLERANCE = 3f;
	private final static float DIRECTION_ASPECT_TOLERANCE_2 = 3f;

	public static String getDirectionString(Vector2 p0, Vector2 pf, int numDirs) {

		if (numDirs == 0 || numDirs == -1)
			return null;

		float dx = pf.x - p0.x;
		float dy = pf.y - p0.y;
		float ratio = Math.abs(dx / dy);
		float ratio2 = ratio;

		if (ratio2 < 1.0)
			ratio2 = 1.0f / ratio;

		EngineLogger.debug("P0: " + p0 + " PF: " + pf + " dx: " + dx + " dy: " + dy + " RATIO: " + ratio);

		if (ratio2 < DIRECTION_ASPECT_TOLERANCE && numDirs > 4) { // DIAGONAL
																	// MOVEMENT
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
			if (ratio > DIRECTION_ASPECT_TOLERANCE_2 || numDirs < 4) { // HOR.
																		// MOVEMENT
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

	/**
	 * Returns:
	 * 
	 * 8 -> when 8 dir animation mode (RIGHT, LEFT, FRONT, BACK, BACKRIGHT,
	 * BACKLEFT, FRONTRIGHT, FRONTLEFT) 4 -> when 4 dir animation mode (RIGHT,
	 * LEFT, FRONT, BACK) 2 -> when 2 dir animation mode (RIGHT, LEFT) 0 -> when
	 * no dirs availables for the base animation -1 -> when base animation
	 * doesn't exists
	 * 
	 * @param base
	 *            Base animation
	 * @param fanims
	 * @return -1, 0, 2, 4 or 8
	 */
	public static int getDirs(String base, HashMap<String, AnimationDesc> fanims) {
		String basePoint = base + ".";

		if (fanims.containsKey(basePoint + FRONTRIGHT) || fanims.containsKey(basePoint + FRONTLEFT))
			return 8;

		if (fanims.containsKey(basePoint + BACK))
			return 4;

		if (fanims.containsKey(basePoint + LEFT) || fanims.containsKey(basePoint + RIGHT))
			return 2;

		if (fanims.containsKey(base))
			return 0;

		return -1;
	}
}

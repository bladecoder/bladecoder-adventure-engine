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
import com.bladecoder.engine.model.AbstractModel;

public class AnimationDesc extends AbstractModel {
	public final static String BACK = "back";
	public final static String FRONT = "front";
	public final static String RIGHT = "right";
	public final static String LEFT = "left";
	public final static String BACKRIGHT = "backright";
	public final static String BACKLEFT = "backleft";
	public final static String FRONTRIGHT = "frontright";
	public final static String FRONTLEFT = "frontleft";
	
	private String source;
	private float speed;
	private float delay;
	private Vector2 inD;
	private Vector2 outD;
	private Tween.Type animationType;
	private int count;
	
	private String sound;
	
	private boolean preload;
	private boolean disposeWhenPlayed;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
	}

	public float getDelay() {
		return delay;
	}

	public void setDelay(float delay) {
		this.delay = delay;
	}

	public Vector2 getInD() {
		return inD;
	}

	public void setInD(Vector2 inD) {
		this.inD = inD;
	}

	public Vector2 getOutD() {
		return outD;
	}

	public void setOutD(Vector2 outD) {
		this.outD = outD;
	}

	public Tween.Type getAnimationType() {
		return animationType;
	}

	public void setAnimationType(Tween.Type animationType) {
		this.animationType = animationType;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getSound() {
		return sound;
	}

	public void setSound(String sound) {
		this.sound = sound;
	}

	public boolean isPreload() {
		return preload;
	}

	public void setPreload(boolean preload) {
		this.preload = preload;
	}

	public boolean isDisposeWhenPlayed() {
		return disposeWhenPlayed;
	}

	public void setDisposeWhenPlayed(boolean disposeWhenPlayed) {
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

	public static String getDirectionString(Vector2 p0, Vector2 pf) {
		float dx = pf.x - p0.x;
		float dy = pf.y - p0.y;
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
			if (Math.abs(dx) > Math.abs(dy) / DIRECTION_ASPECT_TOLERANCE) { // HOR. MOVEMENT
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

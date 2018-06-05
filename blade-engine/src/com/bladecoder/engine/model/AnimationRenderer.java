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
package com.bladecoder.engine.model;

import java.util.HashMap;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;

public abstract class AnimationRenderer implements ActorRenderer {

	public final static String BACK = "back";
	public final static String FRONT = "front";
	public final static String RIGHT = "right";
	public final static String LEFT = "left";
	public final static String BACKRIGHT = "backright";
	public final static String BACKLEFT = "backleft";
	public final static String FRONTRIGHT = "frontright";
	public final static String FRONTLEFT = "frontleft";

	private final static float DEFAULT_DIM = 200;

	protected HashMap<String, AnimationDesc> fanims = new HashMap<String, AnimationDesc>();

	/** Starts this anim the first time that the scene is loaded */
	protected String initAnimation;

	protected AnimationDesc currentAnimation;

	protected CacheEntry currentSource;
	protected boolean flipX;

	protected final HashMap<String, CacheEntry> sourceCache = new HashMap<String, CacheEntry>();
	protected Polygon bbox;

	public class CacheEntry {
		public int refCounter;
	}

	protected int orgAlign = Align.left;

	public abstract void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb);

	public abstract void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb,
			String direction);

	public abstract void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, Vector2 p0,
			Vector2 pf);

	public abstract String[] getInternalAnimations(AnimationDesc anim);

	public AnimationDesc getCurrentAnimation() {
		return currentAnimation;
	}

	@Override
	public float getWidth() {
		return DEFAULT_DIM;
	}

	@Override
	public float getHeight() {
		return DEFAULT_DIM;
	}

	public static float getAlignDx(float width, int align) {
		if ((align & Align.left) != 0)
			return 0;
		else if ((align & Align.right) != 0)
			return -width;
		else if ((align & Align.center) != 0)
			return -width / 2.0f;

		return -width / 2.0f;
	}

	public static float getAlignDy(float height, int align) {
		if ((align & Align.bottom) != 0)
			return 0;
		else if ((align & Align.top) != 0)
			return -height;
		else if ((align & Align.center) != 0)
			return -height / 2.0f;

		return 0;
	}

	public String getCurrentAnimationId() {
		if (currentAnimation == null)
			return null;

		String id = currentAnimation.id;

		if (flipX) {
			id = getFlipId(id);
		}

		return id;

	}

	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());

		sb.append("\n  Anims:");

		for (String v : fanims.keySet()) {
			sb.append(" ").append(v);
		}

		if (currentAnimation != null)
			sb.append("\n  Current Anim: ").append(currentAnimation.id);

		sb.append("\n");

		return sb.toString();
	}

	public void updateBboxFromRenderer(Polygon bbox) {
		this.bbox = bbox;
		computeBbox();
	}

	protected void computeBbox() {
		if (bbox == null)
			return;

		if (bbox.getVertices() == null || bbox.getVertices().length != 8) {
			bbox.setVertices(new float[8]);
		}

		float dx = getAlignDx(getWidth(), orgAlign);
		float dy = getAlignDy(getHeight(), orgAlign);

		float[] verts = bbox.getVertices();

		verts[0] = dx;
		verts[1] = dy;

		verts[2] = dx;
		verts[3] = getHeight() + dy;

		verts[4] = getWidth() + dx;
		verts[5] = getHeight() + dy;

		verts[6] = getWidth() + dx;
		verts[7] = dy;
		bbox.dirty();
	}

	public void addAnimation(AnimationDesc fa) {
		if (initAnimation == null)
			initAnimation = fa.id;

		fanims.put(fa.id, fa);
	}

	public HashMap<String, AnimationDesc> getAnimations() {
		return fanims;
	}

	public void setInitAnimation(String fa) {
		initAnimation = fa;
	}

	public String getInitAnimation() {
		return initAnimation;
	}

	@Override
	public int getOrgAlign() {
		return orgAlign;
	}

	@Override
	public void setOrgAlign(int align) {
		orgAlign = align;
	}

	protected AnimationDesc getAnimation(String id) {
		AnimationDesc fa = fanims.get(id);
		flipX = false;

		if (fa == null && id.indexOf('.') != -1) {
			// Search for flipped
			String flipId = getFlipId(id);

			fa = fanims.get(flipId);

			if (fa != null)
				flipX = true;
			else {
				// search for .left if .frontleft not found and viceversa
				StringBuilder sb = new StringBuilder();

				if (id.endsWith(FRONTLEFT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(LEFT);
				} else if (id.endsWith(FRONTRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(RIGHT);
				} else if (id.endsWith(BACKLEFT) || id.endsWith(BACKRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(BACK);
				} else if (id.endsWith(LEFT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FRONTLEFT);
				} else if (id.endsWith(RIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FRONTRIGHT);
				}

				String s = sb.toString();

				fa = fanims.get(s);

				if (fa == null) {
					// Search for flipped
					flipId = getFlipId(s);

					fa = fanims.get(flipId);

					if (fa != null) {
						flipX = true;
					} else if (s.endsWith(FRONT) || s.endsWith(BACK)) {
						// search only for right or left animations
						if (id.endsWith(LEFT)) {
							sb.append(id.substring(0, id.lastIndexOf('.') + 1));
							sb.append(LEFT);
						} else {
							sb.append(id.substring(0, id.lastIndexOf('.') + 1));
							sb.append(RIGHT);
						}

						s = sb.toString();
						fa = fanims.get(s);

						if (fa == null) {
							// Search for flipped
							flipId = getFlipId(s);

							fa = fanims.get(flipId);

							if (fa != null) {
								flipX = true;
							}
						}
					}
				}
			}
		}

		return fa;
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

	protected String getDirectionString(Vector2 p0, Vector2 pf, int numDirs) {

		if (numDirs == 0 || numDirs == -1)
			return null;

		float dx = pf.x - p0.x;
		float dy = pf.y - p0.y;
		float ratio = Math.abs(dx / dy);
		float ratio2 = ratio;

		if (ratio2 < 1.0)
			ratio2 = 1.0f / ratio;

		// EngineLogger.debug("P0: " + p0 + " PF: " + pf + " dx: " + dx + " dy: " + dy +
		// " RATIO: " + ratio);

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
	 * BACKLEFT, FRONTRIGHT, FRONTLEFT) 4 -> when 4 dir animation mode (RIGHT, LEFT,
	 * FRONT, BACK) 2 -> when 2 dir animation mode (RIGHT, LEFT) 0 -> when no dirs
	 * availables for the base animation -1 -> when base animation doesn't exists
	 * 
	 * @param base
	 *            Base animation
	 * @param fanims
	 * @return -1, 0, 2, 4 or 8
	 */
	protected int getDirs(String base, HashMap<String, AnimationDesc> fanims) {
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

	@Override
	public void write(Json json) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {

			json.writeValue("fanims", fanims, HashMap.class, null);
			json.writeValue("initAnimation", initAnimation);
			json.writeValue("orgAlign", orgAlign);

		} else {

			String currentAnimationId = null;

			if (currentAnimation != null)
				currentAnimationId = currentAnimation.id;

			json.writeValue("currentAnimation", currentAnimationId);

			json.writeValue("flipX", flipX);
		}
	}

	@Override
	public void read(Json json, JsonValue jsonData) {

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {

			// In next versions, the fanims loading will be generic
			// fanims = json.readValue("fanims", HashMap.class, AnimationDesc.class,
			// jsonData);

			initAnimation = json.readValue("initAnimation", String.class, jsonData);
			orgAlign = json.readValue("orgAlign", int.class, Align.bottom, jsonData);
		} else {

			String currentAnimationId = json.readValue("currentAnimation", String.class, jsonData);

			if (currentAnimationId != null)
				currentAnimation = fanims.get(currentAnimationId);
			flipX = json.readValue("flipX", Boolean.class, jsonData);
		}
	}
}

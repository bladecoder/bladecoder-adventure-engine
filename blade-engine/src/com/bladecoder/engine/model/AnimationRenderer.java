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
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

public abstract class AnimationRenderer implements ActorRenderer {
	
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
	
	public abstract void startAnimation(String id, Tween.Type repeatType,
			int count, ActionCallback cb);
	
	public abstract void startAnimation(String id, Tween.Type repeatType,
			int count, ActionCallback cb, String direction);
	
	public abstract void startAnimation(String id, Tween.Type repeatType,
			int count, ActionCallback cb, Vector2 p0, Vector2 pf);
	
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
		if((align & Align.left) != 0)
			return 0;
		else if((align & Align.right) != 0)
			return -width;
		else if((align & Align.center) != 0)
			return -width / 2.0f;
		
		return -width / 2.0f;
	}
	
	public static float getAlignDy(float height, int align) {
		if((align & Align.bottom) != 0)
			return 0;
		else if((align & Align.top) != 0)
			return -height;
		else if((align & Align.center) != 0)
			return -height / 2.0f;
		
		return 0;
	}
	
	public String getCurrentAnimationId() {
		if (currentAnimation == null)
			return null;

		String id = currentAnimation.id;

		if (flipX) {
			id = AnimationDesc.getFlipId(id);
		}

		return id;

	}

	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

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
		
		float dx =  getAlignDx(getWidth(), orgAlign);
		float dy =  getAlignDy(getHeight(), orgAlign);

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
	

	@Override
	public void write(Json json) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

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

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

			// In next versions, the fanims loading will be generic
			// fanims = json.readValue("fanims", HashMap.class, AnimationDesc.class, jsonData);
			
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


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

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.FATween;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.loader.SerializationHelper;
import com.bladecoder.engine.loader.SerializationHelper.Mode;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class AtlasRenderer implements ActorRenderer {
	private final static float DEFAULT_DIM = 200;

	private HashMap<String, AnimationDesc> fanims = new HashMap<String, AnimationDesc>();

	/** Starts this anim the first time that the scene is loaded */
	private String initAnimation;

	private AtlasAnimationDesc currentAnimation;

	private AtlasRegion tex;
	private boolean flipX;
	private FATween faTween;

	private int currentFrameIndex;

	private final HashMap<String, AtlasCacheEntry> sourceCache = new HashMap<String, AtlasCacheEntry>();
	private Polygon bbox;

	class AtlasCacheEntry {
		int refCounter;
	}

	public AtlasRenderer() {

	}

	@Override
	public void setInitAnimation(String fa) {
		initAnimation = fa;
	}

	@Override
	public String getInitAnimation() {
		return initAnimation;
	}

	@Override
	public String[] getInternalAnimations(AnimationDesc anim) {
		retrieveSource(anim.source);

		TextureAtlas atlas = EngineAssetManager.getInstance().getTextureAtlas(anim.source);

		Array<AtlasRegion> animations = atlas.getRegions();
		ArrayList<String> l = new ArrayList<String>();

		for (int i = 0; i < animations.size; i++) {
			AtlasRegion a = animations.get(i);
			if (!l.contains(a.name))
				l.add(a.name);
		}

		return l.toArray(new String[l.size()]);
	}

	@Override
	public void update(float delta) {
		if (faTween != null) {
			faTween.update(this, delta);
			if (faTween.isComplete()) {
				faTween = null;
				computeBbox();
			}
		}
	}

	public void setFrame(int i) {
		currentFrameIndex = i;
		tex = currentAnimation.regions.get(i);
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale) {

		if (tex == null) {
			x = x - getWidth() / 2 * scale;

			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight() * scale, Color.RED);
			return;
		}

		x = x + tex.offsetX - tex.originalWidth / 2;
		y = y + tex.offsetY + tex.originalHeight * (scale - 1) / 2;

		if (!flipX) {
			batch.draw(tex, x, y, tex.packedWidth / 2, tex.packedHeight / 2, tex.packedWidth, tex.packedHeight, scale,
					scale, 0);
		} else {
			batch.draw(tex, x + tex.packedWidth * scale, y, tex.packedWidth / 2, tex.packedHeight / 2, -tex.packedWidth,
					tex.packedHeight, scale, scale, 0);
		}
	}

	@Override
	public float getWidth() {
		if (tex == null)
			return DEFAULT_DIM;

		// return tex.getRegionWidth();
		return tex.originalWidth;
	}

	@Override
	public float getHeight() {
		if (tex == null)
			return DEFAULT_DIM;

		// return tex.getRegionHeight();
		return tex.originalHeight;
	}

	@Override
	public AnimationDesc getCurrentAnimation() {
		return currentAnimation;
	}

	@Override
	public HashMap<String, AnimationDesc> getAnimations() {
		return (HashMap<String, AnimationDesc>) fanims;
	}

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb) {

		if (id == null)
			id = initAnimation;

		AtlasAnimationDesc fa = getAnimation(id);

		if (fa == null) {
			EngineLogger.error("AnimationDesc not found: " + id);

			return;
		}

		if (currentAnimation != null && currentAnimation.disposeWhenPlayed) {
			disposeSource(currentAnimation.source);
			currentAnimation.regions = null;
		}

		currentAnimation = fa;

		// If the source is not loaded. Load it.
		if (currentAnimation != null && currentAnimation.regions == null) {

			retrieveFA(fa);

			if (currentAnimation.regions == null || currentAnimation.regions.size == 0) {
				EngineLogger.error(currentAnimation.id + " has no regions in ATLAS " + currentAnimation.source);
				fanims.remove(currentAnimation.id);
			}
		}

		if (currentAnimation == null) {

			tex = null;

			computeBbox();
			return;
		}

		if (currentAnimation.regions.size == 1 || currentAnimation.duration == 0.0) {

			setFrame(0);
			computeBbox();

			if (cb != null) {
				ActionCallbackQueue.add(cb);
			}

			return;
		}

		if (repeatType == Tween.Type.SPRITE_DEFINED) {
			repeatType = currentAnimation.animationType;
			count = currentAnimation.count;
		}

		faTween = new FATween();
		faTween.start(this, repeatType, count, currentAnimation.duration, cb);

		if (repeatType == Tween.Type.REVERSE)
			setFrame(getNumFrames() - 1);
		else
			setFrame(0);

		computeBbox();
	}

	public int getNumFrames() {
		return currentAnimation.regions.size;
	}

	@Override
	public String getCurrentAnimationId() {
		if (currentAnimation == null)
			return null;

		String id = currentAnimation.id;

		if (flipX) {
			id = AnimationDesc.getFlipId(id);
		}

		return id;

	}

	private void computeBbox() {
		if (bbox == null)
			return;

		if (bbox.getVertices() == null || bbox.getVertices().length != 8) {
			bbox.setVertices(new float[8]);
		}

		float[] verts = bbox.getVertices();

		verts[0] = -getWidth() / 2;
		verts[1] = 0f;
		verts[2] = -getWidth() / 2;
		verts[3] = getHeight();
		verts[4] = getWidth() / 2;
		verts[5] = getHeight();
		verts[6] = getWidth() / 2;
		verts[7] = 0f;
		bbox.dirty();
	}

	@Override
	public void addAnimation(AnimationDesc fa) {
		if (initAnimation == null)
			initAnimation = fa.id;

		fanims.put(fa.id, (AtlasAnimationDesc) fa);
	}

	@Override
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

	private AtlasAnimationDesc getAnimation(String id) {
		AnimationDesc fa = fanims.get(id);
		flipX = false;

		if (fa == null) {
			// Search for flipped
			String flipId = AnimationDesc.getFlipId(id);

			fa = fanims.get(flipId);

			if (fa != null)
				flipX = true;
			else {
				// search for .left if .frontleft not found and viceversa
				StringBuilder sb = new StringBuilder();

				if (id.endsWith(AnimationDesc.FRONTLEFT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(AnimationDesc.LEFT);
				} else if (id.endsWith(AnimationDesc.FRONTRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(AnimationDesc.RIGHT);
				} else if (id.endsWith(AnimationDesc.BACKLEFT) || id.endsWith(AnimationDesc.BACKRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(AnimationDesc.BACK);
				} else if (id.endsWith(AnimationDesc.LEFT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(AnimationDesc.FRONTLEFT);
				} else if (id.endsWith(AnimationDesc.RIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(AnimationDesc.FRONTRIGHT);
				}

				String s = sb.toString();

				fa = fanims.get(s);

				if (fa == null) {
					// Search for flipped
					flipId = AnimationDesc.getFlipId(s);

					fa = fanims.get(flipId);

					if (fa != null)
						flipX = true;
				}
			}
		}

		return (AtlasAnimationDesc) fa;
	}

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, String direction) {
		StringBuilder sb = new StringBuilder(id);

		// if dir==null gets the current animation direction
		if (direction == null) {
			int idx = getCurrentAnimationId().indexOf('.');

			if (idx != -1) {
				String dir = getCurrentAnimationId().substring(idx);
				sb.append(dir);
			}
		} else {
			sb.append('.');
			sb.append(direction);
		}

		String anim = sb.toString();

		if (getAnimation(anim) == null) {
			anim = id;
		}

		startAnimation(anim, repeatType, count, null);
	}

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, Vector2 p0, Vector2 pf) {
		startAnimation(id, repeatType, count, cb, AnimationDesc.getDirectionString(p0, pf));
	}

	@Override
	public void updateBboxFromRenderer(Polygon bbox) {
		this.bbox = bbox;
	}

	private void loadSource(String source) {
		AtlasCacheEntry entry = sourceCache.get(source);

		if (entry == null) {
			entry = new AtlasCacheEntry();
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0)
			EngineAssetManager.getInstance().loadAtlas(source);

		entry.refCounter++;
	}

	private void retrieveFA(AtlasAnimationDesc fa) {
		retrieveSource(fa.source);
		fa.regions = EngineAssetManager.getInstance().getRegions(fa.source, fa.id);
	}

	private void retrieveSource(String source) {
		AtlasCacheEntry entry = sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
		}
	}

	private void disposeSource(String source) {
		AtlasCacheEntry entry = sourceCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeAtlas(source);
		}

		entry.refCounter--;
	}

	@Override
	public void loadAssets() {
		for (AnimationDesc fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source);
		}

		if (currentAnimation != null && !currentAnimation.preload) {
			loadSource(currentAnimation.source);
		} else if (currentAnimation == null && initAnimation != null) {
			String a = initAnimation;

			if (flipX) {
				a = AnimationDesc.getFlipId(a);
			}

			AtlasAnimationDesc fa = (AtlasAnimationDesc) fanims.get(a);

			if (fa != null && !fa.preload)
				loadSource(fa.source);
		}
	}

	@Override
	public void retrieveAssets() {
		for (AnimationDesc fa : fanims.values()) {
			if (fa.preload)
				retrieveFA((AtlasAnimationDesc) fa);
		}

		if (currentAnimation != null && !currentAnimation.preload) {
			retrieveFA(currentAnimation);
		} else if (currentAnimation == null && initAnimation != null) {
			String a = initAnimation;

			if (flipX) {
				a = AnimationDesc.getFlipId(a);
			}

			AtlasAnimationDesc fa = (AtlasAnimationDesc) fanims.get(a);

			if (fa != null && !fa.preload)
				retrieveFA(fa);
		}

		if (currentAnimation != null) {
			setFrame(currentFrameIndex);
		} else if (initAnimation != null) {
			startAnimation(initAnimation, Tween.Type.SPRITE_DEFINED, 1, null);
		}

		computeBbox();
	}

	@Override
	public void dispose() {
		for (String key : sourceCache.keySet()) {
			EngineAssetManager.getInstance().disposeAtlas(key);
		}

		sourceCache.clear();
	}

	@Override
	public void write(Json json) {
		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

			json.writeValue("fanims", fanims, HashMap.class, AtlasAnimationDesc.class);
			json.writeValue("initAnimation", initAnimation);

		} else {

			String currentAnimationId = null;

			if (currentAnimation != null)
				currentAnimationId = currentAnimation.id;

			json.writeValue("currentAnimation", currentAnimationId);

			json.writeValue("flipX", flipX);
			json.writeValue("currentFrameIndex", currentFrameIndex);

			json.writeValue("faTween", faTween, faTween == null ? null : FATween.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

			fanims = json.readValue("fanims", HashMap.class, AtlasAnimationDesc.class, jsonData);
			initAnimation = json.readValue("initAnimation", String.class, jsonData);

		} else {

			String currentAnimationId = json.readValue("currentAnimation", String.class, jsonData);

			if (currentAnimationId != null)
				currentAnimation = (AtlasAnimationDesc) fanims.get(currentAnimationId);

			flipX = json.readValue("flipX", Boolean.class, jsonData);
			currentFrameIndex = json.readValue("currentFrameIndex", Integer.class, jsonData);
			faTween = json.readValue("faTween", FATween.class, jsonData);
		}
	}
}
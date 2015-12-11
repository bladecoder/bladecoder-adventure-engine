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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.loader.SerializationHelper;
import com.bladecoder.engine.loader.SerializationHelper.Mode;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class ImageRenderer implements ActorRenderer {

	private HashMap<String, AnimationDesc> fanims = new HashMap<String, AnimationDesc>();

	/** Starts this anim the first time that the scene is loaded */
	private String initAnimation;

	private AnimationDesc currentAnimation;

	private ImageCacheEntry currentSource;
	private boolean flipX;

	private final HashMap<String, ImageCacheEntry> sourceCache = new HashMap<String, ImageCacheEntry>();
	private Polygon bbox;

	class ImageCacheEntry {
		int refCounter;

		Texture tex;
	}

	public ImageRenderer() {

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
		return new String[] { anim.source.substring(0, anim.source.lastIndexOf('.')) };
	}

	@Override
	public void update(float delta) {
	}

	@Override
	public void updateBboxFromRenderer(Polygon bbox) {
		this.bbox = bbox;
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
	public void draw(SpriteBatch batch, float x, float y, float scale) {

		x = x - getWidth() / 2 * scale; // SET THE X ORIGIN TO THE CENTER OF THE
										// SPRITE

		if (currentSource == null || currentSource.tex == null) {
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight() * scale, Color.RED);
			return;
		}

		if (!flipX) {
			batch.draw(currentSource.tex, x, y, currentSource.tex.getWidth() * scale,
					currentSource.tex.getHeight() * scale);
		} else {
			batch.draw(currentSource.tex, x, y, -currentSource.tex.getWidth() * scale,
					currentSource.tex.getHeight() * scale);
		}
	}

	@Override
	public float getWidth() {
		if (currentSource == null || currentSource.tex == null)
			return 200;

		return currentSource.tex.getWidth();
	}

	@Override
	public float getHeight() {
		if (currentSource == null || currentSource.tex == null)
			return 200;

		return currentSource.tex.getHeight();
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
		AnimationDesc fa = getAnimation(id);

		if (fa == null) {
			EngineLogger.error("AnimationDesc not found: " + id);

			return;
		}

		if (cb != null)
			ActionCallbackQueue.add(cb);

		if (currentAnimation != null && currentAnimation.disposeWhenPlayed)
			disposeSource(currentAnimation.source);

		currentAnimation = fa;
		currentSource = sourceCache.get(fa.source);

		// If the source is not loaded. Load it.
		if (currentSource == null || currentSource.refCounter < 1) {
			loadSource(fa.source);
			EngineAssetManager.getInstance().finishLoading();

			retrieveSource(fa.source);

			currentSource = sourceCache.get(fa.source);

			if (currentSource == null) {
				EngineLogger.error("Could not load AnimationDesc: " + id);
				currentAnimation = null;

				computeBbox();
				return;
			}
		}

		computeBbox();
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

	@Override
	public void addAnimation(AnimationDesc fa) {
		if (initAnimation == null)
			initAnimation = fa.id;

		fanims.put(fa.id, fa);
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

	private AnimationDesc getAnimation(String id) {
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

		return fa;
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
		startAnimation(id, repeatType, count, cb, AnimationDesc.getDirectionString(p0, pf, AnimationDesc.getDirs(id, fanims)));
	}

	private void loadSource(String source) {
		ImageCacheEntry entry = sourceCache.get(source);

		if (entry == null) {
			entry = new ImageCacheEntry();
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0) {
			// I18N for images
			if (source.charAt(0) == I18N.PREFIX)
				source = I18N.getString(source.substring(1));
			EngineAssetManager.getInstance().loadTexture(EngineAssetManager.IMAGE_DIR + source);
		}

		entry.refCounter++;
	}

	private void retrieveSource(String source) {
		ImageCacheEntry entry = sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
			entry = sourceCache.get(source);
		}

		if (entry.tex == null) {
			// I18N for images
			if (source.charAt(0) == I18N.PREFIX)
				source = I18N.getString(source.substring(1));

			entry.tex = EngineAssetManager.getInstance().getTexture(EngineAssetManager.IMAGE_DIR + source);
		}
	}

	private void disposeSource(String source) {
		ImageCacheEntry entry = sourceCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeTexture(entry.tex);
			entry.tex = null;
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
			AnimationDesc fa = fanims.get(initAnimation);

			if (!fa.preload)
				loadSource(fa.source);
		}
	}

	@Override
	public void retrieveAssets() {

		for (String key : sourceCache.keySet()) {
			if (sourceCache.get(key).refCounter > 0)
				retrieveSource(key);
		}

		if (currentAnimation != null) {
			ImageCacheEntry entry = sourceCache.get(currentAnimation.source);
			currentSource = entry;
		} else if (initAnimation != null) {
			startAnimation(initAnimation, Tween.Type.SPRITE_DEFINED, 1, null);
		}

		computeBbox();
	}

	@Override
	public void dispose() {
		for (ImageCacheEntry entry : sourceCache.values()) {
			EngineAssetManager.getInstance().disposeTexture(entry.tex);
		}

		sourceCache.clear();
		currentSource = null;
	}

	@Override
	public void write(Json json) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

			json.writeValue("fanims", fanims, HashMap.class, AnimationDesc.class);
			json.writeValue("initAnimation", initAnimation);

		} else {

			String currentAnimationId = null;

			if (currentAnimation != null)
				currentAnimationId = currentAnimation.id;

			json.writeValue("currentAnimation", currentAnimationId);

			json.writeValue("flipX", flipX);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			fanims = json.readValue("fanims", HashMap.class, AnimationDesc.class, jsonData);
			initAnimation = json.readValue("initAnimation", String.class, jsonData);

		} else {

			String currentAnimationId = json.readValue("currentAnimation", String.class, jsonData);

			if (currentAnimationId != null)
				currentAnimation = fanims.get(currentAnimationId);
			flipX = json.readValue("flipX", Boolean.class, jsonData);
		}
	}

}
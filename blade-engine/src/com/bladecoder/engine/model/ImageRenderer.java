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

import com.bladecoder.engine.model.SpriteRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.FrameAnimation;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class ImageRenderer implements SpriteRenderer {

	private HashMap<String, FrameAnimation> fanims = new HashMap<String, FrameAnimation>();
	
	/** Starts this anim the first time that the scene is loaded */
	private String initFrameAnimation;

	private FrameAnimation currentFrameAnimation;

	private ImageCacheEntry currentSource;
	private boolean flipX;
	
	private final HashMap<String, ImageCacheEntry> sourceCache = new HashMap<String, ImageCacheEntry>();

	class ImageCacheEntry {
		int refCounter;
		
		Texture tex;
	}	
	
	public ImageRenderer() {
		
	}

	@Override
	public void setInitFrameAnimation(String fa) {
		initFrameAnimation = fa;
	}
	
	@Override
	public String getInitFrameAnimation() {
		return initFrameAnimation;
	}
	
	@Override
	public String[] getInternalAnimations(String source) {
		return new String[]{source.substring(0,source.lastIndexOf('.'))};
	}	


	@Override
	public void update(float delta) {
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale) {
		
		x = x - getWidth() / 2 * scale; // SET THE X ORIGIN TO THE CENTER OF THE SPRITE

		if (currentSource == null || currentSource.tex == null) {
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight()
					* scale, Color.RED);
			return;
		}

		if (!flipX) {
			batch.draw(currentSource.tex, x, y, currentSource.tex.getWidth() *  scale,
					currentSource.tex.getHeight() * scale);
		} else {
			batch.draw(currentSource.tex, x, y, -currentSource.tex.getWidth() *  scale,
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
	public FrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	@Override
	public HashMap<String, FrameAnimation> getFrameAnimations() {
		return (HashMap<String, FrameAnimation>)fanims;
	}

	@Override
	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		FrameAnimation fa = getFrameAnimation(id);
		
		if(cb != null)
			ActionCallbackQueue.add(cb);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}

		if (currentFrameAnimation != null
				&& currentFrameAnimation.disposeWhenPlayed)
			disposeSource(currentFrameAnimation.source);

		currentFrameAnimation = fa;
		currentSource = sourceCache.get(fa.source);

		// If the source is not loaded. Load it.
		if (currentSource == null || currentSource.refCounter < 1) {
			loadSource(fa.source);
			EngineAssetManager.getInstance().finishLoading();

			retrieveSource(fa.source);

			currentSource = sourceCache.get(fa.source);

			if (currentSource == null) {
				EngineLogger.error("Could not load FrameAnimation: " + id);
				currentFrameAnimation = null;

				return;
			}
		}
	}

	@Override
	public String getCurrentFrameAnimationId() {
		if (currentFrameAnimation == null)
			return null;

		String id = currentFrameAnimation.id;

		if (flipX) {
			id = FrameAnimation.getFlipId(id);
		}

		return id;

	}

	@Override
	public void addFrameAnimation(FrameAnimation fa) {
		if(initFrameAnimation == null)
			initFrameAnimation = fa.id; 
			
		fanims.put(fa.id, fa);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("\n  Anims:");

		for (String v : fanims.keySet()) {
			sb.append(" ").append(v);
		}

		sb.append("\n  Current Anim: ").append(currentFrameAnimation.id);

		sb.append("\n");

		return sb.toString();
	}

	private FrameAnimation getFrameAnimation(String id) {
		FrameAnimation fa = fanims.get(id);
		flipX = false;

		if (fa == null) {
			// Search for flipped
			String flipId = FrameAnimation.getFlipId(id);

			fa = fanims.get(flipId);

			if (fa != null)
				flipX = true;
			else {
				// search for .left if .frontleft not found and viceversa
				StringBuilder sb = new StringBuilder();
				
				if (id.endsWith(FrameAnimation.FRONTLEFT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FrameAnimation.LEFT);
				} else if (id.endsWith(FrameAnimation.FRONTRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FrameAnimation.RIGHT);
				} else if (id.endsWith(FrameAnimation.BACKLEFT) || id.endsWith(FrameAnimation.BACKRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FrameAnimation.BACK);
				} else if (id.endsWith(FrameAnimation.LEFT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FrameAnimation.FRONTLEFT);
				} else if (id.endsWith(FrameAnimation.RIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FrameAnimation.FRONTRIGHT);			
				}

				String s = sb.toString();

				fa = fanims.get(s);

				if (fa == null) {
					// Search for flipped
					flipId = FrameAnimation.getFlipId(s);

					fa = fanims.get(flipId);

					if (fa != null)
						flipX = true;
				}
			}
		}

		return fa;
	}

	@Override
	public void lookat(float x, float y, Vector2 pf) {
		lookat(FrameAnimation.getFrameDirection(x, y, pf));
	}

	@Override
	public void lookat(String dir) {
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.STAND_ANIM);
		sb.append('.');
		sb.append(dir);

		startFrameAnimation(sb.toString(), Tween.FROM_FA, 1, null);
	}

	@Override
	public void stand() {
		String standFA = FrameAnimation.STAND_ANIM;
		int idx = getCurrentFrameAnimationId().indexOf('.');

		if (idx != -1) {
			standFA += getCurrentFrameAnimationId().substring(idx);
		}

		startFrameAnimation(standFA, Tween.FROM_FA, 1, null);
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		String currentDirection = FrameAnimation.getFrameDirection(p0.x, p0.y, pf);
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.WALK_ANIM).append('.').append(currentDirection);
		startFrameAnimation(sb.toString(), Tween.FROM_FA, 1, null);
	}
	
	private void loadSource(String source) {
		ImageCacheEntry entry = sourceCache.get(source);

		if (entry == null) {
			entry = new ImageCacheEntry();
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0) {
			// I18N for images
			if(source.charAt(0) == '@')
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
			if(source.charAt(0) == '@')
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
		for (FrameAnimation fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source);
		}

		if (currentFrameAnimation != null && !currentFrameAnimation.preload) {
			loadSource(currentFrameAnimation.source);
		} else if (currentFrameAnimation == null && initFrameAnimation != null) {
			FrameAnimation fa = fanims.get(initFrameAnimation);

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

		if (currentFrameAnimation != null) {
			ImageCacheEntry entry = sourceCache
					.get(currentFrameAnimation.source);
			currentSource = entry;
		} else if (initFrameAnimation != null) {
			startFrameAnimation(initFrameAnimation, Tween.FROM_FA, 1, null);
		}
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

		json.writeValue("fanims", fanims, HashMap.class, FrameAnimation.class);

		String currentFrameAnimationId = null;

		if (currentFrameAnimation != null)
			currentFrameAnimationId = currentFrameAnimation.id;

		json.writeValue("currentFrameAnimation", currentFrameAnimationId);
		
		json.writeValue("initFrameAnimation", initFrameAnimation);

		json.writeValue("flipX", flipX);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		fanims = json.readValue("fanims", HashMap.class,
				FrameAnimation.class, jsonData);

		String currentFrameAnimationId = json.readValue(
				"currentFrameAnimation", String.class, jsonData);

		if (currentFrameAnimationId != null)
			currentFrameAnimation = fanims.get(currentFrameAnimationId);
		
		initFrameAnimation = json.readValue("initFrameAnimation", String.class,
				jsonData);

		flipX = json.readValue("flipX", Boolean.class, jsonData);
	}

}
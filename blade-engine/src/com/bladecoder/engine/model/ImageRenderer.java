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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

public class ImageRenderer extends AnimationRenderer {

	class ImageCacheEntry extends CacheEntry {
		Texture tex;
	}

	public ImageRenderer() {

	}

	@Override
	public String[] getInternalAnimations(AnimationDesc anim) {
		return new String[] { anim.source.substring(0, anim.source.lastIndexOf('.')) };
	}

	@Override
	public void update(float delta) {
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale, float rotation, Color tint) {
		
		float dx = getAlignDx(getWidth(), orgAlign);
		float dy = getAlignDy(getHeight(), orgAlign);
		
		ImageCacheEntry source = (ImageCacheEntry)currentSource;

		if (source == null || source.tex == null) {			
			RectangleRenderer.draw(batch, x + dx * scale , y + dy * scale, getWidth() * scale, getHeight() * scale, Color.RED);
			return;
		}
		
		if(tint != null)
			batch.setColor(tint);
		
		x = x + dx;
		y = y + dy;
		
		batch.draw(source.tex, x, y, -dx, -dy, getWidth(), getHeight(), scale, scale, rotation, 
				0, 0, source.tex.getWidth(), source.tex.getHeight(), flipX, false);
		
		if(tint != null)
			batch.setColor(Color.WHITE);
	}

	@Override
	public float getWidth() {
		ImageCacheEntry source = (ImageCacheEntry)currentSource;
		
		if (source == null || source.tex == null)
			return super.getWidth();

		return source.tex.getWidth();
	}

	@Override
	public float getHeight() {
		ImageCacheEntry source = (ImageCacheEntry)currentSource;
		
		if (source == null || source.tex == null)
			return super.getHeight();

		return source.tex.getHeight();
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
		currentSource = (ImageCacheEntry)sourceCache.get(fa.source);

		// If the source is not loaded. Load it.
		if (currentSource == null || currentSource.refCounter < 1) {
			loadSource(fa.source);
			EngineAssetManager.getInstance().finishLoading();

			retrieveSource(fa.source);

			currentSource = (ImageCacheEntry)sourceCache.get(fa.source);

			if (currentSource == null) {
				EngineLogger.error("Could not load AnimationDesc: " + id);
				currentAnimation = null;

				computeBbox();
				return;
			}
		}

		computeBbox();
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
		CacheEntry entry = sourceCache.get(source);

		if (entry == null) {
			entry = new ImageCacheEntry();
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0) {
			// I18N for images
			if (source.charAt(0) == I18N.PREFIX) {
				source = getI18NSource(source.substring(1));
			}
			
			EngineAssetManager.getInstance().loadTexture(EngineAssetManager.IMAGE_DIR + source);
		}

		entry.refCounter++;
	}

	private void retrieveSource(String source) {
		CacheEntry entry = sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
			entry = sourceCache.get(source);
		}

		if (((ImageCacheEntry)entry).tex == null) {
			// I18N for images
			if (source.charAt(0) == I18N.PREFIX) {
				source = getI18NSource(source.substring(1));
			}

			((ImageCacheEntry)entry).tex = EngineAssetManager.getInstance().getTexture(EngineAssetManager.IMAGE_DIR + source);
		}
	}
	
	private String getI18NSource(String source) {
		String lang = I18N.getCurrentLocale().getLanguage();
		
		int pointIdx = source.lastIndexOf('.');
		String ext = source.substring(pointIdx);
		String name = source.substring(0, pointIdx);
		
		String localName = name + "_" + lang + ext;
		
		if(EngineAssetManager.getInstance().assetExists(EngineAssetManager.IMAGE_DIR + localName))
			return localName;
		
		return source;
	}

	private void disposeSource(String source) {
		ImageCacheEntry entry = (ImageCacheEntry)sourceCache.get(source);

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

			if (fa != null && !fa.preload)
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
			CacheEntry entry = sourceCache.get(currentAnimation.source);
			currentSource = entry;
		} else if (initAnimation != null) {
			startAnimation(initAnimation, Tween.Type.SPRITE_DEFINED, 1, null);
		}

		computeBbox();
	}

	@Override
	public void dispose() {
		for (CacheEntry entry : sourceCache.values()) {
			if(entry.refCounter > 0)
				EngineAssetManager.getInstance().disposeTexture(((ImageCacheEntry)entry).tex);
		}

		sourceCache.clear();
		currentSource = null;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			fanims = json.readValue("fanims", HashMap.class, AnimationDesc.class, jsonData);
		} else {

		}
	}
}
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
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

public class AtlasRenderer extends AnimationRenderer {

	private AtlasRegion tex;
	private FATween faTween;

	private int currentFrameIndex;

	public AtlasRenderer() {

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
			faTween.update(delta);
			if (faTween.isComplete()) {
				faTween = null;
				computeBbox();
			}
		}
	}

	public void setFrame(int i) {
		currentFrameIndex = i;
		tex = ((AtlasAnimationDesc) currentAnimation).regions.get(i);
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale, float rotation, Color tint) {

		float dx = getAlignDx(getWidth(), orgAlign);
		float dy = getAlignDy(getHeight(), orgAlign);
		
		if (tex == null) {			
			RectangleRenderer.draw(batch, x + dx * scale , y + dy * scale, getWidth() * scale, getHeight() * scale, Color.RED);
			
			return;
		}



		x = x + tex.offsetX + dx;
		y = y + tex.offsetY + dy;

		if (tint != null)
			batch.setColor(tint);

		batch.draw(tex, x, y, -dx - tex.offsetX, -dy - tex.offsetY, tex.packedWidth, tex.packedHeight,
				flipX ? -scale : scale, scale, rotation);

		if (tint != null)
			batch.setColor(Color.WHITE);
	}

	@Override
	public float getWidth() {
		if (tex == null)
			return super.getWidth();

		return tex.originalWidth;
	}

	@Override
	public float getHeight() {
		if (tex == null)
			return super.getHeight();

		return tex.originalHeight;
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
			((AtlasAnimationDesc) currentAnimation).regions = null;
		}

		currentAnimation = fa;

		// If the source is not loaded. Load it.
		if (fa.regions == null) {

			retrieveFA(fa);

			if (fa.regions == null || fa.regions.size == 0) {
				EngineLogger.error(currentAnimation.id + " has no regions in ATLAS " + currentAnimation.source);
				fanims.remove(currentAnimation.id);
			}
		}

		if (fa.regions.size == 1 || fa.duration == 0.0) {

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
		return ((AtlasAnimationDesc) currentAnimation).regions.size;
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
		startAnimation(id, repeatType, count, cb,
				AnimationDesc.getDirectionString(p0, pf, AnimationDesc.getDirs(id, fanims)));
	}

	private void loadSource(String source) {
		CacheEntry entry = sourceCache.get(source);

		if (entry == null) {
			entry = new CacheEntry();
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
		CacheEntry entry = sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
		}
	}

	private void disposeSource(String source) {
		CacheEntry entry = sourceCache.get(source);

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

			AnimationDesc fa = fanims.get(a);

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
			retrieveFA((AtlasAnimationDesc) currentAnimation);
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
			if (sourceCache.get(key).refCounter > 0)
				EngineAssetManager.getInstance().disposeAtlas(key);
		}

		sourceCache.clear();
	}

	@Override
	public void write(Json json) {
		super.write(json);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

		} else {
			json.writeValue("currentFrameIndex", currentFrameIndex);

			if(faTween != null)
				json.writeValue("faTween", faTween);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			fanims = json.readValue("fanims", HashMap.class, AtlasAnimationDesc.class, jsonData);
		} else {

			currentFrameIndex = json.readValue("currentFrameIndex", Integer.class, jsonData);
			faTween = json.readValue("faTween", FATween.class, jsonData);

			if (faTween != null)
				faTween.setTarget(this);
		}
	}
}
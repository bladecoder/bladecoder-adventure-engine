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

import com.bladecoder.engine.model.ActorRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.FATween;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;

public class AtlasRenderer implements ActorRenderer {

	private HashMap<String, AnimationDesc> fanims = new HashMap<String, AnimationDesc>();
	
	/** Starts this anim the first time that the scene is loaded */
	private String initAnimation;

	private AtlasAnimationDesc currentAnimation;

	private TextureRegion tex;
	private boolean flipX;
	private FATween faTween;
	
	private int currentFrameIndex;
	
	private final HashMap<String, AtlasCacheEntry> sourceCache = new HashMap<String, AtlasCacheEntry>();

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
	public String[] getInternalAnimations(String source) {
		retrieveSource(source);
		
		TextureAtlas atlas = EngineAssetManager.getInstance().getTextureAtlas(source);
		
		Array<AtlasRegion> animations = atlas.getRegions();
		ArrayList<String> l = new ArrayList<String>();
		
		for(int i = 0; i< animations.size; i++) {
			AtlasRegion a = animations.get(i);
			if(!l.contains(a.name))
				l.add(a.name);
		}
		
		
		return l.toArray(new String[l.size()]);
	}	


	@Override
	public void update(float delta) {
		if(faTween != null) {
			faTween.update(this, delta);
			if(faTween.isComplete()) {
				faTween = null;
			}
		}
	}
	
	public void setFrame(int i) {
		currentFrameIndex = i;
		tex =  currentAnimation.regions.get(i);
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale) {
		
		x = x - getWidth() / 2 * scale; // SET THE X ORIGIN TO THE CENTER OF THE SPRITE

		if (tex == null) {
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight()
					* scale, Color.RED);
			return;
		}

		if (!flipX) {
			batch.draw(tex, x, y, 0, 0, tex.getRegionWidth(),
					tex.getRegionHeight(), scale, scale, 0);
		} else {
			batch.draw(tex, x + tex.getRegionWidth() * scale, y, 0,
					0, -tex.getRegionWidth(), tex.getRegionHeight(),
					scale, scale, 0);
		}
	}

	@Override
	public float getWidth() {
		if (tex == null)
			return 200;

		return tex.getRegionWidth();
	}

	@Override
	public float getHeight() {
		if (tex == null)
			return 200;

		return tex.getRegionHeight();
	}

	@Override
	public AnimationDesc getCurrentAnimation() {
		return currentAnimation;
	}

	@Override
	public HashMap<String, AnimationDesc> getAnimations() {
		return (HashMap<String, AnimationDesc>)fanims;
	}

	@Override
	public void startAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		
		if(id == null)
			id = initAnimation;
		
		AtlasAnimationDesc fa = getAnimation(id);

		if (fa == null) {
			EngineLogger.error("AnimationDesc not found: " + id);

			return;
		}
		
		if(currentAnimation != null && currentAnimation.disposeWhenPlayed) {
			disposeSource(currentAnimation.source);
			currentAnimation.regions = null;
		}

		currentAnimation = fa;

		// If the source is not loaded. Load it.
		if (currentAnimation != null
				&& currentAnimation.regions == null) {

			retrieveFA(fa);

			if (currentAnimation.regions == null || currentAnimation.regions.size == 0) {
				EngineLogger.error(currentAnimation.id + " has no regions in ATLAS " + currentAnimation.source);
				fanims.remove(currentAnimation.id);
			}
		}

		if (currentAnimation == null) {

			tex = null;

			return;
		}

		if (currentAnimation.regions.size == 1
				|| currentAnimation.duration == 0.0) {

			setFrame(0);

			if (cb != null) {
				ActionCallbackQueue.add(cb);
			}

			return;
		}

		if (repeatType == Tween.FROM_FA) {
			repeatType = currentAnimation.animationType;
			count = currentAnimation.count;
		}
		
		faTween = new FATween();
		faTween.start(this, repeatType, count, currentAnimation.duration, cb);
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

	@Override
	public void addAnimation(AnimationDesc fa) {
		if(initAnimation == null)
			initAnimation = fa.id; 
			
		fanims.put(fa.id, (AtlasAnimationDesc)fa);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("\n  Anims:");

		for (String v : fanims.keySet()) {
			sb.append(" ").append(v);
		}

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

		return (AtlasAnimationDesc)fa;
	}

	@Override
	public void lookat(float x, float y, Vector2 pf) {
		lookat(AnimationDesc.getFrameDirection(x, y, pf));
	}

	@Override
	public void lookat(String dir) {
		StringBuilder sb = new StringBuilder();
		sb.append(AnimationDesc.STAND_ANIM);
		sb.append('.');
		sb.append(dir);

		startAnimation(sb.toString(), Tween.FROM_FA, 1, null);
	}

	@Override
	public void stand() {
		String standFA = AnimationDesc.STAND_ANIM;
		int idx = getCurrentAnimationId().indexOf('.');

		if (idx != -1) {
			standFA += getCurrentAnimationId().substring(idx);
		}

		startAnimation(standFA, Tween.FROM_FA, 1, null);
	}

	@Override
	public void walk(Vector2 p0, Vector2 pf) {
		String currentDirection = AnimationDesc.getFrameDirection(p0.x, p0.y, pf);
		StringBuilder sb = new StringBuilder();
		sb.append(AnimationDesc.WALK_ANIM).append('.').append(currentDirection);
		startAnimation(sb.toString(), Tween.FROM_FA, 1, null);
	}
	
	private void loadSource(String source) {
		AtlasCacheEntry entry = sourceCache.get(source);
		
		if(entry == null) {
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
		
		if(entry==null || entry.refCounter < 1) {
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
			AnimationDesc fa = fanims.get(initAnimation);

			if (!fa.preload)
				loadSource(fa.source);
		}
	}

	@Override
	public void retrieveAssets() {
		for (AnimationDesc fa : fanims.values()) {
			if(fa.preload)
				retrieveFA((AtlasAnimationDesc)fa);
		}
		
		if(currentAnimation != null && !currentAnimation.preload) {
			retrieveFA(currentAnimation);
		} else if(currentAnimation == null && initAnimation != null) {
			AtlasAnimationDesc fa = (AtlasAnimationDesc)fanims.get(initAnimation);
			
			if(!fa.preload)
				retrieveFA(fa);		
		}

		if (currentAnimation != null) {		
			setFrame(currentFrameIndex);
		} else if(initAnimation != null){
			startAnimation(initAnimation, Tween.FROM_FA, 1, null);
		}
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

		json.writeValue("fanims", fanims, HashMap.class, AtlasAnimationDesc.class);

		String currentAnimationId = null;

		if (currentAnimation != null)
			currentAnimationId = currentAnimation.id;

		json.writeValue("currentAnimation", currentAnimationId);
		
		json.writeValue("initAnimation", initAnimation);

		json.writeValue("flipX", flipX);
		json.writeValue("currentFrameIndex", currentFrameIndex);
		
		json.writeValue("faTween", faTween,
				faTween == null ? null : FATween.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		fanims = json.readValue("fanims", HashMap.class,
				AtlasAnimationDesc.class, jsonData);

		String currentAnimationId = json.readValue(
				"currentAnimation", String.class, jsonData);

		if (currentAnimationId != null)
			currentAnimation = (AtlasAnimationDesc)fanims.get(currentAnimationId);
		
		initAnimation = json.readValue("initAnimation", String.class,
				jsonData);

		flipX = json.readValue("flipX", Boolean.class, jsonData);
		currentFrameIndex = json.readValue("currentFrameIndex", Integer.class, jsonData);
		faTween =  json.readValue("faTween", FATween.class, jsonData);
	}

}
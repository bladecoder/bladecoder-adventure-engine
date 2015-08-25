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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("atlas")
public class AtlasRenderer implements ActorRenderer {
	private final static float DEFAULT_DIM = 200;

	private HashMap<String, AnimationDesc> fanims = new HashMap<String, AnimationDesc>();
	
	/** Starts this anim the first time that the scene is loaded */
	private String initAnimation;

	private AtlasAnimationDesc currentAnimation;

	private TextureRegion tex;
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
		retrieveSource(anim.getSource());
		
		TextureAtlas atlas = EngineAssetManager.getInstance().getTextureAtlas(anim.getSource());
		
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
				computeBbox();
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
			return DEFAULT_DIM;

		return tex.getRegionWidth();
	}

	@Override
	public float getHeight() {
		if (tex == null)
			return DEFAULT_DIM;

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
	public void startAnimation(String id, Tween.Type repeatType, int count,
			ActionCallback cb) {
		
		if(id == null)
			id = initAnimation;
		
		AtlasAnimationDesc fa = getAnimation(id);

		if (fa == null) {
			EngineLogger.error("AnimationDesc not found: " + id);

			return;
		}
		
		if(currentAnimation != null && currentAnimation.isDisposeWhenPlayed()) {
			disposeSource(currentAnimation.getSource());
			currentAnimation.regions = null;
		}

		currentAnimation = fa;

		// If the source is not loaded. Load it.
		if (currentAnimation != null
				&& currentAnimation.regions == null) {

			retrieveFA(fa);

			if (currentAnimation.regions == null || currentAnimation.regions.size == 0) {
				EngineLogger.error(currentAnimation.getId() + " has no regions in ATLAS " + currentAnimation.getSource());
				fanims.remove(currentAnimation.getId());
			}
		}

		if (currentAnimation == null) {

			tex = null;

			computeBbox();
			return;
		}

		if (currentAnimation.regions.size == 1
				|| currentAnimation.getSpeed() == 0.0) {

			setFrame(0);
			computeBbox();

			if (cb != null) {
				ActionCallbackQueue.add(cb);
			}

			return;
		}

		if (repeatType == Tween.Type.SPRITE_DEFINED) {
			repeatType = currentAnimation.getAnimationType();
			count = currentAnimation.getCount();
		}
		
		faTween = new FATween();
		faTween.start(this, repeatType, count, currentAnimation.getSpeed(), cb);
		
		if(repeatType == Tween.Type.REVERSE)
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

		String id = currentAnimation.getId();

		if (flipX) {
			id = AnimationDesc.getFlipId(id);
		}

		return id;

	}
	
	private void computeBbox() {
		if(bbox == null)
			return;
		
		if(bbox.getVertices() == null || bbox.getVertices().length != 8) {
			bbox.setVertices(new float[8]);
		}
		
		float[] verts = bbox.getVertices();
		
		verts[0] = -getWidth()/2;
		verts[1] = 0f;
		verts[2] = -getWidth()/2;
		verts[3] = getHeight();
		verts[4] = getWidth()/2;
		verts[5] = getHeight();
		verts[6] = getWidth()/2;
		verts[7] = 0f;		
		bbox.dirty();
	}

	@Override
	public void addAnimation(AnimationDesc fa) {
		if(initAnimation == null)
			initAnimation = fa.getId();
			
		fanims.put(fa.getId(), (AtlasAnimationDesc)fa);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("\n  Anims:");

		for (String v : fanims.keySet()) {
			sb.append(" ").append(v);
		}

		if(currentAnimation != null)
			sb.append("\n  Current Anim: ").append(currentAnimation.getId());

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
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, String direction) {
		StringBuilder sb = new StringBuilder(id);
		
		// if dir==null gets the current animation direction
		if(direction == null) {
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
				
		if(getAnimation(anim) == null) {
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
		
		if(entry == null) {
			entry = new AtlasCacheEntry();
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0)
			EngineAssetManager.getInstance().loadAtlas(source);

		entry.refCounter++;
	}
	
	private void retrieveFA(AtlasAnimationDesc fa) {
		retrieveSource(fa.getSource());
		fa.regions = EngineAssetManager.getInstance().getRegions(fa.getSource(), fa.getId());
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
			if (fa.isPreload())
				loadSource(fa.getSource());
		}

		if (currentAnimation != null && !currentAnimation.isPreload()) {
			loadSource(currentAnimation.getSource());
		} else if (currentAnimation == null && initAnimation != null) {
			AnimationDesc fa = fanims.get(initAnimation);

			if (!fa.isPreload())
				loadSource(fa.getSource());
		}
	}

	@Override
	public void retrieveAssets() {
		for (AnimationDesc fa : fanims.values()) {
			if(fa.isPreload())
				retrieveFA((AtlasAnimationDesc)fa);
		}
		
		if(currentAnimation != null && !currentAnimation.isPreload()) {
			retrieveFA(currentAnimation);
		} else if(currentAnimation == null && initAnimation != null) {
			AtlasAnimationDesc fa = (AtlasAnimationDesc)fanims.get(initAnimation);
			
			if(!fa.isPreload())
				retrieveFA(fa);		
		}

		if (currentAnimation != null) {		
			setFrame(currentFrameIndex);
		} else if(initAnimation != null){
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

		json.writeValue("fanims", fanims, HashMap.class, AtlasAnimationDesc.class);

		String currentAnimationId = null;

		if (currentAnimation != null)
			currentAnimationId = currentAnimation.getId();

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
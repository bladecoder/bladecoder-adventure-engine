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
package org.bladecoder.engine.model;

import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.ActionCallbackSerialization;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationState.AnimationStateListener;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBinary;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonRenderer;

public class SpineRenderer implements SpriteRenderer {

	private HashMap<String, FrameAnimation> fanims = new HashMap<String, FrameAnimation>();

	/** Starts this anim the first time that the scene is loaded */
	private String initFrameAnimation;
	private FrameAnimation currentFrameAnimation;

	private ActionCallback animationCb = null;
	private String animationCbSer = null;

	private int currentCount;
	private int currentAnimationType;

	private boolean flipX;

	private SkeletonCacheEntry currentSource;

	private SkeletonRenderer renderer;
	private SkeletonBounds bounds;

	private final HashMap<String, SkeletonCacheEntry> sourceCache = new HashMap<String, SkeletonCacheEntry>();
	
	private float lastAnimationTime = 0;

	class SkeletonCacheEntry {
		int refCounter;
		Skeleton skeleton;
		AnimationState animation;
	}

	public SpineRenderer() {
		
	}
	
	private AnimationStateListener animationListener = new AnimationStateListener() {
		@Override
		public void complete(int trackIndex, int loopCount) {
			if (currentAnimationType == Tween.REPEAT && (currentCount == Tween.INFINITY || currentCount > loopCount)) {
				return;
			}

			currentSource.animation.setTimeScale(0);
			if (animationCb != null || animationCbSer != null) {

				if (animationCb == null) {
					animationCb = ActionCallbackSerialization
							.find(animationCbSer);
					animationCbSer = null;
				}

				ActionCallbackQueue.add(animationCb);
				animationCb = null;
			}
		}

		@Override
		public void end(int arg0) {
		}

		@Override
		public void event(int arg0, Event arg1) {
		}

		@Override
		public void start(int arg0) {
		}
	};

	@Override
	public HashMap<String, FrameAnimation> getFrameAnimations() {
		return fanims;
	}

	@Override
	public void addFrameAnimation(FrameAnimation fa) {
		if (initFrameAnimation == null)
			initFrameAnimation = fa.id;

		fanims.put(fa.id, fa);
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
	public void setInitFrameAnimation(String fa) {
		initFrameAnimation = fa;
	}

	@Override
	public String getInitFrameAnimation() {
		return initFrameAnimation;
	}

	@Override
	public String[] getInternalAnimations(String source) {
		retrieveSource(source);

		Array<Animation> animations = sourceCache.get(source).skeleton
				.getData().getAnimations();
		String[] result = new String[animations.size];

		for (int i = 0; i < animations.size; i++) {
			Animation a = animations.get(i);
			result[i] = a.getName();
		}

		return result;
	}

	@Override
	public void update(float delta) {
		if (currentSource != null && currentSource.skeleton != null) {
			currentSource.animation.update(delta);
			currentSource.animation.apply(currentSource.skeleton);
			currentSource.skeleton.updateWorldTransform();
			
			lastAnimationTime += delta;
		}
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale) {

		if (currentSource != null && currentSource.skeleton != null) {
			currentSource.skeleton.setX(x / scale);
			currentSource.skeleton.setY(y / scale);

			batch.setTransformMatrix(batch.getTransformMatrix().scale(scale,
					scale, 1.0f));
			renderer.draw(batch, currentSource.skeleton);
			batch.setTransformMatrix(batch.getTransformMatrix().scale(
					1 / scale, 1 / scale, 1.0f));
		} else {
			x = x - getWidth() / 2 * scale;
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight()
					* scale, Color.RED);
		}
	}

	@Override
	public float getWidth() {
		if (bounds != null && bounds.getWidth() > 0) {
			return bounds.getWidth();
		}			

		return 200;
	}

	@Override
	public float getHeight() {
		if (bounds != null && bounds.getHeight() > 0)
			return bounds.getHeight();

		return 200;
	}

	@Override
	public FrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	@Override
	public void lookat(float x, float y, Vector2 pf) {
		lookat(FrameAnimation.getFrameDirection(x, y, pf));
	}

	@Override
	public void lookat(String direction) {
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.STAND_ANIM);
		sb.append('.');
		sb.append(direction);

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
		sb.append(FrameAnimation.WALK_ANIM).append('.')
				.append(currentDirection);
		startFrameAnimation(sb.toString(), Tween.FROM_FA, 1, null);
	}

	@Override
	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		FrameAnimation fa = getFrameAnimation(id);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}

		if (currentFrameAnimation != null
				&& currentFrameAnimation.disposeWhenPlayed)
			disposeSource(currentFrameAnimation.source);

		currentFrameAnimation = fa;
		currentSource = sourceCache.get(fa.source);

		animationCb = cb;

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

		if (repeatType == Tween.FROM_FA) {
			currentAnimationType = currentFrameAnimation.animationType;
			currentCount = currentFrameAnimation.count;
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		lastAnimationTime = 0;
		currentSource.skeleton.setFlipX(flipX);
		currentSource.animation.setTimeScale(fa.duration);
		currentSource.animation.setAnimation(0, fa.id,
				currentAnimationType == Tween.REPEAT);
		update(lastAnimationTime);
		bounds.update(currentSource.skeleton, true);
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
				
				if (id.endsWith(FrameAnimation.FRONTLEFT) || id.endsWith(FrameAnimation.FRONTRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(FrameAnimation.FRONT);
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

	private void loadSource(String source) {
		SkeletonCacheEntry entry = sourceCache.get(source);

		if (entry == null) {
			entry = new SkeletonCacheEntry();
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0)
			EngineAssetManager.getInstance().loadAtlas(source);

		entry.refCounter++;
	}

	private void retrieveSource(String source) {
		SkeletonCacheEntry entry = sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
			entry = sourceCache.get(source);
		}

		if (entry.skeleton == null) {
			TextureAtlas atlas = EngineAssetManager.getInstance()
					.getTextureAtlas(source);

			SkeletonBinary skel = new SkeletonBinary(atlas);
			skel.setScale(EngineAssetManager.getInstance().getScale());
			SkeletonData skeletonData = skel
					.readSkeletonData(EngineAssetManager.getInstance()
							.getSpine(source));

			entry.skeleton = new Skeleton(skeletonData);

			AnimationStateData stateData = new AnimationStateData(skeletonData); // Defines
																					// mixing
																					// between
																					// animations.
			stateData.setDefaultMix(0f);

			entry.animation = new AnimationState(stateData);
			entry.animation.addListener(animationListener);
		}
	}

	private void disposeSource(String source) {
		SkeletonCacheEntry entry = sourceCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeAtlas(source);
			entry.animation = null;
			entry.skeleton = null;
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
		renderer = new SkeletonRenderer();
		renderer.setPremultipliedAlpha(false);
		bounds = new SkeletonBounds();

		for (String key : sourceCache.keySet()) {
			if (sourceCache.get(key).refCounter > 0)
				retrieveSource(key);
		}

		if (currentFrameAnimation != null) {
			SkeletonCacheEntry entry = sourceCache
					.get(currentFrameAnimation.source);
			currentSource = entry;
			
			currentSource.skeleton.setFlipX(flipX);
			currentSource.animation.setAnimation(0, currentFrameAnimation.id,
					currentAnimationType == Tween.REPEAT);
			currentSource.animation.setTimeScale(currentFrameAnimation.duration);
//			currentSource.animation.apply(currentSource.skeleton);
			update(lastAnimationTime);
			bounds.update(currentSource.skeleton, true);

		} else if (initFrameAnimation != null) {
			startFrameAnimation(initFrameAnimation, Tween.FROM_FA, 1, null);
		}
	}

	@Override
	public void dispose() {
		for (String key : sourceCache.keySet()) {
			EngineAssetManager.getInstance().disposeAtlas(key);
		}

		sourceCache.clear();
		currentSource = null;
		renderer = null;
		bounds = null;
	}

	@Override
	public void write(Json json) {

		json.writeValue("fanims", fanims, HashMap.class, FrameAnimation.class);

		String currentFrameAnimationId = null;

		if (currentFrameAnimation != null)
			currentFrameAnimationId = currentFrameAnimation.id;

		json.writeValue("currentFrameAnimation", currentFrameAnimationId,
				currentFrameAnimationId == null ? null : String.class);
		
		json.writeValue("initFrameAnimation", initFrameAnimation);

		json.writeValue("flipX", flipX);
		
		if(animationCbSer != null)
			json.writeValue("cb", animationCbSer);
		else 
			json.writeValue("cb", ActionCallbackSerialization.find(animationCb),
					animationCb == null ? null : String.class);
		
		json.writeValue("currentCount", currentCount);
		json.writeValue("currentAnimationType", currentAnimationType);
		json.writeValue("lastAnimationTime", lastAnimationTime);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		fanims = json.readValue("fanims", HashMap.class,
				FrameAnimation.class, jsonData);

		String currentFrameAnimationId = json.readValue(
				"currentFrameAnimation", String.class, jsonData);

		if (currentFrameAnimationId != null)
			currentFrameAnimation = (AtlasFrameAnimation)fanims.get(currentFrameAnimationId);
		
		initFrameAnimation = json.readValue("initFrameAnimation", String.class,
				jsonData);

		flipX = json.readValue("flipX", Boolean.class, jsonData);
		animationCbSer = json.readValue("cb", String.class, jsonData);
		currentCount = json.readValue("currentCount", Integer.class, jsonData);
		currentAnimationType = json.readValue("currentAnimationType", Integer.class, jsonData);
		lastAnimationTime = json.readValue("lastAnimationTime", Float.class, jsonData);
	}
}
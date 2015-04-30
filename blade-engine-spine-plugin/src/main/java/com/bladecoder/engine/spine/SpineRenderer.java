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
package com.bladecoder.engine.spine;

import java.util.HashMap;

import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.SpineAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.ActionCallbackSerialization;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationState.AnimationStateListener;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBinary;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.esotericsoftware.spine.Slot;

public class SpineRenderer implements ActorRenderer {
	
	private final static int PLAY_ANIMATION_EVENT = 0;
	private final static int PLAY_SOUND_EVENT = 1;
	private final static int RUN_VERB_EVENT = 2;
	private final static int LOOP_EVENT = 3;
	
	private final static float DEFAULT_DIM = 200;

	private HashMap<String, AnimationDesc> fanims = new HashMap<String, AnimationDesc>();

	/** Starts this anim the first time that the scene is loaded */
	private String initAnimation;
	private AnimationDesc currentAnimation;

	private ActionCallback animationCb = null;
	private String animationCbSer = null;

	private int currentCount;
	private int currentAnimationType;

	private boolean flipX;

	private SkeletonCacheEntry currentSource;

	private SkeletonRenderer renderer;
	private SkeletonBounds bounds;
	private float width = DEFAULT_DIM, height = DEFAULT_DIM;

	private final HashMap<String, SkeletonCacheEntry> sourceCache = new HashMap<String, SkeletonCacheEntry>();

	private float lastAnimationTime = 0;
	
	transient private boolean eventsEnabled = true;

	class SkeletonCacheEntry {
		int refCounter;
		Skeleton skeleton;
		AnimationState animation;
		String atlas;
	}

	public SpineRenderer() {

	}
	

	public void enableEvents(boolean v) {
		eventsEnabled = v;
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
					animationCb = ActionCallbackSerialization.find(animationCbSer);
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
		public void event(int trackIndex, Event event) {
			if(!eventsEnabled)
				return;
			
			String actorId = event.getData().getName();
			BaseActor actor = World.getInstance().getCurrentScene().getActor(actorId, true);
			
			switch(event.getInt()) {
			case PLAY_ANIMATION_EVENT:
				((SpriteActor)actor).startAnimation(event.getString(), null);
				break;
			case PLAY_SOUND_EVENT:
				actor.playSound(event.getString());
				break;
			case RUN_VERB_EVENT:
				actor.runVerb(event.getString());
				break;
			case LOOP_EVENT:
				// used for looping from a starting frame
				break;				
			default:
				EngineLogger.error("Spine event not recognized.");
			}
		}

		@Override
		public void start(int arg0) {
		}
	};

	@Override
	public HashMap<String, AnimationDesc> getAnimations() {
		return fanims;
	}

	@Override
	public void addAnimation(AnimationDesc fa) {
		if (initAnimation == null)
			initAnimation = fa.id;

		fanims.put(fa.id, fa);
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
	public void setInitAnimation(String fa) {
		initAnimation = fa;
	}

	@Override
	public String getInitAnimation() {
		return initAnimation;
	}

	@Override
	public String[] getInternalAnimations(AnimationDesc anim) {
		retrieveSource(anim.source, ((SpineAnimationDesc)anim).atlas);

		Array<Animation> animations = sourceCache.get(anim.source).skeleton.getData().getAnimations();
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
			float d = delta;
			
			if(currentAnimationType == Tween.REVERSE) {
				d = -delta;
				
				if(lastAnimationTime < 0) {
					animationListener.complete(0, 1);
					lastAnimationTime = 0f; // to avoid trigger more complete evetns
					return;
				}
			}
			
			updateAnimation(d);

			lastAnimationTime += d;
		}
	}
	
	private void updateAnimation(float time) {
		currentSource.animation.update(time);
		currentSource.animation.apply(currentSource.skeleton);
		currentSource.skeleton.updateWorldTransform();
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale) {

		if (currentSource != null && currentSource.skeleton != null) {
			currentSource.skeleton.setX(x / scale);
			currentSource.skeleton.setY(y / scale);

			batch.setTransformMatrix(batch.getTransformMatrix().scale(scale, scale, 1.0f));
			renderer.draw(batch, currentSource.skeleton);
			batch.setTransformMatrix(batch.getTransformMatrix().scale(1 / scale, 1 / scale, 1.0f));
		} else {
			x = x - getWidth() / 2 * scale;
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight() * scale, Color.RED);
		}
	}

	@Override
	public float getWidth() {
		return width;
	}

	@Override
	public float getHeight() {
		return height;
	}

	@Override
	public AnimationDesc getCurrentAnimation() {
		return currentAnimation;
	}

	@Override
	public void lookat(float x, float y, Vector2 pf) {
		lookat(AnimationDesc.getFrameDirection(x, y, pf));
	}

	@Override
	public void lookat(String direction) {
		StringBuilder sb = new StringBuilder();
		sb.append(AnimationDesc.STAND_ANIM);
		sb.append('.');
		sb.append(direction);

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

	@Override
	public void startAnimation(String id, int repeatType, int count, ActionCallback cb) {
		SpineAnimationDesc fa = (SpineAnimationDesc)getAnimation(id);

		if (fa == null) {
			EngineLogger.error("AnimationDesc not found: " + id);

			return;
		}

		if (currentAnimation != null && currentAnimation.disposeWhenPlayed)
			disposeSource(currentAnimation.source);

		currentAnimation = fa;
		currentSource = sourceCache.get(fa.source);

		animationCb = cb;

		// If the source is not loaded. Load it.
		if (currentSource == null || currentSource.refCounter < 1) {
			loadSource(fa.source, fa.atlas);
			EngineAssetManager.getInstance().finishLoading();

			retrieveSource(fa.source, fa.atlas);

			currentSource = sourceCache.get(fa.source);

			if (currentSource == null) {
				EngineLogger.error("Could not load AnimationDesc: " + id);
				currentAnimation = null;

				return;
			}
		}

		if (repeatType == Tween.FROM_FA) {
			currentAnimationType = currentAnimation.animationType;
			currentCount = currentAnimation.count;
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		if(currentAnimationType == Tween.REVERSE) {
			// get animation duration
			Array<Animation> animations = currentSource.skeleton.getData().getAnimations();
			
			for(Animation a: animations) {
				if(a.getName().equals(currentAnimation.id)) {
					lastAnimationTime = a.getDuration() - 0.01f;
					break;
				}
			}
		} else {
			lastAnimationTime = 0f;
		}
		
		setCurrentAnimation();
	}

	private void setCurrentAnimation() {
		try {
			// TODO Make setup pose parametrizable in the AnimationDesc
			currentSource.skeleton.setToSetupPose();
			currentSource.skeleton.setFlipX(flipX);
			currentSource.animation.setTimeScale(currentAnimation.duration);
			currentSource.animation.setAnimation(0, currentAnimation.id, currentAnimationType == Tween.REPEAT);
			
			updateAnimation(lastAnimationTime);
			
		} catch (Exception e) {
			EngineLogger.error("SpineRenderer:setCurrentFA " + e.getMessage());
		}
	}

	@Override
	public void computeBbox(Polygon bbox) {
		float minX, minY, maxX, maxY;
		
		if(bbox.getVertices() == null || bbox.getVertices().length != 8) {
			bbox.setVertices(new float[8]);
		}
		
		float[] verts = bbox.getVertices();
		
		if(currentSource == null || currentSource.skeleton == null) {
			
			verts[0] = -getWidth()/2;
			verts[1] = 0f;
			verts[2] = -getWidth()/2;
			verts[3] = getHeight();
			verts[4] = getWidth()/2;
			verts[5] = getHeight();
			verts[6] = getWidth()/2;
			verts[7] = 0f;
			bbox.dirty();
			return;
		}
		
		currentSource.skeleton.setPosition(0,0);
		currentSource.skeleton.updateWorldTransform();
		bounds.update(currentSource.skeleton, true);

		if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
			width = bounds.getWidth();
			height = bounds.getHeight();
			minX = bounds.getMinX();
			minY = bounds.getMinY();
			maxX = bounds.getMaxX();
			maxY = bounds.getMaxY();
		} else {

			minX = Float.MAX_VALUE;
			minY = Float.MAX_VALUE;
			maxX = Float.MIN_VALUE;
			maxY = Float.MIN_VALUE;

			Array<Slot> slots = currentSource.skeleton.getSlots();

			for (int i = 0, n = slots.size; i < n; i++) {
				Slot slot = slots.get(i);
				Attachment attachment = slot.getAttachment();
				if (attachment == null)
					continue;

				if (!(attachment instanceof RegionAttachment))
					continue;

				((RegionAttachment) attachment).updateWorldVertices(slot, false);

				float[] vertices = ((RegionAttachment) attachment).getWorldVertices();
				for (int ii = 0, nn = vertices.length; ii < nn; ii += 5) {
					minX = Math.min(minX, vertices[ii]);
					minY = Math.min(minY, vertices[ii + 1]);
					maxX = Math.max(maxX, vertices[ii]);
					maxY = Math.max(maxY, vertices[ii + 1]);
				}
			}
			
			width = (maxX - minX);
			height = (maxY - minY);
			
			if(width <= 0 || height <= 0) {
				width = height = DEFAULT_DIM;
			}
		}
		
		verts[0] = minX;
		verts[1] = minY;
		verts[2] = minX;
		verts[3] = maxY;
		verts[4] = maxX;
		verts[5] = maxY;
		verts[6] = maxX;
		verts[7] = minY;
		
		bbox.dirty();
	}

	private AnimationDesc getAnimation(String id) {
		AnimationDesc fa = fanims.get(id);
		flipX = false;

		if (fa == null && id.indexOf('.') != -1) {
			// Search for flipped
			String flipId = AnimationDesc.getFlipId(id);

			fa = fanims.get(flipId);

			if (fa != null)
				flipX = true;
			else {
				// search for .left if .frontleft not found and viceversa
				StringBuilder sb = new StringBuilder();

				if (id.endsWith(AnimationDesc.FRONTLEFT) || id.endsWith(AnimationDesc.FRONTRIGHT)) {
					sb.append(id.substring(0, id.lastIndexOf('.') + 1));
					sb.append(AnimationDesc.FRONT);
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

					if (fa != null) {
						flipX = true;
					} else if (s.endsWith(AnimationDesc.FRONT) || s.endsWith(AnimationDesc.BACK)) {
						// search only for right or left animations
						if (id.endsWith(AnimationDesc.LEFT)) {
							sb.append(id.substring(0, id.lastIndexOf('.') + 1));
							sb.append(AnimationDesc.LEFT);
						} else {
							sb.append(id.substring(0, id.lastIndexOf('.') + 1));
							sb.append(AnimationDesc.RIGHT);
						}

						s = sb.toString();
						fa = fanims.get(s);

						if (fa == null) {
							// Search for flipped
							flipId = AnimationDesc.getFlipId(s);

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

	private void loadSource(String source, String atlas) {
		SkeletonCacheEntry entry = sourceCache.get(source);

		if (entry == null) {
			entry = new SkeletonCacheEntry();
			entry.atlas = atlas;
			sourceCache.put(source, entry);
		} 

		if (entry.refCounter == 0)
			EngineAssetManager.getInstance().loadAtlas(atlas==null?source:atlas);

		entry.refCounter++;
	}

	private void retrieveSource(String source, String atlas) {
		SkeletonCacheEntry entry = sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source, atlas);
			EngineAssetManager.getInstance().finishLoading();
			entry = sourceCache.get(source);
		}

		if (entry.skeleton == null) {
			TextureAtlas atlasTex = EngineAssetManager.getInstance().getTextureAtlas(atlas == null?source:atlas);

			SkeletonBinary skel = new SkeletonBinary(atlasTex);
			skel.setScale(EngineAssetManager.getInstance().getScale());
			SkeletonData skeletonData = skel.readSkeletonData(EngineAssetManager.getInstance().getSpine(source));

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
		for (AnimationDesc fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source, ((SpineAnimationDesc)fa).atlas);
		}

		if (currentAnimation != null && !currentAnimation.preload) {
			loadSource(currentAnimation.source, ((SpineAnimationDesc)currentAnimation).atlas);
		} else if (currentAnimation == null && initAnimation != null) {
			AnimationDesc fa = fanims.get(initAnimation);

			if (fa != null && !fa.preload)
				loadSource(fa.source, ((SpineAnimationDesc)fa).atlas);
		}
	}

	@Override
	public void retrieveAssets() {
		renderer = new SkeletonRenderer();
		renderer.setPremultipliedAlpha(false);
		bounds = new SkeletonBounds();

		for (String key : sourceCache.keySet()) {
			if (sourceCache.get(key).refCounter > 0)
				retrieveSource(key, sourceCache.get(key).atlas);
		}

		if (currentAnimation != null) {
			SkeletonCacheEntry entry = sourceCache.get(currentAnimation.source);
			currentSource = entry;

			setCurrentAnimation();

		} else if (initAnimation != null) {
			startAnimation(initAnimation, Tween.FROM_FA, 1, null);
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

		json.writeValue("fanims", fanims, HashMap.class, AnimationDesc.class);

		String currentAnimationId = null;

		if (currentAnimation != null)
			currentAnimationId = currentAnimation.id;

		json.writeValue("currentAnimation", currentAnimationId, currentAnimationId == null ? null : String.class);

		json.writeValue("initAnimation", initAnimation);

		json.writeValue("flipX", flipX);

		if (animationCbSer != null)
			json.writeValue("cb", animationCbSer);
		else
			json.writeValue("cb", ActionCallbackSerialization.find(animationCb), animationCb == null ? null
					: String.class);

		json.writeValue("currentCount", currentCount);
		json.writeValue("currentAnimationType", currentAnimationType);
		json.writeValue("lastAnimationTime", lastAnimationTime);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		fanims = json.readValue("fanims", HashMap.class, AnimationDesc.class, jsonData);

		String currentAnimationId = json.readValue("currentAnimation", String.class, jsonData);

		if (currentAnimationId != null)
			currentAnimation = (AtlasAnimationDesc) fanims.get(currentAnimationId);

		initAnimation = json.readValue("initAnimation", String.class, jsonData);

		flipX = json.readValue("flipX", Boolean.class, jsonData);
		animationCbSer = json.readValue("cb", String.class, jsonData);
		currentCount = json.readValue("currentCount", Integer.class, jsonData);
		currentAnimationType = json.readValue("currentAnimationType", Integer.class, jsonData);
		lastAnimationTime = json.readValue("lastAnimationTime", Float.class, jsonData);
	}
}
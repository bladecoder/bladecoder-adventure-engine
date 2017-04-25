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
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.SpineAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.spine.SkeletonDataLoader.SkeletonDataLoaderParameter;
import com.bladecoder.engine.util.ActionCallbackSerialization;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationState.AnimationStateListener;
import com.esotericsoftware.spine.AnimationState.AnimationStateAdapter;
import com.esotericsoftware.spine.AnimationState.TrackEntry;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;

public class SpineRenderer extends AnimationRenderer {

	private final static int PLAY_ANIMATION_EVENT = 0;
	private final static int PLAY_SOUND_EVENT = 1;
	private final static int RUN_VERB_EVENT = 2;
	private final static int LOOP_EVENT = 3;

	private ActionCallback animationCb = null;

	private int currentCount;
	private Tween.Type currentAnimationType;

	private SkeletonRenderer<SpriteBatch> renderer;
	private SkeletonBounds bounds;

	private float width = super.getWidth(), height = super.getHeight();

	private float lastAnimationTime = 0;

	private boolean complete = false;

	private boolean eventsEnabled = true;
	
	private int loopCount = 0;

	class SkeletonCacheEntry extends CacheEntry {
		Skeleton skeleton;
		AnimationState animation;
		String atlas;
	}

	public SpineRenderer() {

	}

	public void enableEvents(boolean v) {
		eventsEnabled = v;
	}

	private AnimationStateListener animationListener = new AnimationStateAdapter() {
		@Override
		public void complete(TrackEntry entry) {
			if (complete)
				return;
			
			loopCount++;

			if ((currentAnimationType == Tween.Type.REPEAT || currentAnimationType == Tween.Type.REVERSE_REPEAT)
					&& (currentCount == Tween.INFINITY || currentCount >= loopCount)) {

				// FIX for latest spine rt not setting setup pose when looping.
				SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

				cs.skeleton.setToSetupPose();
				cs.skeleton.setFlipX(flipX);
				complete = true;
				cs.animation.update(0);
				cs.animation.apply(cs.skeleton);
				cs.skeleton.updateWorldTransform();
				complete = false;
				// END FIX

				return;
			}

			complete = true;
			computeBbox();

			if (animationCb != null) {
				ActionCallbackQueue.add(animationCb);
				animationCb = null;
			}
		}

		@Override
		public void event(TrackEntry entry, Event event) {
			if (!eventsEnabled || currentAnimationType == Tween.Type.REVERSE)
				return;

			String actorId = event.getData().getName();

			EngineLogger.debug("Spine event " + event.getInt() + ":" + actorId + "." + event.getString());

			InteractiveActor actor = (InteractiveActor) World.getInstance().getCurrentScene().getActor(actorId, true);

			if (actor == null) {
				EngineLogger.debug("Actor in Spine event not found in scene: " + actorId);
				return;
			}

			switch (event.getInt()) {
			case PLAY_ANIMATION_EVENT:
				((SpriteActor) actor).startAnimation(event.getString(), null);
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
	};

	@Override
	public String[] getInternalAnimations(AnimationDesc anim) {
		try {
			retrieveSource(anim.source, ((SpineAnimationDesc) anim).atlas);
		} catch (GdxRuntimeException e) {
			sourceCache.remove(anim.source);
			Array<String> dependencies = EngineAssetManager.getInstance().getDependencies(getFileName(anim.source));
			if(dependencies.size > 0)
				dependencies.removeIndex(dependencies.size - 1);
			return new String[0];
		}

		Array<Animation> animations = ((SkeletonCacheEntry) sourceCache.get(anim.source)).skeleton.getData()
				.getAnimations();
		
		String[] result = new String[animations.size];

		for (int i = 0; i < animations.size; i++) {
			Animation a = animations.get(i);
			result[i] = a.getName();
		}

		return result;
	}

	@Override
	public void update(float delta) {
		if (complete) {
			return;
		}

		if (currentSource != null && ((SkeletonCacheEntry) currentSource).skeleton != null) {
			float d = delta;

			if (currentAnimationType == Tween.Type.REVERSE) {
				d = -delta;

				if (lastAnimationTime < 0) {
					lastAnimationTime = 0;
					loopCount = 0;
					animationListener.complete(null);
					return;
				}
			}

			lastAnimationTime += d;

			if (lastAnimationTime >= 0)
				updateAnimation(d);
		}
	}

	private void updateAnimation(float time) {
		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

		cs.animation.update(time);
		cs.animation.apply(cs.skeleton);
		cs.skeleton.updateWorldTransform();
	}

	private static final Matrix4 tmp = new Matrix4();

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale, float rotation, Color tint) {

		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

		if (cs != null && cs.skeleton != null) {
			Matrix4 tm = batch.getTransformMatrix();
			tmp.set(tm);

			float originX = cs.skeleton.getRootBone().getX();
			float originY = cs.skeleton.getRootBone().getY();
			tm.translate(x, y, 0).rotate(0, 0, 1, rotation).scale(scale, scale, 1).translate(originX, originY, 0);

			// cs.skeleton.setX(x / scale);
			// cs.skeleton.setY(y / scale);

			batch.setTransformMatrix(tm);

			if (tint != null)
				cs.skeleton.setColor(tint);

			renderer.draw(batch, cs.skeleton);

			if (tint != null)
				batch.setColor(Color.WHITE);
			batch.setTransformMatrix(tmp);
		} else {
			float dx = getAlignDx(getWidth(), orgAlign);
			float dy = getAlignDy(getHeight(), orgAlign);

			RectangleRenderer.draw(batch, x + dx * scale, y + dy * scale, getWidth() * scale, getHeight() * scale,
					Color.RED);
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

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb) {
		SpineAnimationDesc fa = (SpineAnimationDesc) getAnimation(id);

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

		if (repeatType == Tween.Type.SPRITE_DEFINED) {
			currentAnimationType = currentAnimation.animationType;
			currentCount = currentAnimation.count;
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		if (currentAnimationType == Tween.Type.REVERSE) {
			// get animation duration
			Array<Animation> animations = ((SkeletonCacheEntry) currentSource).skeleton.getData().getAnimations();

			for (Animation a : animations) {
				if (a.getName().equals(currentAnimation.id)) {
					lastAnimationTime = a.getDuration() / currentAnimation.duration - 0.01f;

					System.out.println("LAST ANIM TIME: " + lastAnimationTime + " ID: " + currentAnimation.id);
					break;
				}
			}
		} else {
			lastAnimationTime = 0f;
		}

		complete = false;
		loopCount = 0;
		setCurrentAnimation();
	}

	private void setCurrentAnimation() {
		try {
			SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;
			cs.skeleton.setToSetupPose();
			cs.skeleton.setFlipX(flipX);
			cs.animation.setTimeScale(currentAnimation.duration);
			cs.animation.setAnimation(0, currentAnimation.id, currentAnimationType == Tween.Type.REPEAT);

			updateAnimation(lastAnimationTime);
			computeBbox();

		} catch (Exception e) {
			EngineLogger.error("SpineRenderer:setCurrentFA " + e.getMessage());
		}
	}

	@Override
	public void computeBbox() {
		float minX, minY, maxX, maxY;

		if (bbox != null && (bbox.getVertices() == null || bbox.getVertices().length != 8)) {
			bbox.setVertices(new float[8]);
		}

		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

		if (cs == null || cs.skeleton == null) {

			if (bbox != null) {

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
			return;
		}

		cs.skeleton.setPosition(0, 0);
		cs.skeleton.updateWorldTransform();
		bounds.update(cs.skeleton, true);

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

			Array<Slot> slots = cs.skeleton.getSlots();

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

			if (width <= minX || height <= minY) {
				width = height = super.getWidth();
				float dim2 = super.getWidth() / 2;
				minX = -dim2;
				minY = -dim2;
				maxX = dim2;
				maxY = dim2;
			}
		}

		if (bbox != null) {
			float[] verts = bbox.getVertices();
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
	
	
	private String getFileName(String source) {
		return EngineAssetManager.SPINE_DIR + source + EngineAssetManager.SPINE_EXT;
	}

	private void loadSource(String source, String atlas) {
		EngineLogger.debug("Loading: " + source);
		SkeletonCacheEntry entry = (SkeletonCacheEntry) sourceCache.get(source);

		if (entry == null) {
			entry = new SkeletonCacheEntry();
			entry.atlas = atlas == null ? source : atlas;
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0) {

			if (EngineAssetManager.getInstance().getLoader(SkeletonData.class) == null) {
				EngineAssetManager.getInstance().setLoader(SkeletonData.class,
						new SkeletonDataLoader(EngineAssetManager.getInstance().getFileHandleResolver()));
			}

			SkeletonDataLoaderParameter parameter = new SkeletonDataLoaderParameter(
					EngineAssetManager.ATLASES_DIR + entry.atlas + EngineAssetManager.ATLAS_EXT,
					EngineAssetManager.getInstance().getScale());
			EngineAssetManager.getInstance().load(getFileName(source),
					SkeletonData.class, parameter);
		}

		entry.refCounter++;
	}

	private void retrieveSource(String source, String atlas) {
		EngineLogger.debug("Retrieving: " + source);
		SkeletonCacheEntry entry = (SkeletonCacheEntry) sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source, atlas);
			EngineAssetManager.getInstance().finishLoading();
			entry = (SkeletonCacheEntry) sourceCache.get(source);
		}

		if (entry.skeleton == null) {
			SkeletonData skeletonData = EngineAssetManager.getInstance()
					.get(getFileName(source), SkeletonData.class);

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
		EngineLogger.debug("Disposing: " + source);
		SkeletonCacheEntry entry = (SkeletonCacheEntry) sourceCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance()
					.unload(EngineAssetManager.SPINE_DIR + source + EngineAssetManager.SPINE_EXT);
			entry.animation = null;
			entry.skeleton = null;
		}

		entry.refCounter--;
	}

	@Override
	public void loadAssets() {
		for (AnimationDesc fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source, ((SpineAnimationDesc) fa).atlas);
		}

		if (currentAnimation != null && !currentAnimation.preload) {
			loadSource(currentAnimation.source, ((SpineAnimationDesc) currentAnimation).atlas);
		} else if (currentAnimation == null && initAnimation != null) {
			AnimationDesc fa = fanims.get(initAnimation);

			if (fa != null && !fa.preload)
				loadSource(fa.source, ((SpineAnimationDesc) fa).atlas);
		}
	}

	@Override
	public void retrieveAssets() {
		renderer = new SkeletonRenderer<SpriteBatch>();
		renderer.setPremultipliedAlpha(false);
		bounds = new SkeletonBounds();

		for (String key : sourceCache.keySet()) {
			if (sourceCache.get(key).refCounter > 0)
				retrieveSource(key, ((SkeletonCacheEntry) sourceCache.get(key)).atlas);
		}

		if (currentAnimation != null) {
			SkeletonCacheEntry entry = (SkeletonCacheEntry) sourceCache.get(currentAnimation.source);
			currentSource = entry;

			// Stop events to avoid event trigger
			boolean prevEnableEvents = eventsEnabled;

			eventsEnabled = false;
			setCurrentAnimation();
			eventsEnabled = prevEnableEvents;

		} else if (initAnimation != null) {
			startAnimation(initAnimation, Tween.Type.SPRITE_DEFINED, 1, null);
		}

		computeBbox();
	}

	@Override
	public void dispose() {
		for (Entry<String, CacheEntry> entry : sourceCache.entrySet()) {
			if (entry.getValue().refCounter > 0)
				EngineAssetManager.getInstance()
						.unload(EngineAssetManager.SPINE_DIR + entry.getKey() + EngineAssetManager.SPINE_EXT);
		}

		sourceCache.clear();
		currentSource = null;
		renderer = null;
		bounds = null;
	}

	@Override
	public void write(Json json) {
		super.write(json);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

		} else {

			json.writeValue("cb", ActionCallbackSerialization.find(animationCb));
			json.writeValue("currentCount", currentCount);

			if (currentAnimation != null)
				json.writeValue("currentAnimationType", currentAnimationType);

			json.writeValue("lastAnimationTime", lastAnimationTime);
			json.writeValue("complete", complete);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			fanims = json.readValue("fanims", HashMap.class, SpineAnimationDesc.class, jsonData);
		} else {

			String animationCbSer = json.readValue("cb", String.class, jsonData);
			animationCb = ActionCallbackSerialization.find(animationCbSer);

			currentCount = json.readValue("currentCount", Integer.class, jsonData);

			if (currentAnimation != null)
				currentAnimationType = json.readValue("currentAnimationType", Tween.Type.class, jsonData);

			lastAnimationTime = json.readValue("lastAnimationTime", Float.class, jsonData);
			complete = json.readValue("complete", Boolean.class, jsonData);
		}
	}
}
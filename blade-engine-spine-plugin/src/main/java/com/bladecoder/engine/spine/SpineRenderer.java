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
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.SpineAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.spine.SkeletonDataLoader.SkeletonDataLoaderParameter;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.RectangleRenderer;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationState.AnimationStateAdapter;
import com.esotericsoftware.spine.AnimationState.AnimationStateListener;
import com.esotericsoftware.spine.AnimationState.TrackEntry;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.esotericsoftware.spine.Skin;

public class SpineRenderer extends AnimationRenderer {

	private final static int PLAY_ANIMATION_EVENT = 0;
	private final static int PLAY_SOUND_EVENT = 1;
	private final static int RUN_VERB_EVENT = 2;
	private final static int LOOP_EVENT = 3;

	private ActionCallback animationCb = null;

	private int currentCount;
	private Tween.Type currentAnimationType;

	private SkeletonRenderer renderer;
	private SkeletonBounds bounds;

	private float width = super.getWidth(), height = super.getHeight();

	private float lastAnimationTime = 0;

	private boolean complete = false;

	private boolean eventsEnabled = true;

	private int loopCount = 0;

	private String secondaryAnimation;

	private String skin;

	private World world;

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
			if (complete || (entry != null && entry.getTrackIndex() != 0))
				return;

			loopCount++;

			if ((currentAnimationType == Tween.Type.REPEAT || currentAnimationType == Tween.Type.REVERSE_REPEAT)
					&& (currentCount == Tween.INFINITY || currentCount >= loopCount)) {
				return;
			}

			complete = true;
			computeBbox();

			if (animationCb != null) {
				ActionCallback tmpcb = animationCb;
				animationCb = null;
				tmpcb.resume();
			}
		}

		@Override
		public void event(TrackEntry entry, Event event) {
			if (!eventsEnabled || currentAnimationType == Tween.Type.REVERSE)
				return;

			String actorId = event.getData().getName();

			EngineLogger.debug("Spine event " + event.getInt() + ":" + actorId + "." + event.getString());

			InteractiveActor actor = (InteractiveActor) world.getCurrentScene().getActor(actorId, true);

			switch (event.getInt()) {
			case PLAY_ANIMATION_EVENT:
				if (actor == null) {
					EngineLogger.debug("Actor in Spine animation event not found in scene: " + actorId);
					return;
				}

				((SpriteActor) actor).startAnimation(event.getString(), null);
				break;
			case PLAY_SOUND_EVENT:
				// Backwards compatibility
				String sid = event.getString();
				if (world.getSounds().get(sid) == null && actor != null)
					sid = actor.getId() + "_" + sid;

				world.getCurrentScene().getSoundManager().playSound(sid);
				break;
			case RUN_VERB_EVENT:
				if (actor != null)
					actor.runVerb(event.getString());
				else
					world.getCurrentScene().runVerb(event.getString());
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
			if (dependencies.size > 0)
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

			// keep updating secondary animation
			// WARNING: It doesn't work with REVERSE ANIMATION
			if (secondaryAnimation != null && currentSource != null
					&& (!((SkeletonCacheEntry) currentSource).animation.getTracks().get(1).isComplete()
							|| ((SkeletonCacheEntry) currentSource).animation.getTracks().get(1).getLoop()))
				updateAnimation(delta);

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
	public void draw(SpriteBatch batch, float x, float y, float scaleX, float scaleY, float rotation, Color tint) {

		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

		if (cs != null && cs.skeleton != null) {
			Matrix4 tm = batch.getTransformMatrix();
			tmp.set(tm);

			float originX = cs.skeleton.getRootBone().getX();
			float originY = cs.skeleton.getRootBone().getY();
			tm.translate(x, y, 0).rotate(0, 0, 1, rotation).scale(scaleX, scaleY, 1).translate(originX, originY, 0);

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

			RectangleRenderer.draw(batch, x + dx * scaleX, y + dy * scaleY, getWidth() * scaleX, getHeight() * scaleY,
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

	public String getSkin() {
		return skin;
	}

	public void setSkin(String skin) {
		// set the skin if the current source is loaded
		if (currentSource != null && currentSource.refCounter > 0) {
			SkeletonCacheEntry sce = (SkeletonCacheEntry) currentSource;

			EngineLogger.debug("Setting Spine skin: " + skin);

			if (skin != null) {
				SkeletonData skeletonData = sce.skeleton.getData();

				if (skin.indexOf(',') == -1 || skeletonData.findSkin(skin) != null) {
					sce.skeleton.setSkin(skin);
				} else {
					// we can combine several skins separated by ','
					String[] skins = skin.split(",");

					Skin combinedSkin = new Skin(skin);

					for (String sk : skins) {

						// Get the source skins.
						Skin singleSkin = skeletonData.findSkin(sk.trim());
						combinedSkin.addSkin(singleSkin);
					}

					// Set and apply the Skin to the skeleton.
					sce.skeleton.setSkin(combinedSkin);
				}

			} else {
				sce.skeleton.setSkin((Skin) null);
			}

			// sce.skeleton.setSlotsToSetupPose();
		}

		this.skin = skin;
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
		startAnimation(id, repeatType, count, cb, getDirectionString(p0, pf, getDirs(id, fanims)));
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

	public void setSecondaryAnimation(String animation) {
		secondaryAnimation = animation;
		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

		try {

			if (animation == null) {
				cs.animation.setEmptyAnimation(1, 0);
				// cs.animation.clearTrack(1);
			} else {

				SpineAnimationDesc fa = (SpineAnimationDesc) fanims.get(animation);

				if (fa == null) {
					EngineLogger.error("SpineRenderer:setSecondaryAnimation Animation not found: " + animation);
					return;
				}

				cs.animation.setAnimation(1, secondaryAnimation, fa.animationType == Tween.Type.REPEAT);
			}

			updateAnimation(0);
		} catch (Exception e) {
			EngineLogger.error("Error in SpineRenderer::setSecondaryAnimation", e);
		}
	}

	public Skeleton getCurrentSkeleton() {
		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;
		return cs.skeleton;
	}

	public AnimationState getCurrentAnimationState() {
		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;
		return cs.animation;
	}

	private void setCurrentAnimation() {
		try {
			SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

			if(cs.skeleton == null) {
				return;
			}

			if (skin != null && (cs.skeleton.getSkin() == null || !skin.equals(cs.skeleton.getSkin().getName()))) {
				setSkin(skin);
			}

			cs.skeleton.setToSetupPose();
			cs.skeleton.setScaleX(flipX ? -1 : 1);
			cs.animation.setTimeScale(currentAnimation.duration);
			cs.animation.clearTracks();
			cs.animation.setAnimation(0, currentAnimation.id, currentAnimationType == Tween.Type.REPEAT);

			if (secondaryAnimation != null)
				setSecondaryAnimation(secondaryAnimation);

			updateAnimation(lastAnimationTime);
			computeBbox();

		} catch (Exception e) {
			EngineLogger.error("Error in SpineRenderer::setCurrentAnimation", e);
		}
	}

	@Override
	public void computeBbox() {
		float minX, minY, maxX, maxY;

		if (bbox == null)
			bbox = new Polygon(new float[8]);

		if (bbox != null && (bbox.getVertices() == null || bbox.getVertices().length != 8)) {
			bbox.setVertices(new float[8]);
		}

		SkeletonCacheEntry cs = (SkeletonCacheEntry) currentSource;

		if (cs == null || cs.skeleton == null) {

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
			return;
		}

		cs.skeleton.setPosition(0, 0);
		cs.skeleton.updateWorldTransform();
		bounds.update(cs.skeleton, true);

		if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
			// if there is only one bbox, get the polygon, else get the rectangle bbox
			// (union of all bboxes).
			if (bounds.getPolygons().size == 1) {
				FloatArray p = bounds.getPolygons().get(0);

				bbox.setVertices(p.toArray());
				bbox.dirty();
				Rectangle boundingRectangle = bbox.getBoundingRectangle();
				width = boundingRectangle.getWidth();
				height = boundingRectangle.getHeight();
				return;

			} else {
				width = bounds.getWidth();
				height = bounds.getHeight();
				minX = bounds.getMinX();
				minY = bounds.getMinY();
				maxX = bounds.getMaxX();
				maxY = bounds.getMaxY();
			}

		} else {

			Vector2 offset = new Vector2();
			Vector2 size = new Vector2();
			cs.skeleton.getBounds(offset, size, new FloatArray());
			width = size.x;
			height = size.y;

			minX = offset.x;
			minY = offset.y;
			maxX = offset.x + width;
			maxY = offset.y + height;
		}

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
			EngineAssetManager.getInstance().load(getFileName(source), SkeletonData.class, parameter);
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
			SkeletonData skeletonData = EngineAssetManager.getInstance().get(getFileName(source), SkeletonData.class);

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
		renderer = new SkeletonRenderer();
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
	}

	@Override
	public void dispose() {
		for (Entry<String, CacheEntry> entry : sourceCache.entrySet()) {

			if (entry.getValue().refCounter > 0) {
				String filename = EngineAssetManager.SPINE_DIR + entry.getKey() + EngineAssetManager.SPINE_EXT;

				if (EngineAssetManager.getInstance().isLoaded(filename))
					EngineAssetManager.getInstance().unload(filename);
			}
		}

		sourceCache.clear();
		currentSource = null;
		renderer = null;
		bounds = null;
	}

	@Override
	public void write(Json json) {
		super.write(json);

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {

		} else {

			if (animationCb != null) {
				json.writeValue("cb",
						ActionCallbackSerializer.serialize(bjson.getWorld(), bjson.getScene(), animationCb));
			}

			json.writeValue("currentCount", currentCount);

			if (currentAnimation != null)
				json.writeValue("currentAnimationType", currentAnimationType);

			json.writeValue("lastAnimationTime", lastAnimationTime);
			json.writeValue("complete", complete);
			json.writeValue("loopCount", loopCount);
			json.writeValue("secondaryAnimation", secondaryAnimation);
		}

		json.writeValue("skin", skin);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			fanims = json.readValue("fanims", HashMap.class, SpineAnimationDesc.class, jsonData);

			world = bjson.getWorld();
		} else {

			animationCb = ActionCallbackSerializer.find(((BladeJson) json).getWorld(), ((BladeJson) json).getScene(),
					json.readValue("cb", String.class, jsonData));

			currentCount = json.readValue("currentCount", Integer.class, jsonData);

			if (currentAnimation != null)
				currentAnimationType = json.readValue("currentAnimationType", Tween.Type.class, jsonData);

			lastAnimationTime = json.readValue("lastAnimationTime", Float.class, jsonData);
			complete = json.readValue("complete", Boolean.class, jsonData);
			loopCount = json.readValue("loopCount", int.class, loopCount, jsonData);

			secondaryAnimation = json.readValue("secondaryAnimation", String.class, (String) null, jsonData);
		}

		skin = json.readValue("skin", String.class, skin, jsonData);
	}
}
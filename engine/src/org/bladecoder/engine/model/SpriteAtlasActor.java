package org.bladecoder.engine.model;

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.SpriteFATween;
import org.bladecoder.engine.anim.TweenManagerSingleton;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.EngineLogger;

import aurelienribon.tweenengine.TweenManager;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SpriteAtlasActor extends SpriteActor {

	private HashMap<String, FrameAnimation> fanims = new HashMap<String, FrameAnimation>();

	private int currentFrame = 0;
	private FrameAnimation currentFrameAnimation;

	/**
	 * When the atlas is loaded for the current FA this var keeps track of it
	 * for freeing when FA is changed
	 */
	private String notPreloadedAtlas;

	@Override
	public void update(float delta) {
		// TODO Auto-generated method stub
	}

	public FrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	public HashMap<String, FrameAnimation> getFrameAnimations() {
		return fanims;
	}

	private AtlasRegion getCurrentRegion() {
		return currentFrameAnimation.regions.get(currentFrame);
	}

	public void setCurrentFrameAnimation(FrameAnimation fa) {
		// EngineLogger.debug(getId() + ": Setting FrameAnimation " + id);

		if (currentFrameAnimation != null) {
			if (currentFrameAnimation.sound != null) {
				stopSound(currentFrameAnimation.sound);
			}

			if (fa.outD != null) {
				float s = EngineAssetManager.getInstance().getScale();

				pos.x += currentFrameAnimation.outD.x * s;
				pos.y += currentFrameAnimation.outD.y * s;
			}
		}

		currentFrameAnimation = fa;

		if (fa.inD != null) {
			float s = EngineAssetManager.getInstance().getScale();
			pos.x += fa.inD.x * s;
			pos.y += fa.inD.y * s;
		}
	}

	public void startFrameAnimation(String id, int repeatType, int count,
			boolean reverse, ActionCallback cb) {

		FrameAnimation fa = getFrameAnimation(id);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}

		setCurrentFrameAnimation(fa);
		startCurrentFrameAnimation(repeatType, count, reverse, cb);
	}

	public void setCurrentFrame(int f) {
		currentFrame = f;

		tex = getCurrentRegion();
	}

	public int getCurrentFrame() {
		return currentFrame;
	}

	public int getNumFrames() {
		return currentFrameAnimation.regions.size;
	}

	private void startCurrentFrameAnimation(int repeatType, int count,
			boolean reverse, ActionCallback cb) {

		if (currentFrameAnimation.sound != null) {
			playSound(currentFrameAnimation.sound);
		}

		// free not pre loaded in scene atlas
		if (notPreloadedAtlas != null
				&& !currentFrameAnimation.atlas.equals(notPreloadedAtlas)) {
			EngineAssetManager.getInstance().disposeAtlas(notPreloadedAtlas);
			notPreloadedAtlas = null;
		}

		// If the atlas is not loaded. Try to load it.
		if (currentFrameAnimation != null
				&& currentFrameAnimation.regions == null) {
			retrieveFA(currentFrameAnimation.id, true);
		}

		if (currentFrameAnimation == null
				|| currentFrameAnimation.regions == null) {
			if (bbox == null) {
				bbox = new Rectangle(pos.x - 50, pos.y, 100, 100);
			}

			tex = null;

			return;
		}

		TweenManager manager = TweenManagerSingleton.getInstance();

		manager.killTarget(this, EngineTween.SPRITE_FA_TYPE);

		if (!reverse)
			setCurrentFrame(0);
		else
			setCurrentFrame(getNumFrames() - 1);

		if (currentFrameAnimation.regions.size <= 1
				|| currentFrameAnimation.speed == 0.0) {
			if (cb != null) {
				cb.onEvent();
			}

			return;
		}
		
		SpriteFATween t = new SpriteFATween();
		
		t.start(this, repeatType, count, reverse, cb);

		manager.add(t);
	}

	public String getFlipId(String id) {
		StringBuilder sb = new StringBuilder();

		if (id.endsWith("left")) {
			sb.append(id.substring(0, id.length() - 4));
			sb.append("right");
		} else if (id.endsWith("right")) {
			sb.append(id.substring(0, id.length() - 5));
			sb.append("left");
		}

		return sb.toString();
	}

	public String getCurrentFrameAnimationId() {
		String id = currentFrameAnimation.id;

		if (flipX) {
			id = getFlipId(id);
		}

		return id;

	}

	public void addFrameAnimation(FrameAnimation fa) {
		if (initFrameAnimation == null)
			initFrameAnimation = fa.id;

		fanims.put(fa.id, fa);
	}

	@Override
	public void retrieveAssets() {
		super.retrieveAssets();

		for (FrameAnimation fa : fanims.values()) {
			retrieveFA(fa.id, fa == currentFrameAnimation);
		}

		if (currentFrameAnimation == null && initFrameAnimation != null) {
			startFrameAnimation(initFrameAnimation, null);
		} else {
			tex = getCurrentRegion();
		}
	}

	private void retrieveFA(String faId, boolean loadAtlas) {
		FrameAnimation fa = fanims.get(faId);

		if (!EngineAssetManager.getInstance().isAtlasLoaded(fa.atlas)) {

			if (loadAtlas) {
				EngineLogger
						.debug(MessageFormat
								.format("{0} ATLAS NOT LOADED. Needed for Sprite {1}.LOADING...",
										fa.atlas, fa.id));
				EngineAssetManager.getInstance().loadAtlas(fa.atlas);
				notPreloadedAtlas = fa.atlas;
				EngineAssetManager.getInstance().getManager().finishLoading();
			} else {
				EngineLogger
						.debug(MessageFormat
								.format("{0} ATLAS NOT LOADED. Needed for Sprite {1}.NOT LOADING...",
										fa.atlas, fa.id));
				return;
			}
		}

		fa.regions = EngineAssetManager.getInstance().getRegions(fa.atlas,
				fa.id);

		if (fa.regions == null || fa.regions.size == 0) {
			EngineLogger.error(fa.id + " has no regions in ATLAS " + fa.atlas);
			fanims.remove(fa.id);
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("  Sprite Bbox: ").append(getBBox().toString());

		sb.append("\n  Anims:");

		for (String v : fanims.keySet()) {
			sb.append(" ").append(v);
		}

		sb.append("\n  Current Anim: ").append(currentFrameAnimation.id);
		sb.append("  Current Frame: ").append(currentFrame);

		sb.append("\n");

		return sb.toString();
	}

	public FrameAnimation getFrameAnimation(String id) {
		FrameAnimation fa = fanims.get(id);
		flipX = false;

		if (fa == null) {
			// Search for flipped
			String flipId = getFlipId(id);

			fa = fanims.get(flipId);

			if (fa != null)
				flipX = true;
			else {
				// search for .left if .frontleft not found and viceversa
				StringBuilder sb = new StringBuilder();

				if (id.endsWith(SpriteActor.LEFT)) {
					sb.append(id.substring(0, id.length() - 4));
					sb.append("frontleft");
				} else if (id.endsWith(SpriteActor.FRONTLEFT)) {
					sb.append(id.substring(0, id.length() - 9));
					sb.append("left");
				} else if (id.endsWith(SpriteActor.RIGHT)) {
					sb.append(id.substring(0, id.length() - 5));
					sb.append("frontright");
				} else if (id.endsWith(SpriteActor.FRONTRIGHT)) {
					sb.append(id.substring(0, id.length() - 10));
					sb.append("right");
				}

				String s = sb.toString();

				fa = fanims.get(s);

				if (fa == null) {
					// Search for flipped
					flipId = getFlipId(s);

					fa = fanims.get(flipId);

					if (fa != null)
						flipX = true;
				}
			}
		}

		return fa;
	}

	@Override
	public void lookat(Vector2 p) {
		lookat(getFrameDirection(getPosition(), p));
	}

	@Override
	public void lookat(String dir) {
		StringBuilder sb = new StringBuilder();
		sb.append(STAND_ANIM);
		sb.append('.');
		sb.append(dir);

		TweenManagerSingleton.getInstance().killTarget(this,
				EngineTween.SPRITE_POS_TYPE);
		startFrameAnimation(sb.toString(), null);
	}

	@Override
	public void stand() {
		String standFA = STAND_ANIM;
		int idx = getCurrentFrameAnimationId().indexOf('.');

		if (idx != -1) {
			standFA += getCurrentFrameAnimationId().substring(idx);
		}

		startFrameAnimation(standFA, null);
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		String currentDirection = getFrameDirection(p0, pf);
		StringBuilder sb = new StringBuilder();
		sb.append(WALK_ANIM).append('.').append(currentDirection);
		startFrameAnimation(sb.toString(), null);
	}

	private final static float DIRECTION_ASPECT_TOLERANCE = 2.5f;

	private String getFrameDirection(Vector2 p0, Vector2 pf) {
		float dx = pf.x - p0.x;
		float dy = pf.y - p0.y;
		float ratio = Math.abs(dx / dy);

		if (ratio < 1.0)
			ratio = 1.0f / ratio;

		// EngineLogger.debug("P0: " + p0 + " PF: " + pf + " dx: " + dx +
		// " dy: "
		// + dy + " RATIO: " + ratio);

		if (ratio < DIRECTION_ASPECT_TOLERANCE) { // DIAGONAL MOVEMENT
			if (dy > 0) { // UP. MOVEMENT
				if (dx > 0) { // TO THE RIGHT
					return BACKRIGHT;
				} else { // TO THE LEFT
					return BACKLEFT;
				}

			} else { // DOWN. MOVEMENT
				if (dx > 0) { // TO THE RIGHT
					return FRONTRIGHT;
				} else { // TO THE LEFT
					return FRONTLEFT;
				}
			}
		} else { // HOR OR VERT MOVEMENT
			if (Math.abs(dx) > Math.abs(dy)) { // HOR. MOVEMENT
				if (dx > 0) { // TO THE RIGHT
					return RIGHT;
				} else { // TO THE LEFT
					return LEFT;
				}

			} else { // VERT. MOVEMENT
				if (dy > 0) { // TO THE TOP
					return BACK;
				} else { // TO THE BOTTOM
					return FRONT;
				}
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();

		// free not pre loaded atlas in scene
		if (notPreloadedAtlas != null) {
			EngineAssetManager.getInstance().disposeAtlas(notPreloadedAtlas);
			notPreloadedAtlas = null;
		}
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("fanims", fanims);
		json.writeValue("currentFrame", currentFrame);

		String currentFrameAnimationId = null;

		if (currentFrameAnimation != null)
			currentFrameAnimationId = currentFrameAnimation.id;

		json.writeValue("currentFrameAnimation", currentFrameAnimationId,
				currentFrameAnimationId == null ? null : String.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		fanims = json.readValue("fanims", HashMap.class, FrameAnimation.class,
				jsonData);
		currentFrame = json.readValue("currentFrame", Integer.class, jsonData);

		String currentFrameAnimationId = json.readValue(
				"currentFrameAnimation", String.class, jsonData);

		if (currentFrameAnimationId != null)
			currentFrameAnimation = fanims.get(currentFrameAnimationId);
	}
}
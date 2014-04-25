package org.bladecoder.engine.model;

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.TweenManagerSingleton;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.ActionCallbackSerialization;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SpriteAtlasRenderer implements SpriteRenderer {

	private HashMap<String, AtlasFrameAnimation> fanims = new HashMap<String, AtlasFrameAnimation>();

	private AtlasFrameAnimation currentFrameAnimation;
	private Animation currentAnimation;
	private float animationTime;
	private ActionCallback animationCb = null;
	private String animationCbSer = null;

	/**
	 * When the atlas is loaded for the current FA this var keeps track of it
	 * for freeing when FA is changed
	 */
	private String notPreloadedAtlas;
		
	private TextureRegion tex;
	private boolean flipX;

	@Override
	public void update(float delta) {
		if(currentAnimation != null) {
			animationTime += delta;
			tex = currentAnimation.getKeyFrame(animationTime);
			
			if(currentAnimation.isAnimationFinished(animationTime) && (animationCb != null || animationCbSer != null)) {
				
				if (animationCb == null) {
					animationCb = ActionCallbackSerialization.find(animationCbSer);
					animationCbSer = null;
				}
				
				ActionCallback cb2 = animationCb;
				animationCb = null;
				cb2.onEvent();
			}
		}
	}
	
	@Override
	public void draw(SpriteBatch batch, float x, float y, float originX,
			float originY, float scale) {
		
		if(tex == null) {
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight() * scale, Color.RED);
		}
		
		if (!flipX) {
			batch.draw(tex, x, y, originX, originY, tex.getRegionWidth(),
				tex.getRegionHeight(), scale, scale, 0);
		} else {
			batch.draw(tex, x +  tex.getRegionWidth() * scale, y, originX, originY, -tex.getRegionWidth(),
					tex.getRegionHeight(), scale, scale, 0);
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

	public AtlasFrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	public HashMap<String, AtlasFrameAnimation> getFrameAnimations() {
		return fanims;
	}


	public void startFrameAnimation(String id, int repeatType, int count, ActionCallback cb) {

		AtlasFrameAnimation fa = getFrameAnimation(id);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}

		currentFrameAnimation = fa;
		
		startCurrentFrameAnimation(repeatType, count, cb);
	}

	public int getNumFrames() {
		return currentFrameAnimation.regions.size;
	}

	private void startCurrentFrameAnimation(int repeatType, int count, ActionCallback cb) {

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

			tex = null;

			return;
		}
		
		currentAnimation = null;
		animationTime = 0;
		animationCb = cb;

		if (currentFrameAnimation.regions.size <= 1
				|| currentFrameAnimation.duration == 0.0) {
			
			tex = currentFrameAnimation.regions.first();
			
			if (cb != null) {
				cb.onEvent();
			}

			return;
		}

		if(repeatType != EngineTween.FROM_FA) {
			currentFrameAnimation.animationType = repeatType;
			currentFrameAnimation.count = count;
		}
		
		newCurrentAnimation(currentFrameAnimation.animationType, count);
	}
	
	private void newCurrentAnimation(int repeatType, int count) {
		PlayMode animationType = Animation.PlayMode.NORMAL;
		// TODO: ADD COUNT SUPPORT
		
		switch(repeatType) {
		case EngineTween.REPEAT:
			animationType = Animation.PlayMode.LOOP;
			break;
		case EngineTween.YOYO:
			animationType = Animation.PlayMode.LOOP_PINGPONG;
			break;
		case EngineTween.REVERSE:
			animationType = Animation.PlayMode.REVERSED;
			break;
		case EngineTween.REVERSE_REPEAT:
			animationType = Animation.PlayMode.LOOP_REVERSED;
			break;
		}		
		
		currentAnimation = new Animation(currentFrameAnimation.duration / getNumFrames(),
				currentFrameAnimation.regions, animationType);
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
		if(currentFrameAnimation==null)
			return null;
		
		String id = currentFrameAnimation.id;

		if (flipX) {
			id = getFlipId(id);
		}

		return id;

	}

	public void addFrameAnimation(AtlasFrameAnimation fa) {
		fanims.put(fa.id, fa);
	}

	private void retrieveFA(String faId, boolean loadAtlas) {
		AtlasFrameAnimation fa = fanims.get(faId);

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

		sb.append("\n  Anims:");

		for (String v : fanims.keySet()) {
			sb.append(" ").append(v);
		}

		sb.append("\n  Current Anim: ").append(currentFrameAnimation.id);

		sb.append("\n");

		return sb.toString();
	}

	public AtlasFrameAnimation getFrameAnimation(String id) {
		AtlasFrameAnimation fa = fanims.get(id);
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

				if (id.endsWith(SpriteRenderer.LEFT)) {
					sb.append(id.substring(0, id.length() - 4));
					sb.append("frontleft");
				} else if (id.endsWith(SpriteRenderer.FRONTLEFT)) {
					sb.append(id.substring(0, id.length() - 9));
					sb.append("left");
				} else if (id.endsWith(SpriteRenderer.RIGHT)) {
					sb.append(id.substring(0, id.length() - 5));
					sb.append("frontright");
				} else if (id.endsWith(SpriteRenderer.FRONTRIGHT)) {
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
	public void lookat(Vector2 p0, Vector2 pf) {
		lookat(getFrameDirection(p0, pf));
	}

	@Override
	public void lookat(String dir) {
		StringBuilder sb = new StringBuilder();
		sb.append(STAND_ANIM);
		sb.append('.');
		sb.append(dir);

		TweenManagerSingleton.getInstance().killTarget(this,
				EngineTween.SPRITE_POS_TYPE);
		startFrameAnimation(sb.toString(), EngineTween.FROM_FA, 1, null);
	}

	@Override
	public void stand() {
		String standFA = STAND_ANIM;
		int idx = getCurrentFrameAnimationId().indexOf('.');

		if (idx != -1) {
			standFA += getCurrentFrameAnimationId().substring(idx);
		}

		startFrameAnimation(standFA, EngineTween.FROM_FA, 1, null);
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		String currentDirection = getFrameDirection(p0, pf);
		StringBuilder sb = new StringBuilder();
		sb.append(WALK_ANIM).append('.').append(currentDirection);
		startFrameAnimation(sb.toString(), EngineTween.FROM_FA, 1, null);
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
	public void loadAssets() {
		// SpriteAtlas are preloaded by Scene
		// so it is not necessary to load the atlas here		
	}
	
	@Override
	public void retrieveAssets() {

		for (AtlasFrameAnimation fa : fanims.values()) {
			retrieveFA(fa.id, fa == currentFrameAnimation);
		}
		
		if(currentFrameAnimation != null) {
			
			if(currentFrameAnimation.regions.size == 1)
				tex = currentFrameAnimation.regions.first();
			else {
				newCurrentAnimation(currentFrameAnimation.animationType, currentFrameAnimation.count);
				tex = currentAnimation.getKeyFrame(animationTime);
			}
			
		} else if(fanims.size() == 1) {
			String f = fanims.values().iterator().next().id;
			startFrameAnimation(f,  EngineTween.FROM_FA, 1, null);
		}
	}

	@Override
	public void dispose() {
		// free not pre-loaded atlas in scene
		if (notPreloadedAtlas != null) {
			EngineAssetManager.getInstance().disposeAtlas(notPreloadedAtlas);
			notPreloadedAtlas = null;
		}
	}

	@Override
	public void write(Json json) {

		json.writeValue("fanims", fanims);

		String currentFrameAnimationId = null;

		if (currentFrameAnimation != null)
			currentFrameAnimationId = currentFrameAnimation.id;

		json.writeValue("currentFrameAnimation", currentFrameAnimationId,
				currentFrameAnimationId == null ? null : String.class);
		
		json.writeValue("flipX", flipX);
				
		json.writeValue("animationTime", animationTime);
		json.writeValue("animationCb",
				ActionCallbackSerialization.find(animationCb),
				animationCb == null ? null : String.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		fanims = json.readValue("fanims", HashMap.class, AtlasFrameAnimation.class,
				jsonData);

		String currentFrameAnimationId = json.readValue(
				"currentFrameAnimation", String.class, jsonData);

		if (currentFrameAnimationId != null)
			currentFrameAnimation = fanims.get(currentFrameAnimationId);
		
		flipX = json.readValue("flipX", Boolean.class, jsonData);
		
		animationTime = json.readValue("animationTime", Float.class, jsonData);		
		animationCbSer = json.readValue("animationCb", String.class, jsonData);
	}
}
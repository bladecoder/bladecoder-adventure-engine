package org.bladecoder.engine.model;

import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.anim.AtlasFrameAnimation;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.FrameAnimation;
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
	
	/** Starts this anim the first time that the scene is loaded */
	private String initFrameAnimation;

	private AtlasFrameAnimation currentFrameAnimation;
	
	private Animation animation;
	private float animationTime;
	private ActionCallback animationCb = null;
	private String animationCbSer = null;
	
	private int currentCount;
	private int currentAnimationType;

	private TextureRegion tex;
	private boolean flipX;
	

	@Override
	public void setInitFrameAnimation(String fa) {
		initFrameAnimation = fa;
	}
	
	@Override
	public String getInitFrameAnimation() {
		return initFrameAnimation;
	}

	private boolean isAnimationFinished() {
		if (currentAnimationType == EngineTween.REPEAT
				|| currentAnimationType == EngineTween.REVERSE_REPEAT ||
						currentAnimationType == EngineTween.YOYO) {
			if (currentCount > 0
					&& animationTime > currentFrameAnimation.duration
							* currentCount)
				return true;
		} else if (animationTime > currentFrameAnimation.duration) {
			return true;
		}

		return false;
	}

	@Override
	public void update(float delta) {
		if (animation != null) {
			animationTime += delta;
			tex = animation.getKeyFrame(animationTime);

			if (isAnimationFinished()) {
				
				animation = null;

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
		}
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float originX,
			float originY, float scale) {

		if (tex == null) {
			RectangleRenderer.draw(batch, x, y, getWidth() * scale, getHeight()
					* scale, Color.RED);
			return;
		}

		if (!flipX) {
			batch.draw(tex, x, y, originX, originY, tex.getRegionWidth(),
					tex.getRegionHeight(), scale, scale, 0);
		} else {
			batch.draw(tex, x + tex.getRegionWidth() * scale, y, originX,
					originY, -tex.getRegionWidth(), tex.getRegionHeight(),
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

	public AtlasFrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	public HashMap<String, AtlasFrameAnimation> getFrameAnimations() {
		return fanims;
	}

	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		AtlasFrameAnimation fa = getFrameAnimation(id);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}
		
		if(currentFrameAnimation != null && currentFrameAnimation.disposeWhenPlayed)
			currentFrameAnimation.dispose();

		currentFrameAnimation = fa;

		animationCb = cb;
		animation = null;
		animationTime = 0;

		// If the atlas is not loaded. Load it.
		if (currentFrameAnimation != null
				&& currentFrameAnimation.regions == null) {
			currentFrameAnimation.loadAssets();
			EngineAssetManager.getInstance().getManager().finishLoading();

			currentFrameAnimation.retrieveAssets();

			if (currentFrameAnimation.regions == null || currentFrameAnimation.regions.size == 0) {
				EngineLogger.error(currentFrameAnimation.id + " has no regions in ATLAS " + currentFrameAnimation.source);
				fanims.remove(currentFrameAnimation.id);
			}
		}

		if (currentFrameAnimation == null) {

			tex = null;

			return;
		}

		if (currentFrameAnimation.regions.size == 1
				|| currentFrameAnimation.duration == 0.0) {

			tex = currentFrameAnimation.regions.first();

			if (cb != null) {
				ActionCallbackQueue.add(cb);
			}

			return;
		}

		if (repeatType == EngineTween.FROM_FA) {
			currentAnimationType = currentFrameAnimation.animationType;
			currentCount = currentFrameAnimation.count;
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		newCurrentAnimation(currentAnimationType, currentCount);
	}

	public int getNumFrames() {
		return currentFrameAnimation.regions.size;
	}

	private void newCurrentAnimation(int repeatType, int count) {
		PlayMode animationType = Animation.PlayMode.NORMAL;

		switch (repeatType) {
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

		// TODO NO CREATE NEW INSTANCES OF ANIMATION, ONLY 1 INSTACE
		animation = new Animation(currentFrameAnimation.duration
				/ getNumFrames(), currentFrameAnimation.regions, animationType);
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
	public void addFrameAnimation(FrameAnimation fa) {
		if(initFrameAnimation == null)
			initFrameAnimation = fa.id; 
			
		fanims.put(fa.id, (AtlasFrameAnimation)fa);
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

	private AtlasFrameAnimation getFrameAnimation(String id) {
		AtlasFrameAnimation fa = fanims.get(id);
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

				if (id.endsWith(FrameAnimation.LEFT)) {
					sb.append(id.substring(0, id.length() - 4));
					sb.append("frontleft");
				} else if (id.endsWith(FrameAnimation.FRONTLEFT)) {
					sb.append(id.substring(0, id.length() - 9));
					sb.append("left");
				} else if (id.endsWith(FrameAnimation.RIGHT)) {
					sb.append(id.substring(0, id.length() - 5));
					sb.append("frontright");
				} else if (id.endsWith(FrameAnimation.FRONTRIGHT)) {
					sb.append(id.substring(0, id.length() - 10));
					sb.append("right");
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

	@Override
	public void lookat(Vector2 p0, Vector2 pf) {
		lookat(FrameAnimation.getFrameDirection(p0, pf));
	}

	@Override
	public void lookat(String dir) {
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.STAND_ANIM);
		sb.append('.');
		sb.append(dir);

		startFrameAnimation(sb.toString(), EngineTween.FROM_FA, 1, null);
	}

	@Override
	public void stand() {
		String standFA = FrameAnimation.STAND_ANIM;
		int idx = getCurrentFrameAnimationId().indexOf('.');

		if (idx != -1) {
			standFA += getCurrentFrameAnimationId().substring(idx);
		}

		startFrameAnimation(standFA, EngineTween.FROM_FA, 1, null);
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		String currentDirection = FrameAnimation.getFrameDirection(p0, pf);
		StringBuilder sb = new StringBuilder();
		sb.append(FrameAnimation.WALK_ANIM).append('.').append(currentDirection);
		startFrameAnimation(sb.toString(), EngineTween.FROM_FA, 1, null);
	}



	@Override
	public void loadAssets() {
		for (AtlasFrameAnimation fa : fanims.values()) {
			if(fa.preload)
				fa.loadAssets();
		}
		
		if(currentFrameAnimation != null && !currentFrameAnimation.preload) {
			currentFrameAnimation.loadAssets();
		} else if(currentFrameAnimation == null && initFrameAnimation != null) {
			AtlasFrameAnimation fa = fanims.get(initFrameAnimation);
			
			if(!fa.preload)
				fa.loadAssets();		
		}
	}

	@Override
	public void retrieveAssets() {
		for (AtlasFrameAnimation fa : fanims.values()) {
			if(fa.preload)
				fa.retrieveAssets();
		}
		
		if(currentFrameAnimation != null && !currentFrameAnimation.preload) {
			currentFrameAnimation.retrieveAssets();
		} else if(currentFrameAnimation == null && initFrameAnimation != null) {
			AtlasFrameAnimation fa = fanims.get(initFrameAnimation);
			
			if(!fa.preload)
				fa.retrieveAssets();		
		}

		if (currentFrameAnimation != null) {
			
			if (currentFrameAnimation.regions.size == 1)
				tex = currentFrameAnimation.regions.first();
			else { 	// TODO Restore previous animation state
				newCurrentAnimation(currentAnimationType,
						currentCount);
				
				tex = animation.getKeyFrame(animationTime);
			}
		} else {
			startFrameAnimation(initFrameAnimation, EngineTween.FROM_FA, 1, null);
		}
	}

	@Override
	public void dispose() {
		for (AtlasFrameAnimation fa : fanims.values()) {
			fa.dispose();
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
		
		json.writeValue("initFrameAnimation", initFrameAnimation);

		json.writeValue("flipX", flipX);
		json.writeValue("currentCount", currentCount);
		json.writeValue("currentAnimationType", currentAnimationType);

		json.writeValue("animationTime", animationTime);
		json.writeValue("animationCb",
				ActionCallbackSerialization.find(animationCb),
				animationCb == null ? null : String.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		fanims = json.readValue("fanims", HashMap.class,
				AtlasFrameAnimation.class, jsonData);

		String currentFrameAnimationId = json.readValue(
				"currentFrameAnimation", String.class, jsonData);

		if (currentFrameAnimationId != null)
			currentFrameAnimation = fanims.get(currentFrameAnimationId);
		
		initFrameAnimation = json.readValue("initFrameAnimation", String.class,
				jsonData);

		flipX = json.readValue("flipX", Boolean.class, jsonData);
		currentCount = json.readValue("currentCount", Integer.class, jsonData);
		currentAnimationType = json.readValue("currentAnimationType", Integer.class, jsonData);

		animationTime = json.readValue("animationTime", Float.class, jsonData);
		animationCbSer = json.readValue("animationCb", String.class, jsonData);
	}
}
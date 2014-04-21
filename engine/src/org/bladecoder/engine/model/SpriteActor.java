package org.bladecoder.engine.model;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.SpritePosTween;
import org.bladecoder.engine.anim.TweenManagerSingleton;
import org.bladecoder.engine.anim.WalkTween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import aurelienribon.tweenengine.TweenManager;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class SpriteActor extends BaseActor {

	public final static String BACK = "back";
	public final static String FRONT = "front";
	public final static String RIGHT = "right";
	public final static String LEFT = "left";
	public final static String BACKRIGHT = "backright";
	public final static String BACKLEFT = "backleft";
	public final static String FRONTRIGHT = "frontright";
	public final static String FRONTLEFT = "frontleft";
	public final static String STAND_ANIM = "stand";
	public final static String WALK_ANIM = "walk";
	public final static String TALK_ANIM = "talk";

	private final static float DEFAULT_WALKING_SPEED = 700f; // Speed units:
																// pix/sec.

	public static enum DepthType {
		NONE, MAP, VECTOR
	};

	protected Vector2 pos = new Vector2();
	protected float scale = 1.0f;
	protected boolean flipX;

	/** Starts this anim the first time that the scene is loaded */
	protected String initFrameAnimation;

	/** The texture to draw */
	protected TextureRegion tex = null;

	/** Scale sprite acording to the scene depth map */
	private DepthType depthType = DepthType.NONE;
	protected Scene scene = null;

	private float walkingSpeed = DEFAULT_WALKING_SPEED;

	public void setWalkingSpeed(float s) {
		walkingSpeed = s;
	}

	public void setScene(Scene s) {
		scene = s;
	}

	public DepthType getDepthType() {
		return depthType;
	}

	public void setDepthType(DepthType v) {
		depthType = v;
	}

	public void setInitFrameAnimation(String initFA) {
		initFrameAnimation = initFA;
	}

	public String getInitFrameAnimation() {
		return initFrameAnimation;
	}

	public void setPosition(float x, float y) {

		pos.x = x;
		pos.y = y;

		if (scene != null) {

			if (depthType == DepthType.MAP) {
				float depth = scene.getBackgroundMap().getDepth(x, y);

				if (depth != 0)
					setScale(depth);
			} else if (depthType == DepthType.VECTOR
					&& scene.getDepthVector() != null) {
				Vector2 depth = scene.getDepthVector();

				// interpolation equation
				float s = Math.abs(depth.x + (depth.y - depth.x) * y
						/ scene.getBBox().height);

				if (s != 0)
					setScale(s);
			}

			if (scene.getCameraFollowActor() == this)
				scene.getCamera().updatePos(this);

		}

	}

	public Vector2 getPosition() {
		return pos;
	}

	public float getWidth() {
		if (tex == null)
			return 200;

		return tex.getRegionWidth() * scale;
	}

	public float getHeight() {
		if (tex == null)
			return 200;

		return tex.getRegionHeight() * scale;
	}

	@Override
	public Rectangle getBBox() {
		if (bbox != null)
			return new Rectangle(pos.x + bbox.x, pos.y + bbox.y, bbox.width
					* scale, bbox.height * scale);
		else
			return new Rectangle(pos.x - getWidth() / 2, pos.y, getWidth(),
					getHeight());
	}

	@Override
	public void setBbox(Rectangle bbox) {
		if (bbox == null)
			this.bbox = null;
		else
			this.bbox = new Rectangle(bbox.x, bbox.y, bbox.width, bbox.height);
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public boolean isLoaded() {
		return tex != null;
	}

	// public TextureRegion getTextureRegion() {
	// return tex;
	// }

	public abstract void update(float delta);

	public void draw(SpriteBatch batch, float x, float y, float originX,
			float originY, float scale) {
		batch.draw(tex, x, y, originX, originY, tex.getRegionWidth(),
				tex.getRegionHeight(), scale, scale, 0);
	}

	public void draw(SpriteBatch batch) {
		if (isVisible()) {
			if (tex == null) {
				if (bbox != null)
					RectangleRenderer.draw(batch, pos.x - bbox.width / 2,
							pos.y, bbox.width, bbox.height, Color.RED);
				else
					RectangleRenderer.draw(batch, pos.x - getWidth() / 2,
							pos.y, getWidth(), getHeight(), Color.RED);

				return;
			}

			if (flipX) {
				float x = pos.x + tex.getRegionWidth() / 2 * scale;
				batch.draw(tex, x, pos.y, 0, 0, -tex.getRegionWidth(),
						tex.getRegionHeight(), scale, scale, 0.0f);
			} else {
				float x = pos.x - tex.getRegionWidth() / 2 * scale;
				batch.draw(tex, x, pos.y, 0, 0, tex.getRegionWidth(),
						tex.getRegionHeight(), scale, scale, 0.0f);
			}
		}
	}

	public void startFrameAnimation(String id, ActionCallback cb) {
		startFrameAnimation(id, EngineTween.REPEAT_DEFAULT, 1, false, cb);
	}

	public abstract void startFrameAnimation(String id, int repeatType,
			int count, boolean reverse, ActionCallback cb);

	/**
	 * Create position animation.
	 * 
	 * @param manager
	 * @param type
	 * @param duration
	 *            is in pixels/seg
	 * @param destX
	 * @param destY
	 */
	public void startPosAnimation(int repeatType, int count, float duration,
			float destX, float destY, ActionCallback cb) {

		TweenManager manager = TweenManagerSingleton.getInstance();

		manager.killTarget(this, EngineTween.SPRITE_POS_TYPE);
		manager.killTarget(this, EngineTween.WALK_TYPE);

		SpritePosTween t = new SpritePosTween();

		t.start(this, repeatType, count, new Vector2(destX, destY), duration,
				cb);

		manager.add(t);
	}

	public abstract String getCurrentFrameAnimationId();

	public abstract void lookat(Vector2 p);

	public abstract void lookat(String direction);

	public abstract void stand();

	public abstract void startWalkFA(Vector2 p0, Vector2 pf);

	// WALKING SUPPORT
	public void goTo(Vector2 pf, ActionCallback cb) {
		EngineLogger.debug(MessageFormat.format("GOTO {0},{1}", pf.x, pf.y));

		Vector2 p0 = getPosition();

		ArrayList<Vector2> walkingPath = scene.getBackgroundMap().findPath(
				scene, p0, pf);

		if (walkingPath.size() == 0 ) {
			// llamamos al callback aunque el camino esté vacío
			if(cb != null)
				cb.onEvent();
			
			return;
		}

		TweenManager manager = TweenManagerSingleton.getInstance();

		manager.killTarget(this);

		WalkTween t = new WalkTween();

		t.start(this, walkingPath, walkingSpeed, cb);

		manager.add(t);
	}

	@Override
	public void retrieveAssets() {
		super.retrieveAssets();

		// setPosition(getPosition().x, getPosition().y);
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("initFrameAnimation", initFrameAnimation);

		json.writeValue("scale", scale);

		float worldScale = EngineAssetManager.getInstance().getScale();
		Vector2 scaledPos = new Vector2(pos.x / worldScale, pos.y / worldScale);
		json.writeValue("pos", scaledPos);

		json.writeValue("flipX", flipX);

		json.writeValue("walkingSpeed", walkingSpeed);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		initFrameAnimation = json.readValue("initFrameAnimation", String.class,
				jsonData);

		scale = json.readValue("scale", Float.class, jsonData);
		pos = json.readValue("pos", Vector2.class, jsonData);

		float worldScale = EngineAssetManager.getInstance().getScale();
		pos.x *= worldScale;
		pos.y *= worldScale;

		flipX = json.readValue("flipX", Boolean.class, jsonData);

		walkingSpeed = json.readValue("walkingSpeed", Float.class, jsonData);
	}
}
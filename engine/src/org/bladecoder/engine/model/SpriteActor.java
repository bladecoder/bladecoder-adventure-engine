package org.bladecoder.engine.model;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.SpritePosTween;
import org.bladecoder.engine.anim.TweenManagerSingleton;
import org.bladecoder.engine.anim.WalkTween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.pathfinder.PixTileMap;
import org.bladecoder.engine.util.EngineLogger;

import aurelienribon.tweenengine.TweenManager;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class SpriteActor extends Actor {
	private final static float DEFAULT_WALKING_SPEED = 700f; // Speed units:
																// pix/sec.

	private SpriteRenderer renderer;

	public static enum DepthType {
		NONE, MAP, VECTOR
	};

	protected Vector2 pos = new Vector2();
	protected float scale = 1.0f;

	/** Scale sprite acording to the scene depth map */
	private DepthType depthType = DepthType.NONE;
	protected Scene scene = null;

	private float walkingSpeed = DEFAULT_WALKING_SPEED;

	public void setRenderer(SpriteRenderer r) {
		renderer = r;
	}

	public SpriteRenderer getRenderer() {
		return renderer;
	}

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
		return renderer.getWidth() * scale;
	}

	public float getHeight() {
		return renderer.getHeight() * scale;
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

	public void update(float delta) {
		renderer.update(delta);
	}

	public void draw(SpriteBatch batch) {
		if (isVisible()) {
			float x = pos.x - renderer.getWidth() / 2 * scale;

			renderer.draw(batch, x, pos.y, 0, 0, scale);
		}
	}

	public void startFrameAnimation(String id, ActionCallback cb) {
		startFrameAnimation(id, EngineTween.FROM_FA, 1, cb);
	}

	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {

		FrameAnimation fa = renderer.getCurrentFrameAnimation();

		if (fa != null) {

			if (fa.sound != null) {
				stopSound(fa.sound);
			}

			Vector2 outD = fa.outD;

			if (outD != null) {
				float s = EngineAssetManager.getInstance().getScale();

				pos.x += outD.x * s;
				pos.y += outD.y * s;
			}
		}

		renderer.startFrameAnimation(id, repeatType, count, cb);

		fa = renderer.getCurrentFrameAnimation();

		if (fa != null) {
			if (fa.sound != null) {
				playSound(fa.sound);
			}

			Vector2 inD = fa.inD;

			if (inD != null) {
				float s = EngineAssetManager.getInstance().getScale();
				pos.x += inD.x * s;
				pos.y += inD.y * s;
			}
		}
	}

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

	public void lookat(Vector2 p) {
		renderer.lookat(pos, p);
	}

	public void lookat(String direction) {
		renderer.lookat(direction);
	}

	public void stand() {
		renderer.stand();
	}

	public void startWalkFA(Vector2 p0, Vector2 pf) {
		renderer.startWalkFA(p0, pf);
	}

	// WALKING SUPPORT
	public void goTo(Vector2 pf, ActionCallback cb) {
		EngineLogger.debug(MessageFormat.format("GOTO {0},{1}", pf.x, pf.y));

		Vector2 p0 = getPosition();

		ArrayList<Vector2> walkingPath = null;
		PixTileMap bgMap = scene.getBackgroundMap();

		if (bgMap != null)
			walkingPath = bgMap.findPath(scene, p0, pf);

		if (walkingPath == null || walkingPath.size() == 0) {
			// llamamos al callback aunque el camino esté vacío
			if (cb != null)
				ActionCallbackQueue.add(cb);

			return;
		}

		TweenManager manager = TweenManagerSingleton.getInstance();

		manager.killTarget(this);

		WalkTween t = new WalkTween();

		t.start(this, walkingPath, walkingSpeed, cb);

		manager.add(t);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("  Sprite Bbox: ").append(getBBox().toString());

		sb.append(renderer);

		return sb.toString();
	}

	@Override
	public void loadAssets() {
		super.loadAssets();

		renderer.loadAssets();
	}

	@Override
	public void retrieveAssets() {
		super.retrieveAssets();

		renderer.retrieveAssets();
	}

	@Override
	public void dispose() {
		renderer.dispose();
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("scale", scale);

		float worldScale = EngineAssetManager.getInstance().getScale();
		Vector2 scaledPos = new Vector2(pos.x / worldScale, pos.y / worldScale);
		json.writeValue("pos", scaledPos);

		json.writeValue("walkingSpeed", walkingSpeed);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		scale = json.readValue("scale", Float.class, jsonData);
		pos = json.readValue("pos", Vector2.class, jsonData);

		float worldScale = EngineAssetManager.getInstance().getScale();
		pos.x *= worldScale;
		pos.y *= worldScale;

		walkingSpeed = json.readValue("walkingSpeed", Float.class, jsonData);
	}
}
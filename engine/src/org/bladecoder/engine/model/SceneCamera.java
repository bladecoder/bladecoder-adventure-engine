package org.bladecoder.engine.model;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.CameraTween;
import org.bladecoder.engine.anim.EngineTween;
import org.bladecoder.engine.anim.TweenManagerSingleton;

import aurelienribon.tweenengine.TweenManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class SceneCamera extends OrthographicCamera implements Serializable  {
	
	private static final float START_SCROLLX = 0.1f;
	private static final float START_SCROLLY = 0.2f;

	private float startScrollDistanceX;
	private float startScrollDistanceY;

	private float scrollingWidth, scrollingHeight;
	
	// to avoid create new vector when calling getPosition
	private final Vector2 tmpPos = new Vector2();
	
	public SceneCamera() {
	}
	
	public void create(float worldWidth, float worldHeight) {
		scrollingWidth = worldWidth;
		scrollingHeight = worldHeight;
		
		zoom = 1.0f;
		
		setToOrtho(false, worldWidth, worldHeight);
		update();
		
		startScrollDistanceX = worldWidth * START_SCROLLX; // When the followed actor reach 1/4 of
		  // the world scrolling starts

		startScrollDistanceY = worldHeight * START_SCROLLY;
	}
	
	public float getWidth() {
		return viewportWidth;
	}
	
	public float getHeight() {
		return viewportHeight;
	}

	public void setScrollingDimensions(float w, float h) {
		scrollingWidth = w;
		scrollingHeight = h;
	}

	public void setPosition(float x, float y) {

		float maxleft = viewportWidth / 2;
		float maxright = scrollingWidth - viewportWidth / 2;
		
		float maxbottom = viewportHeight / 2;
		float maxtop = scrollingHeight - viewportHeight / 2;

		if (x <= maxleft)
			x = maxleft;
		else if (x >= maxright)
			x = maxright;
		
		if (y <= maxbottom)
			y = maxbottom;
		else if (y >= maxtop)
			y = maxtop;

		position.set(x, y, 0);
		update();
	}
	
	public void setZoom(float zoom) {
		this.zoom = zoom;
		update();
	}
	
	public Vector2 getPosition() {
		Vector3 p = position;
		return tmpPos.set(p.x, p.y);
	}
	
	public float getZoom() {
		return zoom;
	}

	/**
	 * Create camera animation.
	 * 
	 * @param manager
	 * @param type
	 * @param speed
	 *            is in pixels/seg
	 * @param destX
	 * @param destY
	 */
	public void startAnimation(float destX, float destY, float zoom, float duration, ActionCallback cb) {
		TweenManager manager = TweenManagerSingleton.getInstance();

		manager.killTarget(this, EngineTween.CAMERA_TYPE);

		CameraTween t = new CameraTween();
		
		t.start(EngineTween.NO_REPEAT, 1, new Vector2(destX, destY), zoom, duration, cb);
		manager.add(t);
	}

	public Vector3 getInputUnProject(Rectangle viewport) {

		Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);

		unproject(touchPos, viewport.x, viewport.y, viewport.width, viewport.height);

		if (touchPos.x >= scrollingWidth)
			touchPos.x = scrollingWidth - 1;
		else if (touchPos.x < 0)
			touchPos.x = 0;

		if (touchPos.y >= scrollingHeight)
			touchPos.y = scrollingHeight - 1;
		else if (touchPos.y < 0)
			touchPos.y = 0;

		return touchPos;
	}

	public void updatePos(SpriteActor followActor) {
		float cx = position.x;
		float posx = followActor.getPosition().x;
		float cy = position.y;
		float posy = followActor.getPosition().y;
		
		boolean translate = false;

		if (cx - posx > startScrollDistanceX) {
			cx = cx - (cx - posx - startScrollDistanceX);
			translate = true;
		} else if (posx - cx > startScrollDistanceX) {
			cx = cx + (posx - cx - startScrollDistanceX);
			translate = true;
		}
		
		if (cy - posy  + followActor.getHeight() > startScrollDistanceY) {
			cy = cy - (cy - posy - startScrollDistanceY);
			translate = true;
		} else if (posy - cy > startScrollDistanceY) {
			cy = cy + (posy - cy - startScrollDistanceY);
			translate = true;
		}
		
		if(translate) {
			setPosition(cx, cy);
		}
	}

	public Vector3 scene2screen(float x, float y, Rectangle viewport) {
		Vector3 v = new Vector3(x, y, 0);

		project(v, 0, 0, viewport.width, viewport.height);

		return v;
	}
	
	@Override
	public void write(Json json) {
		json.writeValue("startScrollDistanceX", startScrollDistanceX);
		json.writeValue("startScrollDistanceY", startScrollDistanceY);
		json.writeValue("width", viewportWidth);
		json.writeValue("height", viewportHeight);
		json.writeValue("scrollingWidth", scrollingWidth);
		json.writeValue("scrollingHeight", scrollingHeight);
		json.writeValue("pos", getPosition());
		json.writeValue("zoom", getZoom());
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		startScrollDistanceX = json.readValue("startScrollDistanceX", Float.class, jsonData);
		startScrollDistanceY = json.readValue("startScrollDistanceY", Float.class, jsonData);
		
		viewportWidth = json.readValue("width", Float.class, jsonData);
		viewportHeight = json.readValue("height", Float.class, jsonData);
		scrollingWidth = json.readValue("scrollingWidth", Float.class, jsonData);
		scrollingHeight = json.readValue("scrollingHeight", Float.class, jsonData);
		Vector2 pos = json.readValue("pos", Vector2.class, jsonData);
		float z = json.readValue("zoom", Float.class, jsonData);
		
		create(viewportWidth, viewportHeight);
		setPosition(pos.x, pos.y);
		setZoom(z);
	}	
}

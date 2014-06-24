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

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.anim.CameraTween;
import org.bladecoder.engine.anim.Tween;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.Viewport;

public class SceneCamera extends OrthographicCamera implements Serializable  {
	
	private static final float START_SCROLLX = 0.1f;
	private static final float START_SCROLLY = 0.2f;
	
	// to avoid create new vector when calling getPosition
	private final static Vector2 tmpPos = new Vector2();

	private float startScrollDistanceX;
	private float startScrollDistanceY;

	private float scrollingWidth, scrollingHeight;
	
	private CameraTween cameraTween;
	
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
	
	public float getScrollingWidth() {
		return scrollingWidth;
	}
	
	public float getScrollingHeight() {
		return scrollingHeight;
	}

	public void setScrollingDimensions(float w, float h) {
		scrollingWidth = w;
		scrollingHeight = h;
	}
	
	public void update(float delta) {
		if(cameraTween != null) {
			cameraTween.update(delta, this);
			if(cameraTween.isComplete()) {
				cameraTween = null;
			}
		}
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
		cameraTween = new CameraTween();
		
		cameraTween.start(this, Tween.NO_REPEAT, 1, destX, destY, zoom, duration, cb);
	}

	public void getInputUnProject(Viewport viewport, Vector3 out) {

		out.set(Gdx.input.getX(), Gdx.input.getY(), 0);

		unproject(out, viewport.getViewportX(), viewport.getViewportY(), 
				viewport.getViewportWidth(), viewport.getViewportHeight());

		if (out.x >= scrollingWidth)
			out.x = scrollingWidth - 1;
		else if (out.x < 0)
			out.x = 0;

		if (out.y >= scrollingHeight)
			out.y = scrollingHeight - 1;
		else if (out.y < 0)
			out.y = 0;
	}

	public void updatePos(SpriteActor followActor) {
		float cx = position.x;
		float posx = followActor.getX();
		float cy = position.y;
		float posy = followActor.getY();
		
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

	public void scene2screen(Viewport viewport, Vector3 out) {
		project(out, 0, 0, viewport.getViewportWidth(), viewport.getViewportHeight());
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
		json.writeValue("cameraTween", cameraTween);
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

		cameraTween = json.readValue("cameraTween", CameraTween.class, jsonData);
	}	
}

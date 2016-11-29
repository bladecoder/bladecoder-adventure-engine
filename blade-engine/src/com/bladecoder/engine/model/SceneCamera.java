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
package com.bladecoder.engine.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.CameraTween;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.InterpolationMode;

public class SceneCamera extends OrthographicCamera implements Serializable  {
	
	private static final float START_SCROLLX = 0.1f;
	private static final float START_SCROLLY = 0.15f;
	
	// to avoid create new vector when calling getPosition
	private final static Vector2 tmpPos = new Vector2();

	private float startScrollDistanceX;
	private float startScrollDistanceY;

	private float scrollingWidth, scrollingHeight;
	
	private CameraTween cameraTween;
	
	private Matrix4 parallaxView = new Matrix4();
	private Matrix4 parallaxCombined = new Matrix4();
	private Vector3 tmp = new Vector3();
	private Vector3 tmp2 = new Vector3();
	
	public SceneCamera() {
	}
	
	public void create(float worldWidth, float worldHeight) {
		scrollingWidth = worldWidth;
		scrollingHeight = worldHeight;
		
		zoom = 1.0f;
		
		setToOrtho(false, worldWidth, worldHeight);
		update();
		
		startScrollDistanceX = worldWidth * START_SCROLLX; 
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
		scrollingWidth = Math.max(w, viewportWidth);
		scrollingHeight =  Math.max(h, viewportHeight);
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

		float maxleft = viewportWidth / 2 * zoom;
		float maxright = (scrollingWidth - viewportWidth / 2 * zoom);
		
		float maxbottom = viewportHeight / 2 * zoom;
		float maxtop = (scrollingHeight - viewportHeight / 2 * zoom);

		x = MathUtils.clamp(x, maxleft, maxright);	
		y = MathUtils.clamp(y, maxbottom, maxtop);

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
	 */
	public void startAnimation(float destX, float destY, float zoom, float duration, InterpolationMode interpolation, ActionCallback cb) {
		cameraTween = new CameraTween();
		
		cameraTween.start(this, Tween.Type.NO_REPEAT, 1, destX, destY, zoom, duration, interpolation, cb);
	}

	public void getInputUnProject(Viewport viewport, Vector3 out) {

		out.set(Gdx.input.getX(), Gdx.input.getY(), 0);

		unproject(out, viewport.getScreenX(), viewport.getScreenY(), 
				viewport.getScreenWidth(), viewport.getScreenHeight());

		out.x = MathUtils.clamp(out.x, 0, scrollingWidth - 1);
		out.y = MathUtils.clamp(out.y, 0, scrollingHeight - 1);
	}

	public void updatePos(SpriteActor followActor) {
		float cx = position.x;
		float posx = followActor.getX();
		float cy = position.y;
		float posy = followActor.getY();
		
		boolean translate = false;

		if (cx - posx > startScrollDistanceX * zoom) {
			cx = cx - (cx - posx - startScrollDistanceX * zoom);
			translate = true;
		} else if (posx - cx > startScrollDistanceX * zoom) {
			cx = cx + (posx - cx - startScrollDistanceX * zoom);
			translate = true;
		}
		
		if (cy - posy  + followActor.getHeight() > startScrollDistanceY * zoom) {
			cy = cy - (cy - posy - startScrollDistanceY * zoom);
			translate = true;
		} else if (posy - cy > startScrollDistanceY * zoom) {
			cy = cy + (posy - cy - startScrollDistanceY * zoom);
			translate = true;
		}
		
		if(translate) {
			setPosition(cx, cy);
		}
	}

	public void scene2screen(Viewport viewport, Vector3 out) {
		project(out, 0, 0, viewport.getScreenWidth(), viewport.getScreenHeight());
	}
	
	public Matrix4 calculateParallaxMatrix (float parallaxX, float parallaxY) {
		update();
		tmp.set(position);
//		tmp.x *= parallaxX;
		tmp.y *= parallaxY;
		
		tmp.x = (tmp.x - scrollingWidth / 2) * parallaxX + scrollingWidth / 2; 

		parallaxView.setToLookAt(tmp, tmp2.set(tmp).add(direction), up);
		parallaxCombined.set(projection);
		Matrix4.mul(parallaxCombined.val, parallaxView.val);
		return parallaxCombined;
	}
	
	@Override
	public void write(Json json) {
		float worldScale = EngineAssetManager.getInstance().getScale();
		
		json.writeValue("width", viewportWidth / worldScale);
		json.writeValue("height", viewportHeight / worldScale);
		json.writeValue("scrollingWidth", scrollingWidth / worldScale);
		json.writeValue("scrollingHeight", scrollingHeight / worldScale);
		
		Vector2 p = getPosition();
		p.x = p.x/worldScale;
		p.y = p.y/worldScale;
		json.writeValue("pos", p);
		json.writeValue("zoom", getZoom());
		json.writeValue("cameraTween", cameraTween);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		float worldScale = EngineAssetManager.getInstance().getScale();
		
		viewportWidth = json.readValue("width", Float.class, jsonData) * worldScale;
		viewportHeight = json.readValue("height", Float.class, jsonData) * worldScale;
		scrollingWidth = json.readValue("scrollingWidth", Float.class, jsonData) * worldScale;
		scrollingHeight = json.readValue("scrollingHeight", Float.class, jsonData) * worldScale;
		Vector2 pos = json.readValue("pos", Vector2.class, jsonData);
		pos.x *=  worldScale;
		pos.y *=  worldScale;
		float z = json.readValue("zoom", Float.class, jsonData);
		
		create(viewportWidth, viewportHeight);
		this.zoom = z;
		position.set(pos.x, pos.y, 0);
		update();

		cameraTween = json.readValue("cameraTween", CameraTween.class, jsonData);
	}	
}

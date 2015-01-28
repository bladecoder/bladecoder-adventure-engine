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

import java.text.MessageFormat;
import java.util.ArrayList;

import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.ActorRenderer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.SpritePosTween;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.anim.WalkTween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.EngineLogger;

public class SpriteActor extends BaseActor {
	private final static float DEFAULT_WALKING_SPEED = 700f; // Speed units:
																// pix/sec.

	public static enum DepthType {
		NONE, VECTOR
	};	
	
	private ActorRenderer renderer;
	private SpritePosTween posTween;
	private float scale = 1.0f;

	/** Scale sprite acording to the scene depth map */
	private DepthType depthType = DepthType.NONE;

	private float walkingSpeed = DEFAULT_WALKING_SPEED;
	private boolean bboxFromRenderer = false;

	public void setRenderer(ActorRenderer r) {
		renderer = r;
	}

	public ActorRenderer getRenderer() {
		return renderer;
	}

	public void setWalkingSpeed(float s) {
		walkingSpeed = s;
	}

	public DepthType getDepthType() {
		return depthType;
	}

	public void setDepthType(DepthType v) {
		depthType = v;
	}

	public void setPosition(float x, float y) {
		
		if(isWalkObstacle() && scene.getPolygonalNavGraph() != null) {
			scene.getPolygonalNavGraph().removeDinamicObstacle(bbox);
		}
		
		bbox.setPosition(x, y);
		
		if(isWalkObstacle() && scene.getPolygonalNavGraph() != null) {
			scene.getPolygonalNavGraph().addDinamicObstacle(bbox);
		}
		
		if (scene != null) {
			if (depthType == DepthType.VECTOR) {
				// interpolation equation
				float s = scene.getFakeDepthScale(y);

				if (s != 0)
					setScale(s);
			}

			if (scene.getCameraFollowActor() == this)
				scene.getCamera().updatePos(this);

		}

	}
	
	public boolean isBboxFromRenderer() {
		return bboxFromRenderer;
	}

	public void setBboxFromRenderer(boolean v) {
		this.bboxFromRenderer = v;
		
		if(v)
			updateBBox();
	}

	public float getWidth() {
		return renderer.getWidth() * scale;
	}

	public float getHeight() {
		return renderer.getHeight() * scale;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
		bbox.setScale(scale, scale);
	}

	@Override
	public void update(float delta) {
		super.update(delta);
		renderer.update(delta);
		if(posTween != null) {
			posTween.update(this, delta);
			if(posTween.isComplete()) {
				posTween = null;
			}
		}
	}

	public void draw(SpriteBatch batch) {
		if (isVisible()) {
			renderer.draw(batch, getX(), getY(), scale);
		}
	}

	public void startAnimation(String id, ActionCallback cb) {
		startAnimation(id, Tween.FROM_FA, 1, cb);
	}

	public void startAnimation(String id, int repeatType, int count,
			ActionCallback cb) {

		AnimationDesc fa = renderer.getCurrentAnimation();

		if (fa != null) {

			if (fa.sound != null) {
				stopSound(fa.sound);
			}

			Vector2 outD = fa.outD;

			if (outD != null) {
				float s = EngineAssetManager.getInstance().getScale();
				
				setPosition(getX() + outD.x * s, getY() + outD.y * s);
			}
		}

		renderer.startAnimation(id, repeatType, count, cb);

		fa = renderer.getCurrentAnimation();

		if (fa != null) {
			if(bboxFromRenderer) {
				if(isWalkObstacle() && scene.getPolygonalNavGraph() != null) {
					scene.getPolygonalNavGraph().removeDinamicObstacle(bbox);
				}
				
				updateBBox();
				
				if(isWalkObstacle() && scene.getPolygonalNavGraph() != null) {
					scene.getPolygonalNavGraph().addDinamicObstacle(bbox);
				}
			}
			
			if (fa.sound != null) {
				playSound(fa.sound);
			}

			Vector2 inD = fa.inD;

			if (inD != null) {
				float s = EngineAssetManager.getInstance().getScale();
				
				setPosition(getX() + inD.x * s, getY() + inD.y * s);
			}
		}
	}

	/**
	 * Create position animation.
	 */
	public void startPosAnimation(int repeatType, int count, float duration,
			float destX, float destY, ActionCallback cb) {

		posTween = new SpritePosTween();

		posTween.start(this, repeatType, count, destX, destY, duration,
				cb);
	}

	public void lookat(Vector2 p) {
		renderer.lookat(bbox.getX(), bbox.getY(), p);
		if(bboxFromRenderer)
			updateBBox();
	}

	public void lookat(String direction) {
		renderer.lookat(direction);
		if(bboxFromRenderer)
			updateBBox();
	}

	public void stand() {
		renderer.stand();
		if(bboxFromRenderer)
			updateBBox();
	}

	public void startWalkFA(Vector2 p0, Vector2 pf) {
		renderer.walk(p0, pf);
		if(bboxFromRenderer)
			updateBBox();
	}

	/**
	 * Walking Support
	 * 
	 * @param pf Final position to walk
	 * @param cb The action callback
	 */
	public void goTo(Vector2 pf, ActionCallback cb) {
		EngineLogger.debug(MessageFormat.format("GOTO {0},{1}", pf.x, pf.y));

		Vector2 p0 = new Vector2(bbox.getX(), bbox.getY());

		ArrayList<Vector2> walkingPath = null;

		if(scene.getPolygonalNavGraph() != null) {
			walkingPath = scene.getPolygonalNavGraph().findPath(p0.x, p0.y, pf.x, pf.y);
		}

		if (walkingPath == null || walkingPath.size() == 0) {
			// llamamos al callback aunque el camino esté vacío
			if (cb != null)
				ActionCallbackQueue.add(cb);

			return;
		}

		posTween = new WalkTween();

		((WalkTween)posTween).start(this, walkingPath, walkingSpeed, cb);
	}
	
	/**
	 * Updates de bbox with the renderer width and height information
	 */
	private void updateBBox() {
		if(bbox.getVertices() == null || bbox.getVertices().length != 4) {
			bbox.setVertices(new float[8]);
		}
		
		float[] verts = bbox.getVertices();
		
		verts[0] = -renderer.getWidth()/2;
		verts[1] = 0f;
		verts[2] = -renderer.getWidth()/2;
		verts[3] = renderer.getHeight();
		verts[4] = renderer.getWidth()/2;
		verts[5] = renderer.getHeight();
		verts[6] = renderer.getWidth()/2;
		verts[7] = 0f;		
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
		renderer.retrieveAssets();
		
		if(bboxFromRenderer) {
			updateBBox();
		}
		
		// Call setPosition to recalc fake depth and camera follow
		setPosition(bbox.getX(), bbox.getY());
		
		super.retrieveAssets();
	}

	@Override
	public void dispose() {
		renderer.dispose();
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("scale", scale);
		json.writeValue("walkingSpeed", walkingSpeed);
		json.writeValue("posTween", posTween, null);
		json.writeValue("depthType", depthType);
		json.writeValue("renderer", renderer, null);
		json.writeValue("bboxFromRenderer", bboxFromRenderer);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		scale = json.readValue("scale", Float.class, jsonData);
		walkingSpeed = json.readValue("walkingSpeed", Float.class, jsonData);
		posTween = json.readValue("posTween", SpritePosTween.class, jsonData);
		depthType = json.readValue("depthType", DepthType.class, jsonData);
		
		renderer = json.readValue("renderer", ActorRenderer.class, jsonData);
		
		bboxFromRenderer = json.readValue("bboxFromRenderer", Boolean.class, jsonData);
		
		if(bboxFromRenderer)
			bbox.setScale(1, 1);
	}

}
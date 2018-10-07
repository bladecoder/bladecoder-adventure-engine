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

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.anim.Tween.Type;
import com.bladecoder.engine.anim.WalkTween;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.util.EngineLogger;

public class SpriteActor extends InteractiveActor implements AssetConsumer {

	protected ActorRenderer renderer;

	protected ArrayList<Tween<SpriteActor>> tweens = new ArrayList<>(0);

	private float rot = 0.0f;
	private float scaleX = 1.0f;
	private float scaleY = 1.0f;
	private Color tint;

	private boolean fakeDepth = false;

	private boolean bboxFromRenderer = false;

	private String playingSound;

	public void setRenderer(ActorRenderer r) {
		renderer = r;
	}

	public ActorRenderer getRenderer() {
		return renderer;
	}

	public boolean getFakeDepth() {
		return fakeDepth;
	}

	public void setFakeDepth(boolean fd) {
		fakeDepth = fd;
	}

	@Override
	public void setPosition(float x, float y) {
		super.setPosition(x, y);

		if (scene != null) {
			if (fakeDepth) {
				// interpolation equation
				float s = scene.getFakeDepthScale(y);

				setScale(s);
			}
		}

	}

	public boolean isBboxFromRenderer() {
		return bboxFromRenderer;
	}

	public void setBboxFromRenderer(boolean v) {
		this.bboxFromRenderer = v;

		if (v)
			renderer.updateBboxFromRenderer(bbox);
		else
			renderer.updateBboxFromRenderer(null);
	}

	public float getWidth() {
		return renderer.getWidth() * scaleX;
	}

	public float getHeight() {
		return renderer.getHeight() * scaleY;
	}

	public float getScale() {
		return scaleX;
	}
	
	public float getScaleX() {
		return scaleX;
	}
	
	public float getScaleY() {
		return scaleY;
	}

	public Color getTint() {
		return tint;
	}

	public void setTint(Color tint) {
		this.tint = tint;
	}

	public void setScale(float scale) {
		setScale(scale, scale);
	}
	
	public void setScale(float scaleX, float scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;

		if (bboxFromRenderer)
			bbox.setScale(scaleX, scaleY);
		else {
			float worldScale = EngineAssetManager.getInstance().getScale();
			bbox.setScale(scaleX * worldScale, scaleY * worldScale);
		}
	}

	public void setRot(float rot) {
		this.rot = rot;
		bbox.setRotation(rot);
	}

	public float getRot() {
		return rot;
	}

	@Override
	public void update(float delta) {
		super.update(delta);

		if (visible) {
			renderer.update(delta);

			for (int i = 0; i < tweens.size(); i++) {
				Tween<SpriteActor> t = tweens.get(i);

				t.update(delta);

				// Needs extra checks before remove because the update can remove the tween
				if (t.isComplete() && i < tweens.size() && tweens.get(i) == t) {
					tweens.remove(i);
					i--;
				}
			}
		}
	}

	public void draw(SpriteBatch batch) {
		if (isVisible()) {
			if (scaleX != 0 && scaleY != 0) {
				renderer.draw(batch, getX(), getY(), scaleX, scaleY, rot, tint);
			}
		}
	}

	public void startAnimation(String id, ActionCallback cb) {
		startAnimation(id, Tween.Type.SPRITE_DEFINED, 1, cb);
	}

	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb) {

		if (!(renderer instanceof AnimationRenderer))
			return;

		inAnim();

		// resets posTween when walking
		removeTween(WalkTween.class);

		EngineLogger.debug("ANIMATION: " + this.id + "." + id);

		((AnimationRenderer) renderer).startAnimation(id, repeatType, count, cb);

		outAnim(repeatType);
	}

	public void removeTween(Class<?> clazz) {
		for (int i = 0; i < tweens.size(); i++) {
			Tween<SpriteActor> t = tweens.get(i);
			if (clazz.isInstance(t)) {
				tweens.remove(i);
				i--;
			}
		}
	}

	/**
	 * Actions to do when setting an animation: - stop previous animation sound -
	 * add 'out' distance from previous animation
	 */
	protected void inAnim() {
		AnimationDesc fa = ((AnimationRenderer) renderer).getCurrentAnimation();

		if (fa != null) {

			if (fa.sound != null) {
				// Backwards compatibility
				String sid = fa.sound;
				if (scene != null && scene.getWorld().getSounds().get(sid) == null)
					sid = id + "_" + fa.sound;

				// it will not play the sound in inventory
				if (scene != null)
					scene.getSoundManager().stopSound(sid);
			}

			Vector2 outD = fa.outD;

			if (outD != null) {
				float s = EngineAssetManager.getInstance().getScale();

				setPosition(getX() + outD.x * s, getY() + outD.y * s);
			}
		}
	}

	/**
	 * Actions to do when setting an animation: - play animation sound - add 'in'
	 * distance
	 * 
	 * @param repeatType
	 */
	protected void outAnim(Type repeatType) {
		AnimationDesc fa = ((AnimationRenderer) renderer).getCurrentAnimation();

		if (fa != null) {

			if (fa.sound != null && repeatType != Tween.Type.REVERSE) {
				// Backwards compatibility
				String sid = fa.sound;
				if (scene != null && scene.getWorld().getSounds().get(sid) == null)
					sid = id + "_" + fa.sound;

				// it will not play the sound in inventory
				if (scene != null)
					scene.getSoundManager().playSound(sid);
			}

			Vector2 inD = fa.inD;

			if (inD != null) {
				float s = EngineAssetManager.getInstance().getScale();

				setPosition(getX() + inD.x * s, getY() + inD.y * s);
			}
		}
	}

	public void addTween(Tween<SpriteActor> tween) {
		removeTween(tween.getClass());
		
		tweens.add(tween);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());

		sb.append("  Sprite Bbox: ").append(getBBox().toString());

		sb.append(renderer);

		return sb.toString();
	}

	@Override
	public void loadAssets() {

		renderer.loadAssets();
	}

	@Override
	public void retrieveAssets() {

		renderer.retrieveAssets();

		// Call setPosition to recalc fake depth and camera follow
		setPosition(bbox.getX(), bbox.getY());
	}

	@Override
	public void dispose() {
		// EngineLogger.debug("DISPOSE: " + id);

		renderer.dispose();
	}

	@Override
	public void write(Json json) {
		BladeJson bjson = (BladeJson) json;

		// Reset vertices if bboxFromRenderer to save always with 0.0 value
		if (bboxFromRenderer && bjson.getMode() == Mode.MODEL) {
			float[] verts = bbox.getVertices();
			bbox.setVertices(new float[8]);

			super.write(json);

			bbox.setVertices(verts);
		} else {
			super.write(json);
		}
		
		if (bjson.getMode() == Mode.MODEL) {
			json.writeValue("renderer", renderer, null);
		} else {
			json.writeValue("renderer", renderer);
			json.writeValue("tweens", tweens, ArrayList.class, Tween.class);
			json.writeValue("playingSound", playingSound);
		}

		json.writeValue("scaleX", scaleX);
		json.writeValue("scaleY", scaleY);
		json.writeValue("rot", rot);
		
		if(tint != null)
			json.writeValue("tint", tint);
		
		json.writeValue("fakeDepth", fakeDepth);
		json.writeValue("bboxFromRenderer", bboxFromRenderer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			renderer = json.readValue("renderer", ActorRenderer.class, jsonData);
		} else {
			tweens = json.readValue("tweens", ArrayList.class, Tween.class, jsonData);

			if(tweens == null) {
				EngineLogger.debug("Couldn't load state of actor: " + id);
				return;
			}
				
			for (Tween<SpriteActor> t : tweens)
				t.setTarget(this);

			renderer.read(json, jsonData.get("renderer"));

			playingSound = json.readValue("playingSound", String.class, jsonData);
		}

		if (jsonData.get("scale") != null) {
			scaleX = json.readValue("scale", float.class, jsonData);
			scaleY = scaleX;
		} else {
			scaleX = json.readValue("scaleX", float.class, scaleX, jsonData);
			scaleY = json.readValue("scaleY", float.class, scaleY, jsonData);
		}
		
		rot = json.readValue("rot", float.class, rot, jsonData);
		tint = json.readValue("tint", Color.class, tint, jsonData);

		// backwards compatibility fakeDepth
		if (jsonData.get("depthType") != null) {
			String depthType = json.readValue("depthType", String.class, (String) null, jsonData);

			fakeDepth = "VECTOR".equals(depthType);
		} else {
			fakeDepth = json.readValue("fakeDepth", boolean.class, fakeDepth, jsonData);
		}

		bboxFromRenderer = json.readValue("bboxFromRenderer", boolean.class, bboxFromRenderer, jsonData);

		if (bboxFromRenderer)
			renderer.updateBboxFromRenderer(bbox);

		setScale(scaleX, scaleY);
		setRot(rot);
	}

}
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

import java.nio.IntBuffer;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader.Config;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.serialization.ActionCallbackSerializer;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.Utils3D;

@SuppressWarnings("deprecation")
public class Sprite3DRenderer extends AnimationRenderer {

	private static final String FRAGMENT_SHADER = "com/bladecoder/engine/shading/cel.fragment.glsl";
	private static final String FLOOR_FRAGMENT_SHADER = "com/bladecoder/engine/shading/floor.fragment.glsl";
	private static final String VERTEX_SHADER = "com/bladecoder/engine/shading/cel.vertex.glsl";
	private final static boolean USE_FBO = false;
	private final static int MAX_BONES = 40;

	private static final Rectangle VIEWPORT = new Rectangle();
	private final static IntBuffer VIEWPORT_RESULTS = BufferUtils.newIntBuffer(16);

	private int currentCount;
	private Tween.Type currentAnimationType;

	private TextureRegion tex;

	private Environment environment;
	private Environment shadowEnvironment;

	private FrameBuffer fb = null;

	private int width = 200, height = 200;

	private Vector3 cameraPos;
	private Vector3 cameraRot;
	private String cameraName = "Camera";
	private float cameraFOV = 49.3f;

	// Rotation of the model in the Y axis
	private float modelRotation = 0;

	// CREATE STATIC BATCHS FOR EFICIENCY
	private static ModelBatch modelBatch;
	private static ModelBatch shadowBatch;
	private static ModelBatch floorBatch;

	// TODO Move shadowLight to static for memory eficiency.
	// This implies that the shadow must be calculated in the draw method and
	// not in the update
	private final DirectionalShadowLight shadowLight = (DirectionalShadowLight) new DirectionalShadowLight(1024, 1024,
			30f, 30f, 1f, 100f).set(1f, 1f, 1f, 0.01f, -1f, 0.01f);

	PointLight celLight;

	String celLightName = "Light";

	private ActionCallback animationCb = null;

	private float lastAnimationTime = 0;

	private boolean renderShadow = true;

	class ModelCacheEntry extends CacheEntry {
		ModelInstance modelInstance;
		AnimationController controller;
		PerspectiveCamera camera3d;
	}

	public Sprite3DRenderer() {

	}

	@Override
	public String getCurrentAnimationId() {
		return currentAnimation.id;
	}

	@Override
	public String[] getInternalAnimations(AnimationDesc anim) {
		retrieveSource(anim.source);

		Array<Animation> animations = ((ModelCacheEntry) sourceCache.get(anim.source)).modelInstance.animations;
		String[] result = new String[animations.size];

		for (int i = 0; i < animations.size; i++) {
			Animation a = animations.get(i);
			result[i] = a.id;
		}

		return result;
	}

	/**
	 * Render the 3D model into the texture
	 */
	private void renderTex() {
		updateViewport();

		fb.begin();

		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		drawModel();

		fb.end((int) VIEWPORT.x, (int) VIEWPORT.y, (int) VIEWPORT.width, (int) VIEWPORT.height);
	}

	/**
	 * Generates the Shadow Map
	 */
	private void genShadowMap() {
		updateViewport();

		ModelCacheEntry cs = (ModelCacheEntry) currentSource;

		shadowLight.begin(Vector3.Zero, cs.camera3d.direction);
		shadowBatch.begin(shadowLight.getCamera());
		shadowBatch.render(cs.modelInstance);
		shadowBatch.end();
		shadowLight.end();

		Gdx.graphics.getGL20().glViewport((int) VIEWPORT.x, (int) VIEWPORT.y, (int) VIEWPORT.width,
				(int) VIEWPORT.height);
	}

	private void drawModel() {
		if (currentSource != null) {

			ModelCacheEntry cs = (ModelCacheEntry) currentSource;

			// DRAW SHADOW
			if (renderShadow) {
				floorBatch.begin(cs.camera3d);
				floorBatch.render(Utils3D.getFloor(), shadowEnvironment);
				floorBatch.end();
			}

			// DRAW MODEL
			modelBatch.begin(cs.camera3d);

			if (EngineLogger.debugMode() && EngineLogger.debugLevel == EngineLogger.DEBUG1)
				modelBatch.render(Utils3D.getAxes(), environment);

			modelBatch.render(cs.modelInstance, environment);

			modelBatch.end();
		}
	}

	public void setCameraPos(float x, float y, float z) {
		if (cameraPos == null)
			cameraPos = new Vector3(x, y, z);
		else
			cameraPos.set(x, y, z);
	}

	public void setCameraRot(float x, float y, float z) {
		if (cameraRot == null)
			cameraRot = new Vector3(x, y, z);
		else
			cameraRot.set(x, y, z);
	}

	public void setCameraFOV(float fov) {
		this.cameraFOV = fov;
	}

	public void setCameraName(String name) {
		this.cameraName = name;
	}

	public void setCelLightName(String name) {
		this.celLightName = name;
	}

	public float getCameraFOV() {
		return cameraFOV;
	}

	public String getCameraName() {
		return cameraName;
	}

	public Vector2 getSpriteSize() {
		return new Vector2(width, height);
	}

	private PerspectiveCamera getCamera(ModelInstance modelInstance) {
		PerspectiveCamera camera3d = new PerspectiveCamera(cameraFOV, width, height);

		if (cameraPos == null) {
			Node n = null;

			// SET CAMERA POS FROM MODEL IF EXISTS
			n = modelInstance.getNode(cameraName);

			if (n != null) {
				cameraPos = n.translation;
			} else {
				cameraPos = new Vector3(0, 0, 5);
			}
		}

		if (cameraRot == null) {
			Node n = null;

			// SET CAMERA POS FROM MODEL IF EXISTS
			n = modelInstance.getNode(cameraName);

			if (n != null) {
				float rx = (float) (MathUtils.radiansToDegrees
						* Math.asin(2 * n.rotation.x * n.rotation.y + 2 * n.rotation.z * n.rotation.w));
				float ry = (float) (MathUtils.radiansToDegrees
						* Math.atan2(2 * n.rotation.x * n.rotation.w - 2 * n.rotation.y * n.rotation.z,
								1 - 2 * n.rotation.x * n.rotation.x - 2 * n.rotation.z * n.rotation.z));
				float rz = (float) (Math.atan2(2 * n.rotation.y * n.rotation.w - 2 * n.rotation.x * n.rotation.z,
						1 - 2 * n.rotation.y * n.rotation.y - 2 * n.rotation.z * n.rotation.z));

				setCameraRot(rx, ry, rz);
			} else {
				cameraRot = new Vector3();
			}
		}

		camera3d.position.set(cameraPos);

		camera3d.rotate(cameraRot.x, 1, 0, 0);
		camera3d.rotate(cameraRot.y, 0, 1, 0);
		camera3d.rotate(cameraRot.z, 0, 0, 1);

		camera3d.near = 0.1f;
		camera3d.far = 30;
		camera3d.update();

		return camera3d;
	}

	private final AnimationListener animationListener = new AnimationListener() {

		@Override
		public void onLoop(com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc animation) {
		}

		@Override
		public void onEnd(com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc animation) {
			if (animationCb != null) {
				ActionCallback tmpcb = animationCb;
				animationCb = null;
				tmpcb.resume();
			}
		}
	};

	// public Array<Animation> getAnimations() {
	// return modelInstance.animations;
	// }

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb) {
		AnimationDesc fa = fanims.get(id);

		if (fa == null) {
			EngineLogger.error("AnimationDesc not found: " + id);

			return;
		}

		if (currentAnimation != null && currentAnimation.disposeWhenPlayed)
			disposeSource(currentAnimation.source);

		currentAnimation = fa;
		currentSource = sourceCache.get(fa.source);
		animationCb = cb;

		if (currentSource == null || currentSource.refCounter < 1) {
			// If the source is not loaded. Load it.
			loadSource(fa.source);
			EngineAssetManager.getInstance().finishLoading();

			retrieveSource(fa.source);

			currentSource = sourceCache.get(fa.source);

			if (currentSource == null) {
				EngineLogger.error("Could not load AnimationDesc: " + id);
				currentAnimation = null;

				return;
			}
		}

		if (repeatType == Tween.Type.SPRITE_DEFINED) {
			currentAnimationType = currentAnimation.animationType;
			currentCount = currentAnimation.count;
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		lastAnimationTime = 0;
		float speed = currentAnimation.duration;

		if (currentAnimationType == Tween.Type.REVERSE || currentAnimationType == Tween.Type.REVERSE_REPEAT)
			speed *= -1;

		ModelCacheEntry cs = (ModelCacheEntry) currentSource;

		if (cs.modelInstance.getAnimation(id) != null) {
			animationCb = cb;
			cs.controller.setAnimation(id, currentCount, speed, animationListener);
			computeBbox();
			return;
		}

		int idx = id.indexOf('.');

		if (idx != -1) {
			String s = id.substring(0, idx);
			String dir = id.substring(idx + 1);

			lookat(dir);

			if (cs.modelInstance.getAnimation(s) != null) {
				cs.controller.setAnimation(s, count, speed, animationListener);

				computeBbox();
				return;
			}
		}

		// ERROR CASE
		EngineLogger.error("Animation NOT FOUND: " + id);

		for (Animation a : cs.modelInstance.animations) {
			EngineLogger.debug(a.id);
		}

		if (cb != null) {
			ActionCallback tmpcb = cb;
			cb = null;
			tmpcb.resume();
		}

		computeBbox();
	}

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, String direction) {
		startAnimation(id, repeatType, count, null);

		if (direction != null && currentAnimation != null)
			lookat(direction);
	}

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, Vector2 p0, Vector2 pf) {
		startAnimation(id, repeatType, count, null);

		if (currentAnimation != null) {
			Vector2 tmp = new Vector2(pf);
			float angle = tmp.sub(p0).angle() + 90;
			lookat(angle);
		}
	}

	private void lookat(String dir) {
		EngineLogger.debug("LOOKAT DIRECTION - " + dir);

		if (dir.equals(BACK))
			lookat(180);
		else if (dir.equals(FRONT))
			lookat(0);
		else if (dir.equals(LEFT))
			lookat(270);
		else if (dir.equals(RIGHT))
			lookat(90);
		else if (dir.equals(BACKLEFT))
			lookat(225);
		else if (dir.equals(BACKRIGHT))
			lookat(135);
		else if (dir.equals(FRONTLEFT))
			lookat(-45);
		else if (dir.equals(FRONTRIGHT))
			lookat(45);
		else
			EngineLogger.error("LOOKAT: Direction not found - " + dir);

	}

	private void lookat(float angle) {
		((ModelCacheEntry) currentSource).modelInstance.transform.setToRotation(Vector3.Y, angle);
		modelRotation = angle;
	}

	public void setSpriteSize(Vector2 size) {
		this.width = (int) size.x;
		this.height = (int) size.y;
	}

	@Override
	public void update(float delta) {
		ModelCacheEntry cs = (ModelCacheEntry) currentSource;

		if (cs != null && cs.controller.current != null && cs.controller.current.loopCount != 0) {
			cs.controller.update(delta);
			lastAnimationTime += delta;

			// GENERATE SHADOW MAP
			if (renderShadow)
				genShadowMap();

			if (USE_FBO)
				renderTex();
		}
	}

	@Override
	public float getWidth() {
		return width;
	}

	@Override
	public float getHeight() {
		return height;
	}

	private static final Vector3 tmp = new Vector3();

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scaleX, float scaleY, float rotation, Color tint) {

		x = x - getWidth() / 2 * scaleX;

		if (USE_FBO) {
			if (tint != null)
				batch.setColor(tint);

			batch.draw(tex, x, y, 0, 0, width, height, scaleX, scaleY, 0);

			if (tint != null)
				batch.setColor(Color.WHITE);
		} else {
			float p0x, p0y, pfx, pfy;

			updateViewport();

			// get screen coords for x and y
			tmp.set(x, y, 0);

			tmp.mul(batch.getTransformMatrix());
			tmp.prj(batch.getProjectionMatrix());
			p0x = VIEWPORT.width * (tmp.x + 1) / 2;
			p0y = VIEWPORT.height * (tmp.y + 1) / 2;

			tmp.set(x + width * scaleX, y + height * scaleY, 0);
			tmp.mul(batch.getTransformMatrix());
			tmp.prj(batch.getProjectionMatrix());
			pfx = VIEWPORT.width * (tmp.x + 1) / 2;
			pfy = VIEWPORT.height * (tmp.y + 1) / 2;

			batch.end();

			Gdx.gl20.glViewport((int) (p0x + VIEWPORT.x), (int) (p0y + VIEWPORT.y), (int) (pfx - p0x),
					(int) (pfy - p0y));

			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT
					| (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV : 0));

			drawModel();

			Gdx.gl20.glViewport((int) VIEWPORT.x, (int) VIEWPORT.y, (int) VIEWPORT.width, (int) VIEWPORT.height);
			batch.begin();
		}
	}

	private void createEnvirontment() {
		environment = new Environment();

		// environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f,
		// 0.8f, 0.8f, 1f));

		// environment.add(new DirectionalLight().set(1f, 1f, 1f, 1f, -1f,
		// -1f));

		if (celLight == null) {
			Node n = null;

			if (currentSource != null)
				n = ((ModelCacheEntry) currentSource).modelInstance.getNode(celLightName);

			if (n != null) {
				celLight = new PointLight().set(1f, 1f, 1f, n.translation, 1f);
			} else {
				celLight = new PointLight().set(1f, 1f, 1f, 0.5f, 1f, 1f, 1f);
			}
		}

		environment.add(celLight);

		if (renderShadow) {
			shadowEnvironment = new Environment();
			shadowEnvironment.add(shadowLight);
			shadowEnvironment.shadowMap = shadowLight;
		}
	}

	private static void updateViewport() {
		// GET CURRENT VIEWPORT SIZE
		Gdx.gl20.glGetIntegerv(GL20.GL_VIEWPORT, VIEWPORT_RESULTS);
		VIEWPORT.x = VIEWPORT_RESULTS.get(0);
		VIEWPORT.y = VIEWPORT_RESULTS.get(1);
		VIEWPORT.width = VIEWPORT_RESULTS.get(2);
		VIEWPORT.height = VIEWPORT_RESULTS.get(3);
	}

	public static void createBatchs() {
		Config modelConfigShader = new Config(Gdx.files.classpath(VERTEX_SHADER).readString(),
				Gdx.files.classpath(FRAGMENT_SHADER).readString());

		modelConfigShader.numBones = MAX_BONES;
		modelConfigShader.numDirectionalLights = 0;
		modelConfigShader.numPointLights = 0;
		modelConfigShader.numSpotLights = 0;

		modelBatch = new ModelBatch(new DefaultShaderProvider(modelConfigShader));

		shadowBatch = new ModelBatch(new DepthShaderProvider());
		floorBatch = new ModelBatch(new DefaultShaderProvider(Gdx.files.classpath(VERTEX_SHADER),
				Gdx.files.classpath(FLOOR_FRAGMENT_SHADER)));
	}

	private void loadSource(String source) {
		ModelCacheEntry entry = (ModelCacheEntry) sourceCache.get(source);

		if (entry == null) {
			entry = new ModelCacheEntry();
			sourceCache.put(source, entry);
		}

		if (entry.refCounter == 0) {
			EngineAssetManager.getInstance().loadModel3D(source);
		}

		entry.refCounter++;
	}

	private void retrieveSource(String source) {
		ModelCacheEntry entry = (ModelCacheEntry) sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
			entry = (ModelCacheEntry) sourceCache.get(source);
		}

		if (entry.modelInstance == null) {
			Model model3d = EngineAssetManager.getInstance().getModel3D(source);
			entry.modelInstance = new ModelInstance(model3d);
			entry.controller = new AnimationController(entry.modelInstance);
			entry.camera3d = getCamera(entry.modelInstance);
		}
	}

	private void disposeSource(String source) {
		ModelCacheEntry entry = (ModelCacheEntry) sourceCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeModel3D(source);
			entry.modelInstance = null;
		}

		entry.refCounter--;
	}

	@Override
	public void loadAssets() {
		for (AnimationDesc fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source);
		}

		if (currentAnimation != null && !currentAnimation.preload) {
			loadSource(currentAnimation.source);
		} else if (currentAnimation == null && initAnimation != null) {
			AnimationDesc fa = fanims.get(initAnimation);

			if (!fa.preload)
				loadSource(fa.source);
		}
	}

	@Override
	public void retrieveAssets() {
		// create STATIC BATCHS if not created yet
		if (modelBatch == null)
			createBatchs();

		createEnvirontment();

		for (String key : sourceCache.keySet()) {
			if (sourceCache.get(key).refCounter > 0)
				retrieveSource(key);
		}

		if (currentAnimation != null) { // RESTORE FA
			ModelCacheEntry entry = (ModelCacheEntry) sourceCache.get(currentAnimation.source);
			currentSource = entry;

			float speed = currentAnimation.duration;

			if (currentAnimationType == Tween.Type.REVERSE || currentAnimationType == Tween.Type.REVERSE_REPEAT)
				speed *= -1;

			((ModelCacheEntry) currentSource).controller.setAnimation(currentAnimation.id, currentCount, speed,
					animationListener);

			update(lastAnimationTime);

		} else if (initAnimation != null) {
			startAnimation(initAnimation, Tween.Type.SPRITE_DEFINED, 1, null);

			if (currentAnimation != null)
				lookat(modelRotation);
		}

		if (currentSource != null && renderShadow)
			genShadowMap();

		if (USE_FBO) {
			GLFrameBuffer.FrameBufferBuilder frameBufferBuilder = new GLFrameBuffer.FrameBufferBuilder(width, height);

			frameBufferBuilder.addColorTextureAttachment(GL30.GL_RGBA8, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE);
			fb = frameBufferBuilder.build();

			tex = new TextureRegion(fb.getColorBufferTexture());
			tex.flip(false, true);

			renderTex();
		}

		computeBbox();
	}

	@Override
	public void dispose() {
		for (String key : sourceCache.keySet()) {
			if (sourceCache.get(key).refCounter > 0)
				EngineAssetManager.getInstance().disposeModel3D(key);
		}

		sourceCache.clear();
		currentSource = null;
		environment = null;
		shadowEnvironment = null;

		if (USE_FBO)
			fb.dispose();
	}

	public static void disposeBatchs() {

		if (modelBatch == null)
			return;

		modelBatch.dispose();
		shadowBatch.dispose();
		floorBatch.dispose();

		modelBatch = shadowBatch = floorBatch = null;
	}

	@Override
	public void write(Json json) {
		super.write(json);

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			float worldScale = EngineAssetManager.getInstance().getScale();
			json.writeValue("width", width / worldScale);
			json.writeValue("height", height / worldScale);
			json.writeValue("cameraPos", cameraPos, cameraPos == null ? null : Vector3.class);
			json.writeValue("cameraRot", cameraRot, cameraRot == null ? null : Vector3.class);
			json.writeValue("cameraName", cameraName, cameraName == null ? null : String.class);
			json.writeValue("cameraFOV", cameraFOV);
			json.writeValue("renderShadow", renderShadow);
		} else {

			if (animationCb != null)
				json.writeValue("animationCb",
						ActionCallbackSerializer.find(bjson.getWorld(), bjson.getScene(), animationCb));

			json.writeValue("currentCount", currentCount);
			json.writeValue("currentAnimationType", currentAnimationType);
			json.writeValue("lastAnimationTime", lastAnimationTime);

			// TODO: SAVE AND RESTORE CURRENT DIRECTION
			// TODO: shadowlight, cel light
		}

		json.writeValue("modelRotation", modelRotation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			fanims = json.readValue("fanims", HashMap.class, AnimationDesc.class, jsonData);

			float worldScale = EngineAssetManager.getInstance().getScale();
			width = (int) (json.readValue("width", Integer.class, jsonData) * worldScale);
			height = (int) (json.readValue("height", Integer.class, jsonData) * worldScale);
			cameraPos = json.readValue("cameraPos", Vector3.class, jsonData);
			cameraRot = json.readValue("cameraRot", Vector3.class, jsonData);
			cameraName = json.readValue("cameraName", String.class, jsonData);
			cameraFOV = json.readValue("cameraFOV", Float.class, jsonData);
			renderShadow = json.readValue("renderShadow", Boolean.class, jsonData);
		} else {

			animationCb = ActionCallbackSerializer.find(bjson.getWorld(), bjson.getScene(),
					json.readValue("animationCb", String.class, jsonData));

			currentCount = json.readValue("currentCount", Integer.class, jsonData);
			currentAnimationType = json.readValue("currentAnimationType", Tween.Type.class, jsonData);
			lastAnimationTime = json.readValue("lastAnimationTime", Float.class, jsonData);
		}

		modelRotation = json.readValue("modelRotation", Float.class, jsonData);
	}

}
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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.AtlasAnimationDesc;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.ActionCallbackSerialization;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.Utils3D;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("3d")
@SuppressWarnings("deprecation")
public class Sprite3DRenderer implements ActorRenderer {

	private static final String FRAGMENT_SHADER = "com/bladecoder/engine/shading/cel.fragment.glsl";
	private static final String FLOOR_FRAGMENT_SHADER = "com/bladecoder/engine/shading/floor.fragment.glsl";
	private static final String VERTEX_SHADER = "com/bladecoder/engine/shading/cel.vertex.glsl";
	private final static boolean USE_FBO = false;
	private final static int MAX_BONES = 40;
	private final static Format FRAMEBUFFER_FORMAT = Format.RGBA4444;

	private static final Rectangle VIEWPORT = new Rectangle();
	private final static IntBuffer VIEWPORT_RESULTS = BufferUtils
			.newIntBuffer(16);

	private HashMap<String, AnimationDesc> fanims = new HashMap<String, AnimationDesc>();

	/** Starts this anim the first time that the scene is loaded */
	private String initAnimation;

	private AnimationDesc currentAnimation;
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
	private final DirectionalShadowLight shadowLight = (DirectionalShadowLight) new DirectionalShadowLight(
			1024, 1024, 30f, 30f, 1f, 100f).set(1f, 1f, 1f, 0.01f, -1f, 0.01f);

	PointLight celLight;

	String celLightName = "Light";

	private ActionCallback animationCb = null;
	private String animationCbSer = null;

	private ModelCacheEntry currentSource;
	private HashMap<String, ModelCacheEntry> sourceCache = new HashMap<String, ModelCacheEntry>();
	private float lastAnimationTime = 0;

	private boolean renderShadow = true;
	
	private Polygon bbox;

	class ModelCacheEntry {
		int refCounter;
		ModelInstance modelInstance;
		AnimationController controller;
		PerspectiveCamera camera3d;
	}

	public Sprite3DRenderer() {

	}

	@Override
	public void addAnimation(AnimationDesc fa) {
		if (initAnimation == null)
			initAnimation = fa.getId();

		fanims.put(fa.getId(), fa);
	}

	@Override
	public HashMap<String, AnimationDesc> getAnimations() {
		return fanims;
	}

	@Override
	public void setInitAnimation(String fa) {
		initAnimation = fa;
	}

	@Override
	public String getInitAnimation() {
		return initAnimation;
	}

	@Override
	public AnimationDesc getCurrentAnimation() {
		return currentAnimation;
	}

	@Override
	public String getCurrentAnimationId() {
		return currentAnimation.getId();
	}

	@Override
	public String[] getInternalAnimations(AnimationDesc anim) {
		retrieveSource(anim.getSource());

		Array<Animation> animations = sourceCache.get(anim.getSource()).modelInstance.animations;
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

		fb.end((int) VIEWPORT.x, (int) VIEWPORT.y, (int) VIEWPORT.width,
				(int) VIEWPORT.height);
	}

	/**
	 * Generates the Shadow Map
	 */
	private void genShadowMap() {
		updateViewport();

		shadowLight.begin(Vector3.Zero, currentSource.camera3d.direction);
		shadowBatch.begin(shadowLight.getCamera());
		shadowBatch.render(currentSource.modelInstance);
		shadowBatch.end();
		shadowLight.end();

		Gdx.graphics.getGL20().glViewport((int) VIEWPORT.x, (int) VIEWPORT.y,
				(int) VIEWPORT.width, (int) VIEWPORT.height);
	}

	private void drawModel() {
		if (currentSource != null) {

			// DRAW SHADOW
			if (renderShadow) {
				floorBatch.begin(currentSource.camera3d);
				floorBatch.render(Utils3D.getFloor(), shadowEnvironment);
				floorBatch.end();
			}

			// DRAW MODEL
			modelBatch.begin(currentSource.camera3d);

			if (EngineLogger.debugMode()
					&& EngineLogger.debugLevel == EngineLogger.DEBUG1)
				modelBatch.render(Utils3D.getAxes(), environment);

			modelBatch.render(currentSource.modelInstance, environment);

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

	private PerspectiveCamera getCamera(ModelInstance modelInstance) {
		PerspectiveCamera camera3d = new PerspectiveCamera(cameraFOV, width,
				height);

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
				float rx = (float) (MathUtils.radiansToDegrees * Math.asin(2
						* n.rotation.x * n.rotation.y + 2 * n.rotation.z
						* n.rotation.w));
				float ry = (float) (MathUtils.radiansToDegrees * Math.atan2(2
						* n.rotation.x * n.rotation.w - 2 * n.rotation.y
						* n.rotation.z, 1 - 2 * n.rotation.x * n.rotation.x - 2
						* n.rotation.z * n.rotation.z));
				float rz = (float) (Math.atan2(2 * n.rotation.y * n.rotation.w
						- 2 * n.rotation.x * n.rotation.z, 1 - 2 * n.rotation.y
						* n.rotation.y - 2 * n.rotation.z * n.rotation.z));

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
			if (animationCb != null || animationCbSer != null) {
				if (animationCb == null) {
					animationCb = ActionCallbackSerialization
							.find(animationCbSer);
					animationCbSer = null;
				}

				ActionCallbackQueue.add(animationCb);
			}
		}
	};

	// public Array<Animation> getAnimations() {
	// return modelInstance.animations;
	// }

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count,
			ActionCallback cb) {
		AnimationDesc fa = fanims.get(id);

		if (fa == null) {
			EngineLogger.error("AnimationDesc not found: " + id);

			return;
		}

		if (currentAnimation != null
				&& currentAnimation.isDisposeWhenPlayed())
			disposeSource(currentAnimation.getSource());

		currentAnimation = fa;
		currentSource = sourceCache.get(fa.getSource());
		animationCb = cb;

		if (currentSource == null || currentSource.refCounter < 1) {
			// If the source is not loaded. Load it.
			loadSource(fa.getSource());
			EngineAssetManager.getInstance().finishLoading();

			retrieveSource(fa.getSource());

			currentSource = sourceCache.get(fa.getSource());

			if (currentSource == null) {
				EngineLogger.error("Could not load AnimationDesc: " + id);
				currentAnimation = null;

				return;
			}
		}

		if (repeatType == Tween.Type.SPRITE_DEFINED) {
			currentAnimationType = currentAnimation.getAnimationType();
			currentCount = currentAnimation.getCount();
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		lastAnimationTime = 0;
		float speed = currentAnimation.getSpeed();

		if (currentAnimationType == Tween.Type.REVERSE
				|| currentAnimationType == Tween.Type.REVERSE_REPEAT)
			speed *= -1;

		if (currentSource.modelInstance.getAnimation(id) != null) {
			animationCb = cb;
			currentSource.controller.setAnimation(id, currentCount, speed, animationListener);
			computeBbox();
			return;
		}

		int idx = id.indexOf('.');

		if (idx != -1) {
			String s = id.substring(0, idx);
			String dir = id.substring(idx + 1);

			lookat(dir);

			if (currentSource.modelInstance.getAnimation(s) != null) {
				currentSource.controller.setAnimation(s, count,
						speed, animationListener);

				computeBbox();
				return;
			}
		}

		// ERROR CASE
		EngineLogger.error("Animation NOT FOUND: " + id);

		for (Animation a : currentSource.modelInstance.animations) {
			EngineLogger.debug(a.id);
		}

		if (cb != null) {
			ActionCallbackQueue.add(cb);
		}
		
		computeBbox();
	}
	
	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, String direction) {
		if(direction!=null)
			lookat(direction);
		
		startAnimation(id, repeatType, count, null);
	}

	@Override
	public void startAnimation(String id, Tween.Type repeatType, int count, ActionCallback cb, Vector2 p0, Vector2 pf) {
		Vector2 tmp = new Vector2(pf);
		float angle = tmp.sub(p0).angle() + 90;
		lookat(angle);
		
		startAnimation(id, repeatType, count, null);
	}

	private void lookat(String dir) {
		EngineLogger.debug("LOOKAT DIRECTION - " + dir);

		if (dir.equals(AnimationDesc.BACK))
			lookat(180);
		else if (dir.equals(AnimationDesc.FRONT))
			lookat(0);
		else if (dir.equals(AnimationDesc.LEFT))
			lookat(270);
		else if (dir.equals(AnimationDesc.RIGHT))
			lookat(90);
		else if (dir.equals(AnimationDesc.BACKLEFT))
			lookat(225);
		else if (dir.equals(AnimationDesc.BACKRIGHT))
			lookat(135);
		else if (dir.equals(AnimationDesc.FRONTLEFT))
			lookat(-45);
		else if (dir.equals(AnimationDesc.FRONTRIGHT))
			lookat(45);
		else
			EngineLogger.error("LOOKAT: Direction not found - " + dir);

	}

	private void lookat(float angle) {
		currentSource.modelInstance.transform.setToRotation(Vector3.Y, angle);
		modelRotation = angle;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("\n  Anims:");

		for (Animation a : currentSource.modelInstance.animations) {
			sb.append(" ").append(a.id);
		}

		if (currentSource.controller.current != null)
			sb.append("\n  Current Anim: ").append(
					currentSource.controller.current.animation.id);

		sb.append("\n");

		return sb.toString();
	}

	public void setSpriteSize(Vector2 size) {
		this.width = (int) size.x;
		this.height = (int) size.y;
	}

	@Override
	public void update(float delta) {

		if (currentSource != null && currentSource.controller.current != null
				&& currentSource.controller.current.loopCount != 0) {
			currentSource.controller.update(delta);
			lastAnimationTime += delta;

			// GENERATE SHADOW MAP
			if(renderShadow)
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
	
	@Override
	public void updateBboxFromRenderer(Polygon bbox) {
		this.bbox = bbox;
	}
	
	private void computeBbox() {
		if(bbox == null)
			return;
		
		if(bbox.getVertices() == null || bbox.getVertices().length != 8) {
			bbox.setVertices(new float[8]);
		}
		
		float[] verts = bbox.getVertices();
		
		verts[0] = -getWidth()/2;
		verts[1] = 0f;
		verts[2] = -getWidth()/2;
		verts[3] = getHeight();
		verts[4] = getWidth()/2;
		verts[5] = getHeight();
		verts[6] = getWidth()/2;
		verts[7] = 0f;		
		bbox.dirty();
	}

	@Override
	public void draw(SpriteBatch batch, float x, float y, float scale) {

		x = x - getWidth() / 2 * scale;

		if (USE_FBO) {
			batch.draw(tex, x, y, 0, 0, width, height, scale, scale, 0);
		} else {
			float p0x, p0y, pfx, pfy;

			Vector3 tmp = new Vector3(); // TODO Make static for performance?
			updateViewport();

			// get screen coords for x and y
			tmp.set(x, y, 0);

			tmp.mul(batch.getTransformMatrix());
			tmp.prj(batch.getProjectionMatrix());
			p0x = VIEWPORT.width * (tmp.x + 1) / 2;
			p0y = VIEWPORT.height * (tmp.y + 1) / 2;

			tmp.set(x + width * scale, y + height * scale, 0);
			tmp.mul(batch.getTransformMatrix());
			tmp.prj(batch.getProjectionMatrix());
			pfx = VIEWPORT.width * (tmp.x + 1) / 2;
			pfy = VIEWPORT.height * (tmp.y + 1) / 2;

			batch.end();

			Gdx.gl20.glViewport((int) (p0x + VIEWPORT.x),
					(int) (p0y + VIEWPORT.y), (int) (pfx - p0x),
					(int) (pfy - p0y));

			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT
					| (Gdx.graphics.getBufferFormat().coverageSampling ? GL20.GL_COVERAGE_BUFFER_BIT_NV
							: 0));

			drawModel();

			Gdx.gl20.glViewport((int) VIEWPORT.x, (int) VIEWPORT.y,
					(int) VIEWPORT.width, (int) VIEWPORT.height);
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
				n = currentSource.modelInstance.getNode(celLightName);

			if (n != null) {
				celLight = new PointLight().set(1f, 1f, 1f, n.translation, 1f);
			} else {
				celLight = new PointLight().set(1f, 1f, 1f, 0.5f, 1f, 1f, 1f);
			}
		}

		environment.add(celLight);

		if(renderShadow) {
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
		Config modelConfigShader = new Config(Gdx.files.classpath(
				VERTEX_SHADER).readString(),
				Gdx.files.classpath(
						FRAGMENT_SHADER)
						.readString());

		modelConfigShader.numBones = MAX_BONES;
		modelConfigShader.numDirectionalLights = 0;
		modelConfigShader.numPointLights = 0;
		modelConfigShader.numSpotLights = 0;

		modelBatch = new ModelBatch(
				new DefaultShaderProvider(modelConfigShader));

		shadowBatch = new ModelBatch(new DepthShaderProvider());
		floorBatch = new ModelBatch(
				new DefaultShaderProvider(
						Gdx.files
								.classpath(VERTEX_SHADER),
						Gdx.files
								.classpath(FLOOR_FRAGMENT_SHADER)));
	}

	private void loadSource(String source) {
		ModelCacheEntry entry = sourceCache.get(source);

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
		ModelCacheEntry entry = sourceCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
			entry = sourceCache.get(source);
		}

		if (entry.modelInstance == null) {
			Model model3d = EngineAssetManager.getInstance().getModel3D(source);
			entry.modelInstance = new ModelInstance(model3d);
			entry.controller = new AnimationController(entry.modelInstance);
			entry.camera3d = getCamera(entry.modelInstance);
		}
	}

	private void disposeSource(String source) {
		ModelCacheEntry entry = sourceCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeModel3D(source);
			entry.modelInstance = null;
		}

		entry.refCounter--;
	}

	@Override
	public void loadAssets() {
		for (AnimationDesc fa : fanims.values()) {
			if (fa.isPreload())
				loadSource(fa.getSource());
		}

		if (currentAnimation != null && !currentAnimation.isPreload()) {
			loadSource(currentAnimation.getSource());
		} else if (currentAnimation == null && initAnimation != null) {
			AnimationDesc fa = fanims.get(initAnimation);

			if (!fa.isPreload())
				loadSource(fa.getSource());
		}
	}

	@Override
	public void retrieveAssets() {
		for (String key : sourceCache.keySet()) {
			if (sourceCache.get(key).refCounter > 0)
				retrieveSource(key);
		}

		if (currentAnimation != null) { // RESTORE FA
			ModelCacheEntry entry = sourceCache
					.get(currentAnimation.getSource());
			currentSource = entry;

			float speed = currentAnimation.getSpeed();

			if (currentAnimationType == Tween.Type.REVERSE
					|| currentAnimationType == Tween.Type.REVERSE_REPEAT)
				speed *= -1;
			
			currentSource.controller.setAnimation(currentAnimation.getId(), currentCount, speed, animationListener);
			
			update(lastAnimationTime);

		} else if (initAnimation != null) {
			startAnimation(initAnimation, Tween.Type.SPRITE_DEFINED, 1, null);

			if (currentAnimation != null)
				lookat(modelRotation);
		}

		// create STATIC BATCHS if not created yet
		if (modelBatch == null)
			createBatchs();

		createEnvirontment();

		if (currentSource != null && renderShadow)
			genShadowMap();

		if (USE_FBO) {
			fb = new FrameBuffer(FRAMEBUFFER_FORMAT, width, height, true);

			tex = new TextureRegion(fb.getColorBufferTexture());
			tex.flip(false, true);

			renderTex();
		}
		
		computeBbox();
	}

	@Override
	public void dispose() {
		for (String key : sourceCache.keySet()) {
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
		
		if(modelBatch == null)
			return;
		
		modelBatch.dispose();
		shadowBatch.dispose();
		floorBatch.dispose();

		modelBatch = shadowBatch = floorBatch = null;
	}

	@Override
	public void write(Json json) {
		json.writeValue("fanims", fanims, HashMap.class, AnimationDesc.class);

		String currentAnimationId = null;

		if (currentAnimation != null)
			currentAnimationId = currentAnimation.getId();

		json.writeValue("currentAnimation", currentAnimationId);

		json.writeValue("initAnimation", initAnimation);

		json.writeValue("width", width);
		json.writeValue("height", height);
		json.writeValue("cameraPos", cameraPos, cameraPos == null ? null
				: Vector3.class);
		json.writeValue("cameraRot", cameraRot, cameraRot == null ? null
				: Vector3.class);
		json.writeValue("cameraName", cameraName, cameraName == null ? null
				: String.class);
		json.writeValue("cameraFOV", cameraFOV);
		json.writeValue("modelRotation", modelRotation);

		if (animationCbSer != null)
			json.writeValue("cb", animationCbSer);
		else
			json.writeValue("animationCb",
					ActionCallbackSerialization.find(animationCb),
					animationCb == null ? null : String.class);

		json.writeValue("currentCount", currentCount);
		json.writeValue("currentAnimationType", currentAnimationType.getTweenId());
		json.writeValue("renderShadow", renderShadow);
		json.writeValue("lastAnimationTime", lastAnimationTime);

		// TODO: SAVE AND RESTORE CURRENT DIRECTION
		// TODO: shadowlight, cel light
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		fanims = json.readValue("fanims", HashMap.class,
				AnimationDesc.class, jsonData);

		String currentAnimationId = json.readValue(
				"currentAnimation", String.class, jsonData);

		if (currentAnimationId != null)
			currentAnimation = (AtlasAnimationDesc) fanims
					.get(currentAnimationId);

		initAnimation = json.readValue("initAnimation", String.class,
				jsonData);

		width = json.readValue("width", Integer.class, jsonData);
		height = json.readValue("height", Integer.class, jsonData);
		cameraPos = json.readValue("cameraPos", Vector3.class, jsonData);
		cameraRot = json.readValue("cameraRot", Vector3.class, jsonData);
		cameraName = json.readValue("cameraName", String.class, jsonData);
		cameraFOV = json.readValue("cameraFOV", Float.class, jsonData);
		modelRotation = json.readValue("modelRotation", Float.class, jsonData);
		animationCbSer = json.readValue("animationCb", String.class, jsonData);

		currentCount = json.readValue("currentCount", Integer.class, jsonData);
		currentAnimationType = Tween.Type.fromTweenId(json.readValue("currentAnimationType",
				Integer.class, jsonData));
		renderShadow = json.readValue("renderShadow", Boolean.class, jsonData);
		lastAnimationTime = json.readValue("lastAnimationTime", Float.class, jsonData);
	}
}
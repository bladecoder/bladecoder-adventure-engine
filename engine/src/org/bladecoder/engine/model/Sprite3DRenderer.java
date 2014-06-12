package org.bladecoder.engine.model;

import java.nio.IntBuffer;
import java.util.HashMap;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.ActionCallbackQueue;
import org.bladecoder.engine.anim.FrameAnimation;
import org.bladecoder.engine.anim.Tween;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.util.ActionCallbackSerialization;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.Utils3D;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
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
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Sprite3DRenderer implements SpriteRenderer {

	public final static boolean USE_FBO = false;
	private final static int MAX_BONES = 40;
	private final static Format FRAMEBUFFER_FORMAT = Format.RGBA4444;

	private static final Rectangle VIEWPORT = new Rectangle();
	private final static IntBuffer VIEWPORT_RESULTS = BufferUtils
			.newIntBuffer(16);

	private HashMap<String, FrameAnimation> fanims = new HashMap<String, FrameAnimation>();

	/** Starts this anim the first time that the scene is loaded */
	private String initFrameAnimation;

	private FrameAnimation currentFrameAnimation;
	private int currentCount;
	private int currentAnimationType;

	private TextureRegion tex;

	private Environment environment;
	private Environment shadowEnvironment;

	private FrameBuffer fb = null;

	private int width=200, height=200;

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
	DirectionalShadowLight shadowLight = (DirectionalShadowLight) new DirectionalShadowLight(
			1024, 1024, 30f, 30f, 1f, 100f).set(1f, 1f, 1f, 0.01f, -1f, 0.01f);

	PointLight celLight;

	String celLightName = "Light";

	private ActionCallback animationCb = null;
	private String animationCbSer = null;

	private ModelCacheEntry currentModel;
	private HashMap<String, ModelCacheEntry> modelCache = new HashMap<String, ModelCacheEntry>();

	class ModelCacheEntry {
		int refCounter;
		ModelInstance modelInstance;
		AnimationController controller;
		PerspectiveCamera camera3d;
	}
	
	public Sprite3DRenderer() {
		
	}

	@Override
	public void addFrameAnimation(FrameAnimation fa) {
		if (initFrameAnimation == null)
			initFrameAnimation = fa.id;

		fanims.put(fa.id, fa);
	}
	
	@Override
	public HashMap<String, FrameAnimation> getFrameAnimations() {
		return fanims;
	}

	@Override
	public void setInitFrameAnimation(String fa) {
		initFrameAnimation = fa;
	}

	@Override
	public String getInitFrameAnimation() {
		return initFrameAnimation;
	}

	@Override
	public FrameAnimation getCurrentFrameAnimation() {
		return currentFrameAnimation;
	}

	@Override
	public String getCurrentFrameAnimationId() {
		return currentFrameAnimation.id;
	}

	@Override
	public String[] getInternalAnimations(String source) {
		retrieveSource(source);

		Array<Animation> animations = modelCache.get(source).modelInstance.animations;
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
	 * GENERATE SHADOW MAP
	 */
	private void genShadowMap() {
		updateViewport();

		shadowLight.begin(Vector3.Zero, currentModel.camera3d.direction);
		shadowBatch.begin(shadowLight.getCamera());
		shadowBatch.render(currentModel.modelInstance);
		shadowBatch.end();
		shadowLight.end();

		Gdx.graphics.getGL20().glViewport((int) VIEWPORT.x, (int) VIEWPORT.y,
				(int) VIEWPORT.width, (int) VIEWPORT.height);
	}

	private void drawModel() {
		if (currentModel != null) {

			// DRAW SHADOW
			floorBatch.begin(currentModel.camera3d);
			floorBatch.render(Utils3D.getFloor(), shadowEnvironment);
			floorBatch.end();

			// DRAW MODEL
			modelBatch.begin(currentModel.camera3d);

			if (EngineLogger.debugMode()
					&& EngineLogger.debugLevel == EngineLogger.DEBUG1)
				modelBatch.render(Utils3D.getAxes(), environment);

			modelBatch.render(currentModel.modelInstance, environment);

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
		public void onLoop(AnimationDesc animation) {
		}

		@Override
		public void onEnd(AnimationDesc animation) {
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
	public void startFrameAnimation(String id, int repeatType, int count,
			ActionCallback cb) {
		FrameAnimation fa = fanims.get(id);

		if (fa == null) {
			EngineLogger.error("FrameAnimation not found: " + id);

			return;
		}

		if (currentFrameAnimation != null
				&& currentFrameAnimation.disposeWhenPlayed)
			disposeSource(currentFrameAnimation.source);

		currentFrameAnimation = fa;
		currentModel = modelCache.get(fa.source);
		animationCb = cb;

		if (currentModel == null || currentModel.refCounter < 1) {
			// If the source is not loaded. Load it.
			loadSource(fa.source);
			EngineAssetManager.getInstance().finishLoading();

			retrieveSource(fa.source);
			
			currentModel = modelCache.get(fa.source);
			
			if(currentModel == null) {
				EngineLogger.error("Could not load FrameAnimation: " + id);
				currentFrameAnimation = null;

				return;				
			}
		}

		if (repeatType == Tween.FROM_FA) {
			currentAnimationType = currentFrameAnimation.animationType;
			currentCount = currentFrameAnimation.count;
		} else {
			currentCount = count;
			currentAnimationType = repeatType;
		}

		boolean reverse = false;

		if (currentAnimationType == Tween.REVERSE
				|| currentAnimationType == Tween.REVERSE_REPEAT)
			reverse = true;

		if (currentModel.modelInstance.getAnimation(id) != null) {
			animationCb = cb;
			currentModel.controller.setAnimation(id, currentCount, reverse ? -1
					: 1, animationListener);
			return;
		}

		int idx = id.indexOf('.');

		if (idx != -1) {
			String s = id.substring(0, idx);
			String dir = id.substring(idx + 1);

			lookat(dir);

			if (currentModel.modelInstance.getAnimation(s) != null) {
				currentModel.controller.setAnimation(s, count,
						reverse ? -1 : 1, animationListener);

				return;
			}
		}

		// ERROR CASE
		EngineLogger.error("Animation NOT FOUND: " + id);

		for (Animation a : currentModel.modelInstance.animations) {
			EngineLogger.debug(a.id);
		}

		if (cb != null) {
			ActionCallbackQueue.add(cb);
		}
	}

	@Override
	public void lookat(String dir) {
		EngineLogger.debug("LOOKAT DIRECTION - " + dir);

		if (dir.equals(FrameAnimation.BACK))
			lookat(180);
		else if (dir.equals(FrameAnimation.FRONT))
			lookat(0);
		else if (dir.equals(FrameAnimation.LEFT))
			lookat(270);
		else if (dir.equals(FrameAnimation.RIGHT))
			lookat(90);
		else if (dir.equals(FrameAnimation.BACKLEFT))
			lookat(225);
		else if (dir.equals(FrameAnimation.BACKRIGHT))
			lookat(135);
		else if (dir.equals(FrameAnimation.FRONTLEFT))
			lookat(-45);
		else if (dir.equals(FrameAnimation.FRONTRIGHT))
			lookat(45);
		else
			EngineLogger.error("LOOKAT: Direction not found - " + dir);

	}

	@Override
	public void lookat(float x, float y, Vector2 pf) {
		Vector2 tmp = new Vector2(pf);
		float angle = tmp.sub(x, y).angle() + 90;
		lookat(angle);
	}

	private void lookat(float angle) {
		currentModel.modelInstance.transform.setToRotation(Vector3.Y, angle);
		modelRotation = angle;
	}

	@Override
	public void stand() {
		startFrameAnimation(FrameAnimation.STAND_ANIM, Tween.NO_REPEAT,
				1, null);
	}

	@Override
	public void startWalkFA(Vector2 p0, Vector2 pf) {
		lookat(p0.x, p0.y, pf);
		startFrameAnimation(FrameAnimation.WALK_ANIM, Tween.REPEAT, -1,
				null);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());

		sb.append("\n  Anims:");

		for (Animation a : currentModel.modelInstance.animations) {
			sb.append(" ").append(a.id);
		}

		if (currentModel.controller.current != null)
			sb.append("\n  Current Anim: ").append(
					currentModel.controller.current.animation.id);

		sb.append("\n");

		return sb.toString();
	}

	public void setSpriteSize(Vector2 size) {
		this.width = (int) size.x;
		this.height = (int) size.y;
	}

	@Override
	public void update(float delta) {

		if (currentModel != null && currentModel.controller.current != null
				&& currentModel.controller.current.loopCount != 0) {
			currentModel.controller.update(delta);

			// GENERATE SHADOW MAP
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
	public void draw(SpriteBatch batch, float x, float y,float scale) {
		
		x = x - getWidth() / 2 * scale;
		
		if (USE_FBO) {
			batch.draw(tex, x, y, 0, 0, width, height, scale,
					scale, 0);
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
		shadowEnvironment = new Environment();
		// environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f,
		// 0.8f, 0.8f, 1f));

		// environment.add(new DirectionalLight().set(1f, 1f, 1f, 1f, -1f,
		// -1f));

		if (celLight == null) {
			Node n = null;

			if (currentModel != null)
				n = currentModel.modelInstance.getNode(celLightName);
			
			if (n != null) {
				celLight = new PointLight().set(1f, 1f, 1f, n.translation, 1f);
			} else {
				celLight = new PointLight().set(1f, 1f, 1f, 0.5f, 1f, 1f, 1f);
			}
		}

		environment.add(celLight);

		shadowEnvironment.add(shadowLight);
		shadowEnvironment.shadowMap = shadowLight;
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
				"org/bladecoder/engine/shading/cel.vertex.glsl").readString(),
				Gdx.files.classpath(
						"org/bladecoder/engine/shading/cel.fragment.glsl")
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
								.classpath("org/bladecoder/engine/shading/cel.vertex.glsl"),
						Gdx.files
								.classpath("org/bladecoder/engine/shading/floor.fragment.glsl")));
	}

	private void loadSource(String source) {
		ModelCacheEntry entry = modelCache.get(source);

		if (entry == null) {
			entry = new ModelCacheEntry();
			modelCache.put(source, entry);
		}

		if (entry.refCounter == 0) {
			EngineAssetManager.getInstance().loadModel3D(source);
		}

		entry.refCounter++;
	}

	private void retrieveSource(String source) {
		ModelCacheEntry entry = modelCache.get(source);

		if (entry == null || entry.refCounter < 1) {
			loadSource(source);
			EngineAssetManager.getInstance().finishLoading();
			entry = modelCache.get(source);
		}

		if (entry.modelInstance == null) {
			Model model3d = EngineAssetManager.getInstance().getModel3D(source);
			entry.modelInstance = new ModelInstance(model3d);
			entry.controller = new AnimationController(entry.modelInstance);
			entry.camera3d = getCamera(entry.modelInstance);
		}
	}

	private void disposeSource(String source) {
		ModelCacheEntry entry = modelCache.get(source);

		if (entry.refCounter == 1) {
			EngineAssetManager.getInstance().disposeModel3D(source);
			entry.modelInstance = null;
		}

		entry.refCounter--;
	}

	@Override
	public void loadAssets() {
		for (FrameAnimation fa : fanims.values()) {
			if (fa.preload)
				loadSource(fa.source);
		}

		if (currentFrameAnimation != null && !currentFrameAnimation.preload) {
			loadSource(currentFrameAnimation.source);
		} else if (currentFrameAnimation == null && initFrameAnimation != null) {
			FrameAnimation fa = fanims.get(initFrameAnimation);

			if (!fa.preload)
				loadSource(fa.source);
		}
	}

	@Override
	public void retrieveAssets() {
		for (String key : modelCache.keySet()) {
			if (modelCache.get(key).refCounter > 0)
				retrieveSource(key);
		}

		if (currentFrameAnimation != null) {
			ModelCacheEntry entry = modelCache
					.get(currentFrameAnimation.source);
			currentModel = entry;

			// TODO RESTORE CURRENT ANIMATION STATE

		} else if(initFrameAnimation != null){
			startFrameAnimation(initFrameAnimation, Tween.FROM_FA, 1,
					null);

			if (currentFrameAnimation != null)
				lookat(modelRotation);
		}

		// create STATIC BATCHS if not created yet
		if (modelBatch == null)
			createBatchs();

		createEnvirontment();

		if (currentModel != null)
			genShadowMap();

		if (USE_FBO) {
			fb = new FrameBuffer(FRAMEBUFFER_FORMAT, width, height, true) {
				@Override
				protected void setupTexture() {
					colorTexture = new Texture(width, height, format);
					colorTexture.setFilter(TextureFilter.Linear,
							TextureFilter.Linear);
					colorTexture.setWrap(TextureWrap.ClampToEdge,
							TextureWrap.ClampToEdge);
				}
			};

			tex = new TextureRegion(fb.getColorBufferTexture());
			tex.flip(false, true);

			renderTex();
		}
	}

	@Override
	public void dispose() {
		for (String key : modelCache.keySet()) {
			EngineAssetManager.getInstance().disposeModel3D(key);
		}

		modelCache.clear();
		currentModel = null;
		environment = null;
		shadowEnvironment = null;

		if (USE_FBO)
			fb.dispose();
	}

	public static void disposeBatchs() {
		modelBatch.dispose();
		shadowBatch.dispose();
		floorBatch.dispose();

		modelBatch = shadowBatch = floorBatch = null;
	}

	@Override
	public void write(Json json) {

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
		json.writeValue("animationCb",
				ActionCallbackSerialization.find(animationCb),
				animationCb == null ? null : String.class);

		// TODO: SAVE AND RESTORE CURRENT ANIMATION
		// TODO: shadowlight, cel light
	}

	@Override
	public void read(Json json, JsonValue jsonData) {

		width = json.readValue("width", Integer.class, jsonData);
		height = json.readValue("height", Integer.class, jsonData);
		cameraPos = json.readValue("cameraPos", Vector3.class, jsonData);
		cameraRot = json.readValue("cameraRot", Vector3.class, jsonData);
		cameraName = json.readValue("cameraName", String.class, jsonData);
		cameraFOV = json.readValue("cameraFOV", Float.class, jsonData);
		modelRotation = json.readValue("modelRotation", Float.class, jsonData);
		animationCbSer = json.readValue("animationCb", String.class, jsonData);
	}
}
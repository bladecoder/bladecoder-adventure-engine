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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.ScreenUtils;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.Timers;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.loader.SerializationHelper;
import com.bladecoder.engine.loader.SerializationHelper.Mode;
import com.bladecoder.engine.loader.WorldXMLLoader;
import com.bladecoder.engine.util.EngineLogger;

public class World implements Serializable, AssetConsumer {

	public static final String GAMESTATE_EXT = ".gamestate.v11";
	private static final String GAMESTATE_FILENAME = "default" + GAMESTATE_EXT;

	private static final int SCREENSHOT_DEFAULT_WIDTH = 300;

	public static enum AssetState {
		LOADED, LOADING, LOADING_AND_INIT_SCENE, LOAD_ASSETS, LOAD_ASSETS_AND_INIT_SCENE
	};

	private static final boolean CACHE_ENABLED = true;

	private static final World instance = new World();

	private AssetState assetState;

	private int width;
	private int height;

	private String initScene;
	private HashMap<String, Scene> scenes = new HashMap<String, Scene>();
	private final VerbManager verbs = new VerbManager();

	private Scene currentScene;
	private Dialog currentDialog;

	private Inventory inventory;
	private TextManager textManager;

	private boolean paused;
	private boolean cutMode;

	/** keep track of the time of game */
	private float timeOfGame;

	/** for debug purposes, keep track of loading time */
	private long initLoadingTime;

	private Timers timers;
	private boolean disposed;

	/**
	 * If not null, this scene is set as the currentScene and the test Verb is
	 * executed
	 */
	private String testScene;

	/**
	 * Add support for the use of global custom properties/variables in the game
	 * logic
	 */
	private HashMap<String, String> customProperties;

	private String initChapter;
	private String currentChapter;

	/** For FADEIN/FADEOUT */
	private Transition transition;

	transient private SpriteBatch spriteBatch;

	// We not dispose the last loaded scene.
	// Instead we cache it to improve performance when returning
	transient private Scene cachedScene;

	public static World getInstance() {
		return instance;
	}

	private World() {
		disposed = true;
	}

	private void init() {
		// scenes = new HashMap<String, Scene>();
		scenes.clear();
		inventory = new Inventory();
		textManager = new TextManager();

		timers = new Timers();

		cutMode = false;
		timeOfGame = 0;
		currentChapter = null;
		cachedScene = null;

		customProperties = new HashMap<String, String>();

		spriteBatch = new SpriteBatch();

		transition = new Transition();
		paused = false;

		disposed = false;
	}

	public void addTimer(float time, ActionCallback cb) {
		timers.addTimer(time, cb);
	}

	public String getCustomProperty(String name) {
		return customProperties.get(name);
	}

	public void setCustomProperty(String name, String value) {
		if (value == null)
			customProperties.remove(name);
		else
			customProperties.put(name, value);
	}

	public VerbManager getVerbManager() {
		return verbs;
	}

	public void draw() {
		if (assetState == AssetState.LOADED) {

			spriteBatch.setProjectionMatrix(currentScene.getCamera().combined);
			spriteBatch.begin();
			getCurrentScene().draw(spriteBatch);
			spriteBatch.end();
		}
	}

	public void update(float delta) {
		if (assetState == AssetState.LOAD_ASSETS || assetState == AssetState.LOAD_ASSETS_AND_INIT_SCENE) {
			loadAssets();

			if (assetState == AssetState.LOAD_ASSETS)
				assetState = AssetState.LOADING;
			else
				assetState = AssetState.LOADING_AND_INIT_SCENE;

			// initLoadingTime = System.currentTimeMillis();

			// Try to load scene for 100ms before. If not loaded in this time,
			// show the loading screen
			float t0 = System.currentTimeMillis();
			float t = 0f;
			while (EngineAssetManager.getInstance().isLoading() && t - t0 < 100f) {
				t = System.currentTimeMillis();
			}

		}

		if ((assetState == AssetState.LOADING || assetState == AssetState.LOADING_AND_INIT_SCENE)
				&& !EngineAssetManager.getInstance().isLoading()) {

			retrieveAssets();

			boolean initScene = (assetState == AssetState.LOADING_AND_INIT_SCENE);

			assetState = AssetState.LOADED;

			EngineLogger.debug("ASSETS LOADING TIME (ms): " + (System.currentTimeMillis() - initLoadingTime));

			// call 'init' verb only when arrives from setCurrentScene and not
			// from load or restoring
			if (initScene) {
				initCurrentScene();
			}

		}

		if (paused || assetState != AssetState.LOADED)
			return;

		timeOfGame += delta;

		getCurrentScene().update(delta);
		textManager.update(delta);
		timers.update(delta);

		if (!transition.isFinish()) {
			transition.update(delta);
		}

		ActionCallbackQueue.run();
	}

	@Override
	public void loadAssets() {
		currentScene.loadAssets();

		if (inventory.isDisposed())
			inventory.loadAssets();
	}

	@Override
	public void retrieveAssets() {
		if (inventory.isDisposed())
			inventory.retrieveAssets();

		getCurrentScene().retrieveAssets();

		// Print loaded assets for scene
		if (EngineLogger.debugMode()) {
			Array<String> assetNames = EngineAssetManager.getInstance().getAssetNames();

			assetNames.sort();

			EngineLogger.debug("Assets loaded for SCENE: " + currentScene.getId());

			for (String n : assetNames) {
				EngineLogger.debug("\t" + n);
			}
		}
	}

	public Transition getTransition() {
		return transition;
	}

	public float getTimeOfGame() {
		return timeOfGame;
	}

	public AssetState getAssetState() {
		return assetState;
	}

	public Dialog getCurrentDialog() {
		return currentDialog;
	}

	public Scene getCurrentScene() {
		return currentScene;
	}

	public String getInitScene() {
		return initScene;
	}

	public void setInitScene(String initScene) {
		this.initScene = initScene;
	}

	public void setCurrentScene(Scene scene) {

		initLoadingTime = System.currentTimeMillis();

		// Clear all pending callbacks
		ActionCallbackQueue.clear();

		if (cachedScene == scene) {
			assetState = AssetState.LOADING_AND_INIT_SCENE;
		} else {
			if (cachedScene != null) {
				cachedScene.dispose();
				cachedScene = null;
			}

			assetState = AssetState.LOAD_ASSETS_AND_INIT_SCENE;
		}

		if (currentScene != null) {
			testScene = null;
			textManager.reset();
			timers.clear();
			currentScene.stopMusic();
			currentDialog = null;

			// TODO Stop sounds

			if (CACHE_ENABLED)
				cachedScene = currentScene; // CACHE ENABLED
			else
				currentScene.dispose(); // CACHE DISABLED

			transition.reset();
		}

		currentScene = scene;
	}

	public void initCurrentScene() {
		// If in test mode run 'test' verb
		if (testScene != null && testScene.equals(currentScene.getId()) && currentScene.getVerb("test") != null)
			currentScene.runVerb("test");

		// Run INIT action
		if (currentScene.getVerb("init") != null)
			currentScene.runVerb("init");
	}

	public Inventory getInventory() {
		return inventory;
	}

	public TextManager getTextManager() {
		return textManager;
	}

	public void addScene(Scene scene) {
		scenes.put(scene.getId(), scene);
	}

	public Scene getScene(String id) {
		return scenes.get(id);
	}

	public HashMap<String, Scene> getScenes() {
		return scenes;
	}

	public void setCutMode(boolean v) {
		cutMode = v;
	}

	public void setCurrentScene(String id) {
		Scene s = scenes.get(id);

		if (s != null) {
			setCurrentScene(s);
		} else {
			EngineLogger.error("SetCurrentScene - COULD NOT FIND SCENE: " + id);
		}
	}

	public void setCurrentDialog(Dialog dialog) {
		this.currentDialog = dialog;
		if (dialog != null) {
			dialog.reset();

			int visibleOptions = dialog.getNumVisibleOptions();

			if (visibleOptions == 0)
				currentDialog = null;
			else if (visibleOptions == 1) {
				selectVisibleDialogOption(0);
			}
		}
	}

	public void selectVisibleDialogOption(int i) {
		if (currentDialog == null)
			return;

		currentDialog = currentDialog.selectOption(currentDialog.getVisibleOptions().get(i));
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void showInventory(boolean b) {
		inventory.setVisible(b);
	}

	public boolean isDisposed() {
		return disposed;
	}

	@Override
	public void dispose() {

		try {

			textManager.reset();
			timers.clear();
			currentScene.stopMusic();
			currentDialog = null;

			transition.reset();

			// Clear all pending callbacks
			ActionCallbackQueue.clear();

			// ONLY dispose currentscene because other scenes are already
			// disposed
			currentScene.dispose();
			currentScene = null;

			if (cachedScene != null) {
				cachedScene.dispose();
				cachedScene = null;
			}

			inventory.dispose();

			spriteBatch.dispose();

			Sprite3DRenderer.disposeBatchs();

		} catch (Exception e) {
			EngineLogger.error(e.getMessage());
		}

		disposed = true;
	}

	public SceneCamera getSceneCamera() {
		return currentScene.getCamera();
	}

	public void resize(float viewportWidth, float viewportHeight) {
		currentScene.getCamera().viewportWidth = viewportWidth;
		currentScene.getCamera().viewportHeight = viewportHeight;

		if (currentScene.getCameraFollowActor() != null)
			currentScene.getCamera().updatePos(currentScene.getCameraFollowActor());

		currentScene.getCamera().update();
	}

	public void setChapter(String chapter) {
		this.currentChapter = chapter;
	}

	public String getInitChapter() {
		return initChapter;
	}

	public void setInitChapter(String initChapter) {
		this.initChapter = initChapter;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean inCutMode() {
		return cutMode;
	}

	public void pause() {
		paused = true;

		if (currentScene != null)
			currentScene.pauseMusic();

		// TODO Pause all sounds
	}

	public void resume() {
		if (assetState == AssetState.LOADED) {
			paused = false;

			if (currentScene != null)
				currentScene.resumeMusic();

			// TODO Resume all sounds
		}
	}

	public void newGame() throws Exception {
		loadChapter(null);
	}

	// ********** SERIALIZATION **********

	/**
	 * Try to load the save game if exists. In other case, load the game from
	 * XML.
	 * @throws Exception 
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void load() throws Exception {
		if (EngineAssetManager.getInstance().getUserFile(GAMESTATE_FILENAME).exists()) {
			// SAVEGAME EXISTS
			try {
				instance.loadGameState();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING SAVED GAME", e);
				instance.loadChapter(null);
			}
		} else {
			// XML LOADING
			instance.loadChapter(null);
		}
	}

	/**
	 * Load the world description. First try to load 'world.json'. If doesn't
	 * exists try 'world.xml'.
	 * @throws IOException 
	 */
	public void loadWorldDesc() throws IOException {
		if (EngineAssetManager.getInstance().getModelFile(EngineAssetManager.WORLD_FILENAME_JSON).exists()) {
			SerializationHelper.getInstance().setMode(Mode.MODEL);

			JsonValue root = new JsonReader().parse(EngineAssetManager.getInstance()
					.getModelFile(EngineAssetManager.WORLD_FILENAME_JSON).reader("UTF-8"));

			Json json = new Json();

			int width = json.readValue("width", Integer.class, root);
			int height = json.readValue("height", Integer.class, root);

			// When we know the world width, we can put the scale
			EngineAssetManager.getInstance().setScale(width, height);
			float scale = EngineAssetManager.getInstance().getScale();

			setWidth((int) (width * scale));
			setHeight((int) (height * scale));
			setInitChapter(json.readValue("initChapter", String.class, root));
			verbs.read(json, root);
			I18N.loadWorld(EngineAssetManager.MODEL_DIR + "world");
		} else {
			try {
				WorldXMLLoader.loadWorld(this);
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING WORLD XML", e);
				dispose();
				throw new IOException(e);
			}
		}
	}

	public void saveWorldDesc(FileHandle file) throws IOException {

		float scale = EngineAssetManager.getInstance().getScale();

		Json json = new Json();
		json.setOutputType(OutputType.javascript);

		SerializationHelper.getInstance().setMode(Mode.MODEL);

		json.setWriter(new StringWriter());

		json.writeObjectStart();
		json.writeValue("width", width / scale);
		json.writeValue("height", height / scale);
		json.writeValue("initChapter", initChapter);
		verbs.write(json);
		json.writeObjectEnd();

		String s = null;

		if (EngineLogger.debugMode())
			s = json.prettyPrint(json.getWriter().getWriter().toString());
		else
			s = json.getWriter().getWriter().toString();

		Writer w = file.writer(false, "UTF-8");
		w.write(s);
		w.close();
	}

	public void loadChapter(String chapterName) throws IOException {
		if (!disposed)
			dispose();

		init();

		assetState = AssetState.LOAD_ASSETS;

		long initTime = System.currentTimeMillis();

		SerializationHelper.getInstance().setMode(Mode.MODEL);

		if (chapterName == null)
			chapterName = initChapter;
		
		currentChapter = initChapter;

		if (EngineAssetManager.getInstance().getModelFile(chapterName + EngineAssetManager.CHAPTER_EXT).exists()) {

			JsonValue root = new JsonReader().parse(EngineAssetManager.getInstance()
					.getModelFile(chapterName + EngineAssetManager.CHAPTER_EXT).reader("UTF-8"));

			Json json = new Json();

			read(json, root);

			I18N.loadChapter(EngineAssetManager.MODEL_DIR + chapterName);
		} else {
			try {
				WorldXMLLoader.loadChapter(chapterName, this);
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);
				dispose();
				throw new IOException(e);
			}
		}

		EngineLogger.debug("MODEL LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
	}

	public void loadChapter(String chapter, String scene) throws Exception {
		this.testScene = scene;

		loadChapter(chapter);

		if (testScene != null) {
			currentScene = null;
			setCurrentScene(testScene);
		}
	}

	public boolean savedGameExists() {
		return savedGameExists(GAMESTATE_FILENAME);
	}

	public boolean savedGameExists(String filename) {
		return EngineAssetManager.getInstance().getUserFile(filename).exists();
	}

	public void loadGameState() throws IOException {
		long initTime = System.currentTimeMillis();
		loadGameState(GAMESTATE_FILENAME);
		EngineLogger.debug("JSON LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
	}

	public void loadGameState(String filename) throws IOException {
		FileHandle savedFile = EngineAssetManager.getInstance().getUserFile(filename);

		loadGameState(savedFile);
	}

	public void loadGameState(FileHandle savedFile) throws IOException {
		EngineLogger.debug("LOADING GAME STATE");

		if (!disposed)
			dispose();

		init();

		if (savedFile.exists()) {
			assetState = AssetState.LOAD_ASSETS;

			// new Json().fromJson(World.class, savedFile.reader("UTF-8"));

			SerializationHelper.getInstance().setMode(Mode.STATE);

			JsonValue root = new JsonReader().parse(savedFile.reader("UTF-8"));

			read(new Json(), root);

		} else {
			throw new IOException("LOADGAMESTATE: no saved game exists");
		}
	}

	public void saveGameState() throws IOException {
		saveGameState(GAMESTATE_FILENAME);
	}

	public void saveGameState(String filename) throws IOException {
		EngineLogger.debug("SAVING GAME STATE");

		if (disposed)
			return;

		Json json = new Json();
		json.setOutputType(OutputType.javascript);

		String s = null;

		SerializationHelper.getInstance().setMode(Mode.STATE);

		if (EngineLogger.debugMode())
			s = json.prettyPrint(this);
		else
			s = json.toJson(this);

		Writer w = EngineAssetManager.getInstance().getUserFile(filename).writer(false, "UTF-8");

		try {
			w.write(s);
			w.close();
		} catch (IOException e) {
			throw new IOException("ERROR SAVING GAME", e);
		}

		// Save Screenshot
		takeScreenshot(filename + ".png", SCREENSHOT_DEFAULT_WIDTH);
	}

	public void saveModel(String chapterId) throws IOException {
		EngineLogger.debug("SAVING GAME MODEL");

		if (disposed)
			return;

		Json json = new Json();
		json.setOutputType(OutputType.javascript);

		String s = null;

		SerializationHelper.getInstance().setMode(Mode.MODEL);

		if (EngineLogger.debugMode())
			s = json.prettyPrint(this);
		else
			s = json.toJson(this);

		Writer w = EngineAssetManager.getInstance().getModelFile(chapterId + EngineAssetManager.CHAPTER_EXT)
				.writer(false, "UTF-8");

		try {
			w.write(s);
			w.close();
		} catch (IOException e) {
			throw new IOException("ERROR SAVING MODEL", e);
		}
	}

	public void takeScreenshot(String filename, int w) {

		int h = (int) (w * ((float) height) / (float) width);

		FrameBuffer fbo = new FrameBuffer(Format.RGB565, w, h, false);

		fbo.begin();
		draw();
		Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);
		fbo.end();

		// Flip the pixmap upside down
		ByteBuffer pixels = pixmap.getPixels();
		int numBytes = w * h * 4;
		byte[] lines = new byte[numBytes];
		int numBytesPerLine = w * 4;
		for (int i = 0; i < h; i++) {
			pixels.position((h - i - 1) * numBytesPerLine);
			pixels.get(lines, i * numBytesPerLine, numBytesPerLine);
		}
		pixels.clear();
		pixels.put(lines);

		PixmapIO.writePNG(EngineAssetManager.getInstance().getUserFile(filename), pixmap);
	}

	@Override
	public void write(Json json) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

			json.writeValue("scenes", scenes, scenes.getClass(), Scene.class);
			json.writeValue("initScene", initScene);

		} else {

			json.writeValue("scenes", scenes, scenes.getClass(), Scene.class);
			json.writeValue("currentScene", currentScene.getId());
			json.writeValue("inventory", inventory);
			json.writeValue("timeOfGame", timeOfGame);
			json.writeValue("cutmode", cutMode);
			verbs.write(json);
			json.writeValue("timers", timers);
			json.writeValue("textmanager", textManager);
			json.writeValue("customProperties", customProperties);

			if (currentDialog != null) {
				json.writeValue("dialogActor", currentDialog.getActor());
				json.writeValue("currentDialog", currentDialog.getId());
			}

			if(transition != null)
				json.writeValue("transition", transition);

			json.writeValue("chapter", currentChapter);
			ActionCallbackQueue.write(json);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {

			scenes = json.readValue("scenes", HashMap.class, Scene.class, jsonData);
			initScene = json.readValue("initScene", String.class, jsonData);

			if (initScene == null && scenes.size() > 0) {
				initScene = scenes.keySet().toArray(new String[0])[0];
			}

			for (Scene s : scenes.values()) {
				s.resetCamera(width, height);
			}
			
			setCurrentScene(initScene);
		} else {
			currentChapter = json.readValue("chapter", String.class, jsonData);
			
			try {
				loadChapter(currentChapter);
			} catch (IOException e1) {
				EngineLogger.error("Error Loading Chapter");
				return;
			}
			
			// restore the state after loading the model
			SerializationHelper.getInstance().setMode(Mode.STATE);

			currentScene = scenes.get(json.readValue("currentScene", String.class, jsonData));
			assetState = AssetState.LOAD_ASSETS;

			for (Scene s : scenes.values()) {
				s.read(json, jsonData.get("scenes").get(s.getId()));
			}

			inventory = json.readValue("inventory", Inventory.class, jsonData);

			timeOfGame = json.readValue("timeOfGame", Float.class, jsonData);
			cutMode = json.readValue("cutmode", Boolean.class, jsonData);

			verbs.read(json, jsonData);

			timers = json.readValue("timers", Timers.class, jsonData);

			textManager = json.readValue("textmanager", TextManager.class, jsonData);
			customProperties = json.readValue("customProperties", HashMap.class, String.class, jsonData);

			String actorId = json.readValue("dialogActor", String.class, jsonData);
			String dialogId = json.readValue("currentDialog", String.class, jsonData);

			if (dialogId != null) {
				CharacterActor actor = (CharacterActor) getCurrentScene().getActor(actorId, false);
				currentDialog = actor.getDialog(dialogId);
			}

			transition = json.readValue("transition", Transition.class, jsonData);

			ActionCallbackQueue.read(json, jsonData);

			I18N.loadChapter(EngineAssetManager.MODEL_DIR + instance.currentChapter);
		}
	}
}

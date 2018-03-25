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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.minimalcode.beans.ObjectWrapper;
import org.xml.sax.SAXException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.actions.PlaySoundAction;
import com.bladecoder.engine.actions.SoundAction;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.anim.Timers;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.ink.InkManager;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.FileUtils;
import com.bladecoder.engine.util.SerializationHelper;
import com.bladecoder.engine.util.SerializationHelper.Mode;

public class World implements Serializable, AssetConsumer {

	public static final String GAMESTATE_EXT = ".gamestate.v12";
	private static final String GAMESTATE_FILENAME = "default" + GAMESTATE_EXT;

	private static final String DEFAULT_INVENTORY = "DEFAULT";

	private static final int SCREENSHOT_DEFAULT_WIDTH = 300;

	public static enum AssetState {
		LOADED, LOADING, LOADING_AND_INIT_SCENE, LOAD_ASSETS, LOAD_ASSETS_AND_INIT_SCENE
	};

	public static enum WorldProperties {
		SAVED_GAME_VERSION, PREVIOUS_SCENE, CURRENT_CHAPTER, PLATFORM
	};

	private static final boolean CACHE_ENABLED = true;

	private static final World instance = new World();

	// ------------ WORLD PROPERTIES ------------
	private int width;
	private int height;

	private String initScene;
	private HashMap<String, SoundDesc> sounds;
	private Map<String, Scene> scenes;
	private final VerbManager verbs = new VerbManager();

	private Scene currentScene;
	private Dialog currentDialog;

	private Map<String, Inventory> inventories;
	private String currentInventory;

	private UIActors uiActors;

	private TextManager textManager;

	private boolean paused;
	private boolean cutMode;

	// Keep track of the time of game in ms.
	private long timeOfGame;

	private Timers timers;

	// Add support for the use of global custom properties/variables in the game
	// logic
	private HashMap<String, String> customProperties;

	private String initChapter;
	private String currentChapter;

	// For FADEIN/FADEOUT
	private Transition transition;

	private MusicManager musicManager;

	// ------------ LAZY CREATED OBJECTS ------------
	private InkManager inkManager;
	private ObjectWrapper wrapper;

	// ------------ TRANSIENT OBJECTS ------------
	private AssetState assetState;
	private boolean disposed;
	transient private SpriteBatch spriteBatch;

	// for debug purposes, keep track of loading time
	private long initLoadingTime;

	// We not dispose the last loaded scene.
	// Instead we cache it to improve performance when returning
	transient private Scene cachedScene;

	// If not null, this scene is set as the currentScene and the test Verb is
	// executed
	private String testScene;

	// If true call 'initNewGame' or 'initSavedGame' verbs.
	private boolean initGame;

	public static World getInstance() {
		return instance;
	}

	private World() {
		disposed = true;
	}

	private void init() {

		sounds = null;
		scenes = null;
		inventories = new HashMap<String, Inventory>();
		inventories.put(DEFAULT_INVENTORY, new Inventory());
		currentInventory = DEFAULT_INVENTORY;
		uiActors = new UIActors();
		textManager = new TextManager();

		timers = new Timers();

		cutMode = false;
		currentChapter = null;
		cachedScene = null;

		customProperties = new HashMap<String, String>();

		spriteBatch = new SpriteBatch();

		transition = new Transition();

		musicManager = new MusicManager();

		paused = false;

		disposed = false;

		initGame = true;
	}

	public Timers getTimers() {
		return timers;
	}

	public InkManager getInkManager() {
		// Lazy creation
		if (inkManager == null) {
			// Allow not link the Blade Ink Engine library if you don't use Ink
			try {
				Class.forName("com.bladecoder.ink.runtime.Story");
				inkManager = new InkManager();
			} catch (ClassNotFoundException e) {
				EngineLogger.debug("WARNING: Blade Ink Library not found.");
			}
		}

		return inkManager;
	}

	/**
	 * Returns a scene from the cache. null if the scene is not cached.
	 * 
	 * Note that by now, the cache has only one Scene. In the future, the cache
	 * will be a Hastable.
	 */
	public Scene getCachedScene(String id) {

		if (cachedScene != null && cachedScene.getId().equals(id))
			return cachedScene;

		return null;
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

	public MusicManager getMusicManager() {
		return musicManager;
	}

	public HashMap<String, SoundDesc> getSounds() {
		return sounds;
	}

	public void draw() {
		if (assetState == AssetState.LOADED) {
			getCurrentScene().draw(spriteBatch);
			uiActors.draw(spriteBatch);
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
		}

		if ((assetState == AssetState.LOADING || assetState == AssetState.LOADING_AND_INIT_SCENE)
				&& !EngineAssetManager.getInstance().isLoading()) {

			retrieveAssets();

			paused = false;

			boolean initScene = (assetState == AssetState.LOADING_AND_INIT_SCENE);

			assetState = AssetState.LOADED;

			EngineLogger.debug("ASSETS LOADING TIME (ms): " + (System.currentTimeMillis() - initLoadingTime));

			if (initGame) {
				initGame = false;

				// Call world init verbs. Check for SAVED_GAME_VERSION property
				// to know if new or loaded game.
				if (customProperties.get(WorldProperties.SAVED_GAME_VERSION.toString()) == null
						&& verbs.getVerb(Verb.INIT_NEW_GAME_VERB, null, null) != null)
					verbs.runVerb(Verb.INIT_NEW_GAME_VERB, null, null);
				else if (customProperties.get(WorldProperties.SAVED_GAME_VERSION.toString()) != null
						&& verbs.getVerb(Verb.INIT_SAVED_GAME_VERB, null, null) != null)
					verbs.runVerb(Verb.INIT_SAVED_GAME_VERB, null, null);
			}

			// call 'init' verb only when arrives from setCurrentScene and not
			// from load or restoring
			if (initScene) {
				// If in test mode run 'test' verb (only the first time)
				if (testScene != null && testScene.equals(currentScene.getId())
						&& currentScene.getVerb(Verb.TEST_VERB) != null) {
					currentScene.runVerb(Verb.TEST_VERB);
					testScene = null;
				}

				initCurrentScene();
			}

		}

		if (paused || assetState != AssetState.LOADED)
			return;

		timeOfGame += delta * 1000f;

		getCurrentScene().update(delta);

		uiActors.update(delta);
		getInventory().update(delta);

		textManager.update(delta);
		timers.update(delta);

		transition.update(delta);

		musicManager.update(delta);

		ActionCallbackQueue.run();
	}

	@Override
	public void loadAssets() {
		currentScene.loadAssets();

		if (getInventory().isDisposed())
			getInventory().loadAssets();

		if (uiActors.isDisposed())
			uiActors.loadAssets();

		musicManager.loadAssets();
		textManager.getVoiceManager().loadAssets();
	}

	@Override
	public void retrieveAssets() {
		if (getInventory().isDisposed())
			getInventory().retrieveAssets();

		if (uiActors.isDisposed())
			uiActors.retrieveAssets();

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

		musicManager.retrieveAssets();
		textManager.getVoiceManager().retrieveAssets();
	}

	public Transition getTransition() {
		return transition;
	}

	public long getTimeOfGame() {
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

	public String getCurrentChapter() {
		return currentChapter;
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
			textManager.reset();
			timers.clear();
			currentDialog = null;

			// Stop Sounds
			currentScene.getSoundManager().stop();

			customProperties.put(WorldProperties.PREVIOUS_SCENE.toString(), currentScene.getId());

			if (CACHE_ENABLED)
				cachedScene = currentScene; // CACHE ENABLED
			else
				currentScene.dispose(); // CACHE DISABLED

			transition.reset();
		}

		currentScene = scene;

		musicManager.leaveScene(currentScene.getMusicDesc());
	}

	private void initCurrentScene() {
		cutMode = false;

		// Run INIT action
		if (currentScene.getVerb("init") != null)
			currentScene.runVerb("init");
	}

	public Inventory getInventory() {
		return inventories.get(currentInventory);
	}

	public UIActors getUIActors() {
		return uiActors;
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

	public Map<String, Scene> getScenes() {
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
		}
	}

	public void setInventory(String inventory) {
		Inventory i = inventories.get(inventory);

		if (i == null) {
			i = new Inventory();
			inventories.put(inventory, i);
		}

		currentInventory = inventory;
	}

	public boolean hasDialogOptions() {
		return currentDialog != null || (inkManager != null && inkManager.hasChoices());
	}

	public void selectDialogOption(int i) {
		if (currentDialog != null)
			setCurrentDialog(currentDialog.selectOption(i));
		else if (inkManager != null)
			World.getInstance().getInkManager().selectChoice(i);
	}

	public List<String> getDialogOptions() {
		List<String> choices;

		if (getCurrentDialog() != null) {
			choices = getCurrentDialog().getChoices();
		} else {
			choices = getInkManager().getChoices();
		}

		return choices;
	}

	// tmp vector to use in getInteractiveActorAtInput()
	private final Vector3 unprojectTmp = new Vector3();

	/**
	 * Obtains the actor at (x,y) with TOLERANCE. Search the current scene and
	 * the UIActors list.
	 */
	public InteractiveActor getInteractiveActorAtInput(Viewport v, float tolerance) {

		getSceneCamera().getInputUnProject(v, unprojectTmp);

		InteractiveActor a = currentScene.getInteractiveActorAt(unprojectTmp.x, unprojectTmp.y, tolerance);

		if (a != null)
			return a;

		// search in uiActors
		return uiActors.getActorAtInput(v);
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
		getInventory().setVisible(b);
	}

	public boolean isDisposed() {
		return disposed;
	}

	@Override
	public void dispose() {

		if (disposed)
			return;

		try {

			textManager.reset();
			timers.clear();

			currentDialog = null;

			transition.reset();

			// Clear all pending callbacks
			ActionCallbackQueue.clear();

			// ONLY dispose currentscene because other scenes are already
			// disposed
			if (currentScene != null) {
				musicManager.stopMusic();
				currentScene.dispose();
				currentScene = null;
			}

			if (cachedScene != null) {
				cachedScene.dispose();
				cachedScene = null;
			}

			getInventory().dispose();
			uiActors.dispose();

			spriteBatch.dispose();

			Sprite3DRenderer.disposeBatchs();

			assetState = null;

			musicManager.dispose();

			inkManager = null;

		} catch (Exception e) {
			EngineLogger.error(e.getMessage());
		}

		paused = true;
		disposed = true;
	}

	public SceneCamera getSceneCamera() {
		return currentScene.getCamera();
	}

	public void resize(float viewportWidth, float viewportHeight) {
		if (currentScene != null) {
			currentScene.getCamera().viewportWidth = viewportWidth;
			currentScene.getCamera().viewportHeight = viewportHeight;

			if (currentScene.getCameraFollowActor() != null)
				currentScene.getCamera().updatePos(currentScene.getCameraFollowActor());

			currentScene.getCamera().update();
		}
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

		if (currentScene != null) {

			// do not pause the music when going to the loading screen.
			if (assetState == AssetState.LOADED) {
				musicManager.pauseMusic();
				textManager.getVoiceManager().pause();
			}

			// Pause all sounds
			currentScene.getSoundManager().pause();
		}
	}

	public void resume() {
		paused = false;

		if (assetState == AssetState.LOADED) {
			if (currentScene != null) {
				musicManager.resumeMusic();
				textManager.getVoiceManager().resume();

				// Resume all sounds
				currentScene.getSoundManager().resume();
			}
		}
	}

	public void newGame() throws Exception {
		timeOfGame = 0;
		loadChapter(null);
	}

	public void endGame() {
		dispose();

		// DELETE SAVEGAME
		if (EngineAssetManager.getInstance().getUserFile(GAMESTATE_FILENAME).exists()) {
			EngineAssetManager.getInstance().getUserFile(GAMESTATE_FILENAME).delete();
		}
	}

	// ********** SERIALIZATION **********

	/**
	 * Try to load the save game if exists. In other case, load the game from
	 * XML.
	 * 
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
	 * Load the world description in 'world.json'.
	 * 
	 * @throws IOException
	 */
	public void loadWorldDesc() throws IOException {

		String worldFilename = EngineAssetManager.WORLD_FILENAME;

		if (!EngineAssetManager.getInstance().getModelFile(worldFilename).exists()) {

			// Search the world file with ".json" ext if not found.
			worldFilename = EngineAssetManager.WORLD_FILENAME + ".json";

			if (!EngineAssetManager.getInstance().getModelFile(worldFilename).exists()) {
				EngineLogger.error("ERROR LOADING WORLD: world file not found.");
				dispose();
				throw new IOException("ERROR LOADING WORLD: world file not found.");
			}
		}

		SerializationHelper.getInstance().setMode(Mode.MODEL);

		JsonValue root = new JsonReader()
				.parse(EngineAssetManager.getInstance().getModelFile(worldFilename).reader("UTF-8"));

		Json json = new Json();
		json.setIgnoreUnknownFields(true);

		int width = json.readValue("width", Integer.class, root);
		int height = json.readValue("height", Integer.class, root);

		// We know the world width, so we can set the scale
		EngineAssetManager.getInstance().setScale(width, height);
		float scale = EngineAssetManager.getInstance().getScale();

		setWidth((int) (width * scale));
		setHeight((int) (height * scale));
		setInitChapter(json.readValue("initChapter", String.class, root));
		verbs.read(json, root);
		I18N.loadWorld(EngineAssetManager.MODEL_DIR + EngineAssetManager.WORLD_FILENAME);
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

		long initTime = System.currentTimeMillis();

		SerializationHelper.getInstance().setMode(Mode.MODEL);

		if (chapterName == null)
			chapterName = initChapter;

		currentChapter = chapterName;

		if (EngineAssetManager.getInstance().getModelFile(chapterName + EngineAssetManager.CHAPTER_EXT).exists()) {

			JsonValue root = new JsonReader().parse(EngineAssetManager.getInstance()
					.getModelFile(chapterName + EngineAssetManager.CHAPTER_EXT).reader("UTF-8"));

			Json json = new Json();
			json.setIgnoreUnknownFields(true);

			read(json, root);

			I18N.loadChapter(EngineAssetManager.MODEL_DIR + chapterName);

			customProperties.put(WorldProperties.CURRENT_CHAPTER.toString(), chapterName);
			customProperties.put(WorldProperties.PLATFORM.toString(), Gdx.app.getType().toString());
		} else {
			EngineLogger.error(
					"ERROR LOADING CHAPTER: " + chapterName + EngineAssetManager.CHAPTER_EXT + " doesn't exists.");
			dispose();
			throw new IOException(
					"ERROR LOADING CHAPTER: " + chapterName + EngineAssetManager.CHAPTER_EXT + " doesn't exists.");
		}

		EngineLogger.debug("MODEL LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
	}

	private ObjectWrapper getObjectWrapper() {
		if (wrapper == null)
			wrapper = new ObjectWrapper(this);

		return wrapper;
	}

	public void setModelProp(String prop, String value) {
		getObjectWrapper().setValue(prop, value);
	}

	public Object getModelProp(String prop) {
		return getObjectWrapper().getValue(prop);
	}

	public void loadChapter(String chapter, String scene, boolean test) throws Exception {
		if (test)
			this.testScene = scene;

		loadChapter(chapter);

		if (scene != null) {
			currentScene = null;
			setCurrentScene(scene);
		}
	}

	public void setTestScene(String s) {
		testScene = s;
	}

	public boolean savedGameExists() {
		return savedGameExists(GAMESTATE_FILENAME);
	}

	public boolean savedGameExists(String filename) {
		return EngineAssetManager.getInstance().getUserFile(filename).exists()
				|| FileUtils.exists(EngineAssetManager.getInstance().getAsset("tests/" + filename));
	}

	public void loadGameState() throws IOException {
		long initTime = System.currentTimeMillis();
		loadGameState(GAMESTATE_FILENAME);
		EngineLogger.debug("GAME STATE LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
	}

	public void loadGameState(String filename) throws IOException {
		FileHandle savedFile = null;

		if (EngineAssetManager.getInstance().getUserFile(filename).exists())
			savedFile = EngineAssetManager.getInstance().getUserFile(filename);
		else
			savedFile = EngineAssetManager.getInstance().getAsset("tests/" + filename);

		loadGameState(savedFile);
	}

	public void loadGameState(FileHandle savedFile) throws IOException {
		EngineLogger.debug("LOADING GAME STATE");

		if (!disposed)
			dispose();

		init();

		if (savedFile.exists()) {
			SerializationHelper.getInstance().setMode(Mode.STATE);

			JsonValue root = new JsonReader().parse(savedFile.reader("UTF-8"));

			Json json = new Json();
			json.setIgnoreUnknownFields(true);

			read(json, root);

			assetState = AssetState.LOAD_ASSETS;

		} else {
			throw new IOException("LOADGAMESTATE: no saved game exists");
		}
	}

	public void saveGameState() throws IOException {
		saveGameState(GAMESTATE_FILENAME);
	}

	public void removeGameState(String filename) throws IOException {
		EngineAssetManager.getInstance().getUserFile(filename).delete();
		EngineAssetManager.getInstance().getUserFile(filename + ".png").delete();
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
			w.flush();
		} catch (IOException e) {
			throw new IOException("ERROR SAVING GAME", e);
		} finally {
			w.close();
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
			w.flush();
		} catch (IOException e) {
			throw new IOException("ERROR SAVING MODEL", e);
		} finally {
			w.close();
		}
	}

	public void takeScreenshot(String filename, int w) {

		int h = (int) (w * getSceneCamera().viewportHeight / getSceneCamera().viewportWidth);

		FrameBuffer fbo = new FrameBuffer(Format.RGB565, w, h, false);

		fbo.begin();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
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

		fbo.dispose();
	}

	@Override
	public void write(Json json) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			json.writeValue(Config.BLADE_ENGINE_VERSION_PROP,
					Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, null));

			json.writeValue("sounds", sounds, sounds.getClass(), SoundDesc.class);
			json.writeValue("scenes", scenes, scenes.getClass(), Scene.class);
			json.writeValue("initScene", initScene);

		} else {
			json.writeValue(Config.BLADE_ENGINE_VERSION_PROP,
					Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, null));
			json.writeValue(Config.VERSION_PROP, Config.getProperty(Config.VERSION_PROP, null));
			json.writeValue("scenes", scenes, scenes.getClass(), Scene.class);
			json.writeValue("currentScene", currentScene.getId());
			json.writeValue("inventories", inventories);
			json.writeValue("currentInventory", currentInventory);
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

			if (transition != null)
				json.writeValue("transition", transition);

			json.writeValue("chapter", currentChapter);
			json.writeValue("musicEngine", musicManager);

			if (inkManager != null)
				json.writeValue("inkManager", inkManager);

			if (!uiActors.getActors().isEmpty())
				json.writeValue("uiActors", uiActors);

			ActionCallbackQueue.write(json);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {

		if (SerializationHelper.getInstance().getMode() == Mode.MODEL) {
			String version = json.readValue(Config.BLADE_ENGINE_VERSION_PROP, String.class, jsonData);
			if (version != null && !version.equals(Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""))) {
				EngineLogger.debug("Model Engine Version v" + version + " differs from Current Engine Version v"
						+ Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""));
			}

			sounds = json.readValue("sounds", HashMap.class, SoundDesc.class, jsonData);

			// For backwards compatibility
			if (sounds == null)
				sounds = new HashMap<String, SoundDesc>();

			scenes = json.readValue("scenes", HashMap.class, Scene.class, jsonData);
			initScene = json.readValue("initScene", String.class, jsonData);

			if (initScene == null && scenes.size() > 0) {
				initScene = scenes.keySet().toArray(new String[0])[0];
			}

			for (Scene s : scenes.values()) {
				s.resetCamera(width, height);
			}

			setCurrentScene(initScene);

			// Add sounds to cache
			cacheSounds();
		} else {
			String bladeVersion = json.readValue(Config.BLADE_ENGINE_VERSION_PROP, String.class, jsonData);
			if (bladeVersion != null
					&& !bladeVersion.equals(Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""))) {
				EngineLogger
						.debug("Saved Game Engine Version v" + bladeVersion + " differs from Current Engine Version v"
								+ Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, ""));
			}

			String version = json.readValue(Config.VERSION_PROP, String.class, jsonData);

			if (version == null)
				version = "TEST";

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

			// read inkManager after setting he current scene but before reading
			// scenes and verbs tweens
			if (jsonData.get("inkManager") != null) {
				getInkManager().read(json, jsonData.get("inkManager"));
			}

			// inventories have to be put in the hash to find the actors when
			// reading saved data
			currentInventory = json.readValue("currentInventory", String.class, jsonData);

			JsonValue jsonInventories = jsonData.get("inventories");
			inventories = new HashMap<String, Inventory>();

			for (int i = 0; i < jsonInventories.size; i++) {
				JsonValue jsonValue = jsonInventories.get(i);
				Inventory inv = new Inventory();
				inventories.put(jsonValue.name, inv);
				inv.read(json, jsonValue);
			}

			if (jsonData.get("uiActors") != null) {
				getUIActors().read(json, jsonData.get("uiActors"));
			}

			for (Scene s : scenes.values()) {
				JsonValue jsonValue = jsonData.get("scenes").get(s.getId());

				if (jsonValue != null)
					s.read(json, jsonValue);
				else
					EngineLogger.debug("LOAD WARNING: Scene not found in saved game: " + s.getId());
			}

			timeOfGame = json.readValue("timeOfGame", long.class, 0L, jsonData);
			cutMode = json.readValue("cutmode", boolean.class, false, jsonData);

			verbs.read(json, jsonData);

			timers = json.readValue("timers", Timers.class, jsonData);

			textManager = json.readValue("textmanager", TextManager.class, jsonData);
			customProperties = json.readValue("customProperties", HashMap.class, String.class, jsonData);
			customProperties.put(WorldProperties.SAVED_GAME_VERSION.toString(), version);

			String actorId = json.readValue("dialogActor", String.class, jsonData);
			String dialogId = json.readValue("currentDialog", String.class, jsonData);

			if (dialogId != null) {
				CharacterActor actor = (CharacterActor) getCurrentScene().getActor(actorId, false);
				currentDialog = actor.getDialog(dialogId);
			}

			transition = json.readValue("transition", Transition.class, jsonData);
			musicManager = json.readValue("musicEngine", MusicManager.class, jsonData);

			if (musicManager == null)
				musicManager = new MusicManager();

			ActionCallbackQueue.read(json, jsonData);

			I18N.loadChapter(EngineAssetManager.MODEL_DIR + instance.currentChapter);
		}
	}

	private void cacheSounds() {
		for (Scene s : scenes.values()) {

			HashMap<String, Verb> verbs = s.getVerbManager().getVerbs();

			// Search SoundAction and PlaySoundAction
			for (Verb v : verbs.values()) {
				ArrayList<Action> actions = v.getActions();

				for (Action act : actions) {

					try {
						if (act instanceof SoundAction) {

							String actor = ActionUtils.getStringValue(act, "actor");
							String play = ActionUtils.getStringValue(act, "play");
							if (play != null) {
								SoundDesc sd = World.getInstance().getSounds().get(actor + "_" + play);

								if (sd != null)
									s.getSoundManager().addSoundToLoad(sd);
							}

						} else if (act instanceof PlaySoundAction) {
							String sound = ActionUtils.getStringValue(act, "sound");
							SoundDesc sd = World.getInstance().getSounds().get(sound);

							if (sd != null)
								s.getSoundManager().addSoundToLoad(sd);

						}
					} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
					}
				}
			}

			for (BaseActor a : s.getActors().values()) {

				if (a instanceof InteractiveActor) {
					HashMap<String, Verb> actorVerbs = ((InteractiveActor) a).getVerbManager().getVerbs();

					// Process SayAction of TALK type
					for (Verb v : actorVerbs.values()) {
						ArrayList<Action> actions = v.getActions();

						for (Action act : actions) {

							try {
								if (act instanceof SoundAction) {

									String actor = ActionUtils.getStringValue(act, "actor");
									String play = ActionUtils.getStringValue(act, "play");
									if (play != null) {
										SoundDesc sd = World.getInstance().getSounds().get(actor + "_" + play);

										if (sd != null)
											s.getSoundManager().addSoundToLoad(sd);
									}

								} else if (act instanceof PlaySoundAction) {
									String sound = ActionUtils.getStringValue(act, "sound");
									SoundDesc sd = World.getInstance().getSounds().get(sound);

									if (sd != null)
										s.getSoundManager().addSoundToLoad(sd);

								}
							} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
							}
						}
					}
				}

				if (a instanceof SpriteActor && ((SpriteActor) a).getRenderer() instanceof AnimationRenderer) {
					HashMap<String, AnimationDesc> anims = ((AnimationRenderer) ((SpriteActor) a).getRenderer())
							.getAnimations();

					for (AnimationDesc ad : anims.values()) {
						if (ad.sound != null) {
							String sid = ad.sound;

							SoundDesc sd = World.getInstance().getSounds().get(sid);

							if (sd == null)
								sid = a.getId() + "_" + sid;

							sd = World.getInstance().getSounds().get(sid);

							if (sd != null)
								s.getSoundManager().addSoundToLoad(sd);
							else
								EngineLogger.error(
										a.getId() + ": SOUND not found: " + ad.sound + " in animation: " + ad.id);
						}
					}
				}

			}
		}

	}
}

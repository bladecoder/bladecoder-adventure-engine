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
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.badlogic.gdx.Application.ApplicationType;
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
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.ink.InkManager;
import com.bladecoder.engine.serialization.WorldSerialization;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.FileUtils;

public class World implements AssetConsumer {

	private static final String GAMESTATE_FILENAME = "default" + WorldSerialization.GAMESTATE_EXT;
	private static final String DEFAULT_INVENTORY = "DEFAULT";

	public static enum AssetState {
		LOADED, LOADING, LOADING_AND_INIT_SCENE, LOAD_ASSETS, LOAD_ASSETS_AND_INIT_SCENE
	};

	public static enum WorldProperties {
		SAVED_GAME_VERSION, PREVIOUS_SCENE, CURRENT_CHAPTER, PLATFORM
	};

	private static final boolean CACHE_ENABLED = true;

	// ------------ WORLD PROPERTIES ------------
	private int width;
	private int height;

	private String initScene;
	private final HashMap<String, SoundDesc> sounds = new HashMap<>();
	private final HashMap<String, Scene> scenes = new HashMap<>();
	private final VerbManager verbs = new VerbManager();
	private final I18N i18n = new I18N();

	private Scene currentScene;
	private Dialog currentDialog;

	private final Map<String, Inventory> inventories = new HashMap<>();
	private String currentInventory;

	private UIActors uiActors;

	private boolean paused;
	private boolean cutMode;

	// Keep track of the time of game in ms.
	private long timeOfGame;

	// Add support for the use of global custom properties/variables in the game
	// logic
	private final HashMap<String, String> customProperties = new HashMap<>();

	private String initChapter;
	private String currentChapter;

	// For FADEIN/FADEOUT
	private Transition transition;

	private MusicManager musicManager;

	private WorldListener listener;

	// ------------ LAZY CREATED OBJECTS ------------
	private InkManager inkManager;

	// ------------ TRANSIENT OBJECTS ------------
	private AssetState assetState;
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

	// The verb to call after loading a scene. If null, the "init" verb will be
	// called
	private String initVerb;

	private final WorldSerialization serialization = new WorldSerialization(this);

	public World() {
		init();
	}

	private void init() {

		inventories.clear();
		inventories.put(DEFAULT_INVENTORY, new Inventory());
		setCurrentInventory(DEFAULT_INVENTORY);

		scenes.clear();
		sounds.clear();

		uiActors = new UIActors(this);

		cutMode = false;
		currentChapter = null;
		cachedScene = null;

		customProperties.clear();

		spriteBatch = new SpriteBatch();

		transition = new Transition();

		musicManager = new MusicManager();

		paused = false;

		initGame = true;
	}

	public void setListener(WorldListener l) {
		listener = l;
	}

	public WorldListener getListener() {
		return listener;
	}

	public WorldSerialization getSerializer() {
		return serialization;
	}

	public InkManager getInkManager() {
		// Lazy creation
		if (inkManager == null) {
			// Allow not link the Blade Ink Engine library if you don't use Ink
			try {
				Class.forName("com.bladecoder.ink.runtime.Story");
				inkManager = new InkManager(this);
			} catch (ClassNotFoundException e) {
				EngineLogger.debug("WARNING: Blade Ink Library not found.");
			}
		}

		return inkManager;
	}

	/**
	 * Returns a scene from the cache. null if the scene is not cached.
	 * 
	 * Note that by now, the cache has only one Scene. In the future, the cache will
	 * be a Hastable.
	 */
	public Scene getCachedScene(String id) {

		if (cachedScene != null && cachedScene.getId().equals(id))
			return cachedScene;

		return null;
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

			boolean init = (assetState == AssetState.LOADING_AND_INIT_SCENE);

			assetState = AssetState.LOADED;

			EngineLogger.debug("ASSETS LOADING TIME (ms): " + (System.currentTimeMillis() - initLoadingTime));

			if (initGame) {
				initGame = false;

				// Call world init verbs. Check for SAVED_GAME_VERSION property
				// to know if new or loaded game.
				if (customProperties.get(WorldProperties.SAVED_GAME_VERSION.toString()) == null
						&& verbs.getVerb(Verb.INIT_NEW_GAME_VERB, null, null) != null)
					verbs.runVerb(Verb.INIT_NEW_GAME_VERB, null, null, null);
				else if (customProperties.get(WorldProperties.SAVED_GAME_VERSION.toString()) != null
						&& verbs.getVerb(Verb.INIT_SAVED_GAME_VERB, null, null) != null)
					verbs.runVerb(Verb.INIT_SAVED_GAME_VERB, null, null, null);
			}

			startScene(init);

		}

		if (paused || assetState != AssetState.LOADED)
			return;

		timeOfGame += delta * 1000f;

		getCurrentScene().update(delta);

		uiActors.update(delta);
		getInventory().update(delta);

		transition.update(delta);

		musicManager.update(delta);
	}

	private void startScene(boolean initScene) {
		// call 'init' verb only when arrives from setCurrentScene and not
		// from load or restoring
		if (initScene) {
			currentScene.init();

			if (inkManager != null)
				inkManager.init();

			setCutMode(false);

			// If in test mode run 'test' verb (only the first time)
			if (testScene != null && testScene.equals(currentScene.getId())
					&& currentScene.getVerb(Verb.TEST_VERB) != null) {

				initVerb = Verb.TEST_VERB;

				testScene = null;
			}

			if (initVerb == null)
				initVerb = "init";
		}

		// Run INIT verb
		if (initVerb != null
				&& (currentScene.getVerb(initVerb) != null || getVerbManager().getVerb(initVerb, null, null) != null)) {
			currentScene.runVerb(initVerb);
		}

		initVerb = null;
	}

	@Override
	public void loadAssets() {
		currentScene.loadAssets();

		if (getInventory().isDisposed())
			getInventory().loadAssets();

		if (uiActors.isDisposed())
			uiActors.loadAssets();

		musicManager.loadAssets();
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
	}

	public Transition getTransition() {
		return transition;
	}

	public long getTimeOfGame() {
		return timeOfGame;
	}

	public void setTimeOfGame(long t) {
		timeOfGame = t;
	}

	public AssetState getAssetState() {
		return assetState;
	}

	public I18N getI18N() {
		return i18n;
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

	public void setCurrentScene(Scene scene, boolean init, String initVerb) {

		initLoadingTime = System.currentTimeMillis();

		if (cachedScene == scene || currentScene == scene) {
			if (init)
				assetState = AssetState.LOADING_AND_INIT_SCENE;
			else
				assetState = AssetState.LOADING;
		} else {
			if (cachedScene != null) {
				cachedScene.dispose();
				cachedScene = null;
			}

			if (init)
				assetState = AssetState.LOAD_ASSETS_AND_INIT_SCENE;
			else
				assetState = AssetState.LOAD_ASSETS;
		}

		if (currentScene != null) {
			currentDialog = null;

			// Stop Sounds
			currentScene.getSoundManager().stop();

			customProperties.put(WorldProperties.PREVIOUS_SCENE.toString(), currentScene.getId());

			if (CACHE_ENABLED) {
				if (currentScene != scene) { // Don't cache the scene if it is the same scene.
					cachedScene = currentScene; // CACHE ENABLED
				}
			} else {
				currentScene.dispose(); // CACHE DISABLED
			}

			transition.reset();
		}

		currentScene = scene;
		this.initVerb = initVerb;

		musicManager.leaveScene(currentScene.getMusicDesc());
	}

	public Inventory getInventory() {
		return inventories.get(currentInventory);
	}

	public HashMap<String, String> getCustomProperties() {
		return customProperties;
	}

	public Map<String, Inventory> getInventories() {
		return inventories;
	}

	public UIActors getUIActors() {
		return uiActors;
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

		if (listener != null)
			listener.cutMode(cutMode);
	}

	public void setCurrentScene(String id, boolean init, String initVerb) {
		if (id.equals("$" + WorldProperties.PREVIOUS_SCENE.toString()))
			id = getCustomProperty(WorldProperties.PREVIOUS_SCENE.toString());

		Scene s = scenes.get(id);

		if (s != null) {
			setCurrentScene(s, init, initVerb);
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

		if (listener != null)
			listener.dialogOptions();
	}

	public void setInventory(String inventory) {
		Inventory i = inventories.get(inventory);

		if (i == null) {
			i = new Inventory();
			inventories.put(inventory, i);
		}

		setCurrentInventory(inventory);
	}

	public boolean hasDialogOptions() {
		return currentDialog != null || (inkManager != null && inkManager.hasChoices());
	}

	public void selectDialogOption(int i) {
		if (currentDialog != null)
			setCurrentDialog(currentDialog.selectOption(i));
		else if (inkManager != null)
			getInkManager().selectChoice(i);
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
	 * Obtains the actor at (x,y) with TOLERANCE. Search the current scene and the
	 * UIActors list.
	 */
	public InteractiveActor getInteractiveActorAtInput(Viewport v, float tolerance) {

		getSceneCamera().getInputUnProject(v, unprojectTmp);

		// search first in ui actors
		InteractiveActor a = uiActors.getActorAtInput(v);

		if (a != null)
			return a;

		return currentScene.getInteractiveActorAt(unprojectTmp.x, unprojectTmp.y, tolerance);
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

		if (listener != null)
			listener.inventoryEnabled(b);
	}

	public String getCurrentInventory() {
		return currentInventory;
	}

	public void setCurrentInventory(String currentInventory) {
		this.currentInventory = currentInventory;
	}

	public boolean isDisposed() {
		return currentScene == null;
	}

	@Override
	public void dispose() {

		if (isDisposed())
			return;

		try {

			currentDialog = null;

			transition.reset();

			// ONLY dispose currentscene because other scenes are already
			// disposed
			if (currentScene != null) {
				musicManager.stopMusic();
				currentScene.getTextManager().reset();
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

		init();
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

			uiActors.resize(viewportWidth, viewportHeight);
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
				currentScene.getTextManager().getVoiceManager().pause();
			}

			// Pause all sounds
			currentScene.getSoundManager().pause();
		}

		if (listener != null)
			listener.pause(true);
	}

	public void resume() {
		paused = false;

		if (assetState == AssetState.LOADED) {
			if (currentScene != null) {
				musicManager.resumeMusic();
				currentScene.getTextManager().getVoiceManager().resume();

				// Resume all sounds
				currentScene.getSoundManager().resume();
			}
		}

		if (listener != null)
			listener.pause(false);
	}

	public void newGame() throws Exception {
		timeOfGame = 0;
		serialization.loadChapter();
	}

	public void endGame() {
		dispose();

		// DELETE SAVEGAME
		if (EngineAssetManager.getInstance().getUserFile(GAMESTATE_FILENAME).exists()) {
			EngineAssetManager.getInstance().getUserFile(GAMESTATE_FILENAME).delete();
		}
	}

	// ********** SERIALIZATION **********

	public void saveGameState() throws IOException {
		boolean takeScreenshot = false;

		// Only take screenshot for desktop. For iOs or Android is slow.
		if (Gdx.app.getType() == ApplicationType.Desktop)
			takeScreenshot = true;

		serialization.saveGameState(GAMESTATE_FILENAME, takeScreenshot);
	}

	public void removeGameState(String filename) throws IOException {
		EngineAssetManager.getInstance().getUserFile(filename).delete();
		EngineAssetManager.getInstance().getUserFile(filename + ".png").delete();
	}

	/**
	 * Try to load the saved game if exists. In other case, load the model.
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
				loadGameState();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING SAVED GAME", e);
				// Load the model if fails loading the saved game
				serialization.loadChapter();
			}
		} else {
			serialization.loadChapter();
		}
	}

	public void loadChapter(String chapter, String scene, boolean test) throws Exception {
		if (test)
			this.testScene = scene;

		serialization.loadChapter(chapter, scene, true);
	}

	public void setTestScene(String s) {
		testScene = s;
	}

	/**
	 * Load the world description in 'world.json'.
	 * 
	 * @throws IOException
	 */
	public void loadWorldDesc() throws IOException {
		serialization.loadWorldDesc();
	}

	public void saveWorldDesc(FileHandle file) throws IOException {
		serialization.saveWorldDesc(file);
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

		serialization.loadGameState(savedFile);

		assetState = AssetState.LOAD_ASSETS;
	}

	public void takeScreenshot(String filename, int w) {

		// get viewport
		IntBuffer results = BufferUtils.newIntBuffer(16);
		Gdx.gl20.glGetIntegerv(GL20.GL_VIEWPORT, results);

		int h = (int) (w * getSceneCamera().viewportHeight / getSceneCamera().viewportWidth);

		FrameBuffer fbo = new FrameBuffer(Format.RGB565, w, h, false);

		fbo.begin();
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		draw();

		// TODO: Next line is deprecated, use Pixmap.createFromFrameBuffer();
		Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);

		// restore viewport
		fbo.end(results.get(0), results.get(1), results.get(2), results.get(3));

		PixmapIO.writePNG(EngineAssetManager.getInstance().getUserFile(filename), pixmap, Deflater.DEFAULT_COMPRESSION,
				true);

		fbo.dispose();
	}
}

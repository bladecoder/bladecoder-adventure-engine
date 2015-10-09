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
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.ScreenUtils;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.ActionCallbackQueue;
import com.bladecoder.engine.anim.Timers;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
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

	private HashMap<String, Scene> scenes;
	private String initScene;

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
		scenes = new HashMap<String, Scene>();
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

//			initLoadingTime = System.currentTimeMillis();
			
			
			// Try to load scene for 100ms before. If not loaded in this time, show the loading screen
			float t0 = System.currentTimeMillis();
			float t = 0f;
			while(EngineAssetManager.getInstance().isLoading() && t -t0 < 100f) {
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
		if(EngineLogger.debugMode()) {
			Array<String> assetNames = EngineAssetManager.getInstance().getAssetNames();
			
			assetNames.sort();
			
			EngineLogger.debug("Assets loaded for SCENE: " + currentScene.getId());
			
			for(String n:assetNames) {
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

	/**
	 * Try to load the save game if exists. In other case, load the game from
	 * XML.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void load() {
		if (EngineAssetManager.getInstance().getUserFile(GAMESTATE_FILENAME).exists()) {
			// 2.- SAVEGAME EXISTS
			try {
				instance.loadGameState();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING SAVED GAME", e);
				instance.loadXMLChapter(null);
			}
		} else {
			// 3.- XML LOADING
			instance.loadXMLChapter(null);
		}
	}

	public void loadXMLWorld() {
		try {
			WorldXMLLoader.loadWorld(this);
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING WORLD XML", e);
			instance.dispose();
			Gdx.app.exit();
		}
	}

	public void loadXMLChapter(String chapterName) {
		if (!disposed)
			dispose();

		init();

		assetState = AssetState.LOAD_ASSETS;

		long initTime = System.currentTimeMillis();

		try {
			WorldXMLLoader.loadChapter(chapterName, this);
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING GAME", e);
			instance.dispose();
			Gdx.app.exit();
		}

		EngineLogger.debug("XML LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
	}

	public void loadXMLChapter(String chapter, String scene) {
		this.testScene = scene;

		instance.loadXMLChapter(chapter);

		if (testScene != null) {
			currentScene = null;
			setCurrentScene(testScene);
		}
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
		
		if(cachedScene == scene) {
			assetState = AssetState.LOADING_AND_INIT_SCENE;		
		} else {
			if(cachedScene != null) {
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

			if(CACHE_ENABLED) 
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
			Gdx.app.exit();
		}
	}

	public void setCurrentDialog(Dialog dialog) {
		this.currentDialog = dialog;
		if (dialog != null) {
			dialog.reset();
			
			int visibleOptions = dialog.getNumVisibleOptions();
			
			if(visibleOptions == 0)
				currentDialog = null;
			else if(visibleOptions == 1) {
				selectVisibleDialogOption(0);
			}
		}
	}
	
	public void selectVisibleDialogOption(int i) {
		if(currentDialog == null)
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
			
			if(cachedScene != null) {
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

	public void newGame() {
		loadXMLChapter(null);
	}

	public boolean savedGameExists() {
		return savedGameExists(GAMESTATE_FILENAME);
	}

	public boolean savedGameExists(String filename) {
		return EngineAssetManager.getInstance().getUserFile(filename).exists();
	}

	// ********** JSON SERIALIZATION FOR GAME SAVING **********
	public void saveGameState() {
		saveGameState(GAMESTATE_FILENAME);
	}

	public void loadGameState() {
		long initTime = System.currentTimeMillis();
		loadGameState(GAMESTATE_FILENAME);
		EngineLogger.debug("JSON LOADING TIME (ms): " + (System.currentTimeMillis() - initTime));
	}

	public void loadGameState(String filename) {
		FileHandle savedFile = EngineAssetManager.getInstance().getUserFile(filename);

		loadGameState(savedFile);
	}

	public void loadGameState(FileHandle savedFile) {
		EngineLogger.debug("LOADING GAME STATE");

		if (!disposed)
			dispose();

		init();

		if (savedFile.exists()) {
			assetState = AssetState.LOAD_ASSETS;

			new Json().fromJson(World.class, savedFile.reader("UTF-8"));

		} else {
			EngineLogger.error("LOADGAMESTATE: no saved game exists");
		}
	}

	public void saveGameState(String filename) {
		EngineLogger.debug("SAVING GAME STATE");

		if (disposed)
			return;

		Json json = new Json();
		json.setOutputType(OutputType.javascript);

		String s = null;

		if (EngineLogger.debugMode())
			s = json.prettyPrint(instance);
		else
			s = json.toJson(instance);

		Writer w = EngineAssetManager.getInstance().getUserFile(filename).writer(false, "UTF-8");

		try {
			w.write(s);
			w.close();
		} catch (IOException e) {
			EngineLogger.error("ERROR SAVING GAME", e);
		}

		// Save Screenshot
		takeScreenshot(filename + ".png", SCREENSHOT_DEFAULT_WIDTH);
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
		json.writeValue("scenes", scenes, scenes.getClass(), Scene.class);
		json.writeValue("currentScene", currentScene.getId());
		json.writeValue("inventory", inventory);
		json.writeValue("timeOfGame", timeOfGame);
		json.writeValue("cutmode", cutMode);
		json.writeValue("worldVerbs", VerbManager.worldVerbs, HashMap.class, Verb.class);
		json.writeValue("timers", timers);
		json.writeValue("textmanager", textManager);
		json.writeValue("customProperties", customProperties);

		if (currentDialog == null) {
			json.writeValue("dialogActor", (String) null, null);
			json.writeValue("currentDialog", (String) null, null);
		} else {
			json.writeValue("dialogActor", currentDialog.getActor());
			json.writeValue("currentDialog", currentDialog.getId());
		}

		json.writeValue("transition", transition, transition == null ? null : transition.getClass());

		json.writeValue("chapter", currentChapter);
		ActionCallbackQueue.write(json);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		instance.scenes = json.readValue("scenes", HashMap.class, Scene.class, jsonData);
		instance.currentScene = instance.scenes.get(json.readValue("currentScene", String.class, jsonData));
		instance.inventory = json.readValue("inventory", Inventory.class, jsonData);

		instance.timeOfGame = json.readValue("timeOfGame", Float.class, jsonData);
		instance.cutMode = json.readValue("cutmode", Boolean.class, jsonData);
		VerbManager.worldVerbs = json.readValue("worldVerbs", HashMap.class, Verb.class, jsonData);
		instance.timers = json.readValue("timers", Timers.class, jsonData);

		instance.textManager = json.readValue("textmanager", TextManager.class, jsonData);
		instance.customProperties = json.readValue("customProperties", HashMap.class, String.class, jsonData);

		String actorId = json.readValue("dialogActor", String.class, jsonData);
		String dialogId = json.readValue("currentDialog", String.class, jsonData);

		if (dialogId != null) {
			CharacterActor actor = (CharacterActor)instance.getCurrentScene().getActor(actorId, false);
			instance.currentDialog = actor.getDialog(dialogId);
		}

		transition = json.readValue("transition", Transition.class, jsonData);

		instance.currentChapter = json.readValue("chapter", String.class, jsonData);

		ActionCallbackQueue.read(json, jsonData);

		I18N.loadChapter(EngineAssetManager.MODEL_DIR + instance.currentChapter);

	}
}

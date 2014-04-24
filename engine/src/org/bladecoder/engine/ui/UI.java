package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.AssetConsumer;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.model.World.AssetState;
import org.bladecoder.engine.util.Config;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

public class UI implements CommandListener, TouchEventListener, AssetConsumer {

	private final static String ATLAS_FILENAME = "atlases/ui.atlas";

	private boolean fullscreen = false;
	private Pointer pointer;

	private Screen screen;

	boolean pieMode;

	private SpriteBatch batch;
	private ShapeRenderer renderer;
	private TextureAtlas atlas;

	private ScreenCamera camera;
	
	private Recorder recorder;
	
	private long lastMillis;

	public enum State {
		INIT_SCREEN, SCENE_SCREEN, LOADING_SCREEN, COMMAND_SCREEN, HELP_SCREEN, RESTART_SCREEN, CREDITS_SCREEN
	};

	private State state;

	public UI() {
		batch = new SpriteBatch();
		renderer = new ShapeRenderer();

		camera = new ScreenCamera();
		pointer = new Pointer(camera);
		
		recorder = new Recorder();
		lastMillis = System.currentTimeMillis();

		// EngineInputProcessor inputProcessor = new EngineInputProcessor();
		Gdx.input.setInputProcessor(new EngineInputProcessor(this));
		Gdx.input.setCatchBackKey(true);
		Gdx.input.setCatchMenuKey(true);

		if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
			setPieMode(true);
		else {
			setPieMode(Config.getProperty(Config.PIE_MODE_DESKTOP_PROP, false));
		}

		loadAssets();
		retrieveAssets();

		setState(State.INIT_SCREEN);
	}

	public State getState() {
		return state;
	}
	
	public ScreenCamera getCamera() {
		return camera;
	}

	public void setState(State s) {
		
		if(state == State.INIT_SCREEN) {
			// We must resize always after World.load() to set the viewport
			// to the aspect of the loaded world
			resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
		
		state = s;

		if (state != State.SCENE_SCREEN) {
			World.getInstance().pause();
			pointer.setTarget(null);
		}

		if (screen != null)
			screen.dispose();

		switch (state) {
		case INIT_SCREEN:
			screen = new InitScreen(this, false);
			break;
		case RESTART_SCREEN:
			screen = new InitScreen(this, true);
			break;			
		case COMMAND_SCREEN:
			screen = new CommandScreen(pointer, this);
			break;
		case HELP_SCREEN:
			screen = new HelpScreen(pointer, this, pieMode);
			break;
		case CREDITS_SCREEN:
			screen = new CreditsScreen(pointer, this);
			break;			
		case LOADING_SCREEN:
			screen = new LoadingScreen();
			break;
		case SCENE_SCREEN:
			if (World.getInstance().isDisposed()) {
				try {
					World.getInstance().load();
					resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				} catch (Exception e) {
					EngineLogger.error("ERROR LOADING GAME", e);

					dispose();
					Gdx.app.exit();
				}
			}

			screen = new SceneScreen(pointer, this, pieMode, recorder);
			World.getInstance().resume();
			break;
		}

		screen.createAssets();
		screen.retrieveAssets(atlas);
		screen.resize(camera.getViewport());
	}
	
	public Recorder getRecorder() {
		return recorder;
	}

	public void setPieMode(boolean m) {
		pieMode = m;
		if (m == true)
			pointer.setShowAction(false);
		else
			pointer.setShowAction(true);
	}

	public boolean isPieMode() {
		return pieMode;
	}

	public void update() {
		long currentMillis = System.currentTimeMillis();
		float delta = (currentMillis - lastMillis) / 1000f;
		lastMillis = currentMillis;

		// for long processing frames, limit delta to avoid skipping animations
		// in only one frame
		if (delta > 0.1)
			delta = 0.1f;
		
		if(!World.getInstance().isDisposed()) {
			World.getInstance().update(delta);
		}

		AssetState assetState = World.getInstance().getAssetState();

		if (assetState != AssetState.LOADED && state == State.SCENE_SCREEN) {
			setState(State.LOADING_SCREEN);
		} else if (assetState == AssetState.LOADED
//				&& (state == State.LOADING_SCREEN || state == State.INIT_SCREEN)) {
				&& (state == State.LOADING_SCREEN)) {
			setState(State.SCENE_SCREEN);
		}

		if (state == State.SCENE_SCREEN) {
			((SceneScreen) screen).update();
			recorder.update(delta);
		}
	}

	public void draw() {
		if(state != State.LOADING_SCREEN) {
			Gdx.gl.glClearColor(0, 0, 0, 1);			
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		}

		// WORLD CAMERA
		if (state == State.SCENE_SCREEN) {
			World.getInstance().draw();

			if (EngineLogger.debugMode()
					&& EngineLogger.getDebugLevel() == EngineLogger.DEBUG1) {
				renderer.setProjectionMatrix(World.getInstance().getSceneCamera()
						.combined);
				World.getInstance().getCurrentScene().drawBBoxActors(renderer);
			}
		}

		// SCREEN CAMERA
		batch.setProjectionMatrix(camera.getCamera().combined);
		batch.begin();
		screen.draw(batch);
		batch.end();
	}

	@Override
	public void loadAssets() {
		atlas = new TextureAtlas(EngineAssetManager.getInstance().getResAsset(
				ATLAS_FILENAME));

		pointer.createAssets();

		if (screen != null)
			screen.createAssets();
	}

	@Override
	public void retrieveAssets() {
		pointer.retrieveAssets(atlas);

		if (screen != null)
			screen.retrieveAssets(atlas);
	}
	
	public void restoreGLContext() {
		pointer.createAssets();

		if (screen != null)
			screen.createAssets();
		
		RectangleRenderer.dispose();
		EngineLogger.dispose();
	}

	public void resize(int width, int height) {

		if(!World.getInstance().isDisposed()) {
			camera.setViewport(width, height, World.getInstance().getWidth(), World.getInstance().getHeight());
			World.getInstance().getSceneCamera().update();
		} else {
			camera.setViewport(width, height, width, height);
		}

		pointer.resize((int) camera.getViewport().width,
				(int) camera.getViewport().height);

		if (screen != null)
			screen.resize(camera.getViewport());
	}

	@Override
	public void runCommand(String command, Object param) {

		if (command.equals(CommandListener.CONFIG_COMMAND)) {
			// Exit testMode if config command
			World.getInstance().exitTestMode();
			setState(State.COMMAND_SCREEN);
		} else if (command.equals(CommandScreen.BACK_COMMAND)) {
			setState(State.SCENE_SCREEN);
		} else if (command.equals(CommandScreen.RELOAD_COMMAND)) {
			setState(State.RESTART_SCREEN);
		} else if (command.equals(CommandScreen.CREDITS_COMMAND)) {
			setState(State.CREDITS_SCREEN);
		} else if (command.equals(CommandScreen.HELP_COMMAND)) {
			setState(State.HELP_SCREEN);			
		} else if (command.equals(CommandScreen.QUIT_COMMAND)) {
			exit();
		}
	}

	public void toggleFullScreen() {
		if (!fullscreen) {
			Gdx.graphics.setDisplayMode(Gdx.graphics.getDesktopDisplayMode());
			fullscreen = true;
		} else {
			Gdx.graphics.setDisplayMode(World.getInstance().getWidth(), World
					.getInstance().getHeight(), false);
			fullscreen = false;
		}
	}

	// TODO REMOVE THIS METHOD!!
	public InventoryUI getInventoryUI() {
		return ((SceneScreen) screen).getInventoryUI();
	}

	@Override
	public void dispose() {
		pointer.dispose();
		screen.dispose();
		batch.dispose();
		renderer.dispose();

		atlas.dispose();
	}

	@Override
	public void touchEvent(int type, float x, float y, int pointer, int button) {
		Vector3 unprojectScreen = camera.getInputUnProject();

		screen.touchEvent(type, unprojectScreen.x, unprojectScreen.y, pointer,
				button);
	}

	public void exit() {
		Gdx.app.exit();
	}

}

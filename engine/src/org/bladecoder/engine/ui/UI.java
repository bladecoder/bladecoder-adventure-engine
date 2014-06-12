package org.bladecoder.engine.ui;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.Config;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class UI implements CommandListener {

	private final static String ATLAS_FILENAME = "atlases/ui.atlas";

	private boolean fullscreen = false;
	private Pointer pointer;

	private Screen screen;

	boolean pieMode;

	private SpriteBatch batch;
	private TextureAtlas atlas;

	private ScreenCamera camera;

	public static enum State {
		INIT_SCREEN, SCENE_SCREEN, LOADING_SCREEN, MENU_SCREEN, HELP_SCREEN, RESTART_SCREEN, CREDIT_SCREEN
	};
	
	private final Screen screens[];

	private State state;

	public UI() {
		batch = new SpriteBatch();

		camera = new ScreenCamera();
		pointer = new Pointer(camera);
		
		screens = new Screen[State.values().length];

		Gdx.input.setCatchBackKey(true);
		Gdx.input.setCatchMenuKey(true);

		if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen))
			setPieMode(true);
		else {
			setPieMode(Config.getProperty(Config.PIE_MODE_DESKTOP_PROP, false));
		}
		
		
		screens[State.INIT_SCREEN.ordinal()] = new InitScreen(this, false);
		screens[State.SCENE_SCREEN.ordinal()] = new SceneScreen(this, pieMode);
		screens[State.LOADING_SCREEN.ordinal()] = new LoadingScreen(this);
		screens[State.MENU_SCREEN.ordinal()] = new MenuScreen(this);
		screens[State.HELP_SCREEN.ordinal()] = new HelpScreen(this, pieMode);
		screens[State.RESTART_SCREEN.ordinal()] = new InitScreen(this, true);
		screens[State.CREDIT_SCREEN.ordinal()] =  new CreditsScreen(this);

		loadAssets();

		setScreen(State.INIT_SCREEN);
	}
	
	public Screen getScreen(State state) {
		return screens[state.ordinal()];
	}
	
	public SpriteBatch getBatch() {
		return batch;
	}
	
	public Pointer getPointer() {
		return pointer;
	}

	public State getState() {
		return state;
	}
	
	public ScreenCamera getCamera() {
		return camera;
	}

	public void setScreen(State s) {
		
		state = s;

		if (screen != null) {
			screen.hide();
		}
		
		screen = screens[state.ordinal()];

		screen.show();
			
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	public TextureAtlas getUIAtlas() {
		return atlas;
	}

	private void setPieMode(boolean m) {
		pieMode = m;
		if (m == true)
			pointer.setShowAction(false);
		else
			pointer.setShowAction(true);
	}

	public void render() {
		// for long processing frames, limit delta to 1/30f to avoid skipping animations 
		float delta = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);

		screen.render(delta);
	}

	private void loadAssets() {
		atlas = new TextureAtlas(EngineAssetManager.getInstance().getResAsset(
				ATLAS_FILENAME));
		pointer.createAssets();
		pointer.retrieveAssets(atlas);
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
			screen.resize((int)camera.getViewport().width, (int)camera.getViewport().height);
	}

	@Override
	public void runCommand(String command, Object param) {

		if (command.equals(CommandListener.MENU_COMMAND)) {
			setScreen(State.MENU_SCREEN);
		} else if (command.equals(MenuScreen.BACK_COMMAND)) {
			setScreen(State.SCENE_SCREEN);
		} else if (command.equals(MenuScreen.RELOAD_COMMAND)) {
			setScreen(State.RESTART_SCREEN);
		} else if (command.equals(MenuScreen.CREDITS_COMMAND)) {
			setScreen(State.CREDIT_SCREEN);
		} else if (command.equals(MenuScreen.HELP_COMMAND)) {
			setScreen(State.HELP_SCREEN);			
		} else if (command.equals(MenuScreen.QUIT_COMMAND)) {
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

	public void dispose() {
		pointer.dispose();
		screen.hide();
		batch.dispose();
		atlas.dispose();
	}

	public void exit() {
		Gdx.app.exit();
	}
	
	public void resume() {
		if(Gdx.app.getType() == ApplicationType.Android) {
			// RESTORE GL CONTEXT
			pointer.createAssets();
			RectangleRenderer.dispose();
			EngineLogger.dispose();
		}
		
		if(screen != null)
			screen.resume();
	}
	
	public void pause() {
		if(screen != null)
			screen.pause();
	}
}

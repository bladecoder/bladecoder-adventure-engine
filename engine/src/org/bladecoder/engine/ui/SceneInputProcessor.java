package org.bladecoder.engine.ui;

import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class SceneInputProcessor implements InputProcessor {

	SceneScreen sceneScreen;

	public SceneInputProcessor(SceneScreen sceneScreen) {
		this.sceneScreen = sceneScreen;
	}

	@Override
	public boolean keyDown(int keycode) {
		// EngineLogger.debug("Event Key Down");

		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		switch (keycode) {
		case Input.Keys.ESCAPE:
		case Input.Keys.BACK:
		case Input.Keys.MENU:
			sceneScreen.runCommand(CommandListener.MENU_COMMAND, null);
			break;
		}

		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		switch (character) {

		case 'd':
			EngineLogger.toggle();
			break;
		case '1':
			EngineLogger.setDebugLevel(EngineLogger.DEBUG0);
			break;
		case '2':
			EngineLogger.setDebugLevel(EngineLogger.DEBUG1);
			break;
		case '3':
			EngineLogger.setDebugLevel(EngineLogger.DEBUG2);
			break;
		case 'f':
//			ui.toggleFullScreen();
			break;
		case 's':
			World.getInstance().saveGameState();
			break;
		case 'r':
			try {
				World.restart();
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);

				World.getInstance().dispose();
				Gdx.app.exit();
			}
			break;
		case 'l':
			World.getInstance().loadGameState();
			break;
		case '.':
			if (sceneScreen.getRecorder().isRecording())
				sceneScreen.getRecorder().setRecording(false);
			else
				sceneScreen.getRecorder().setRecording(true);
			break;
		case ',':
			if (sceneScreen.getRecorder().isPlaying())
				sceneScreen.getRecorder().setPlaying(false);
			else {
				sceneScreen.getRecorder().load();
				sceneScreen.getRecorder().setPlaying(true);
			}
			break;
		case 'i': // TODO !!!
			// int inventoryPos =
			// World.getInstance().getUI().getInventoryUI().getInventoryPos();
			// World.getInstance().getUI().getInventoryUI().setInventoryPos(++inventoryPos%4);
			// World.getInstance().getUI().getInventoryUI().resize();
			break;
		case 'p':
			if (World.getInstance().isPaused()) {
				World.getInstance().resume();
			} else {
				World.getInstance().pause();
			}
			break;
		}

		return false;
	}

	@Override
	public boolean touchDown(int x, int y, int pointer, int button) {
		EngineLogger.debug("Event TOUCH DOWN button: " + button);

		sceneScreen.touchEvent(TouchEventListener.TOUCH_DOWN, x, y, pointer, button);

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		EngineLogger.debug("Event TOUCH UP button: " + button);

		sceneScreen.touchEvent(TouchEventListener.TOUCH_UP, x, y, pointer, button);

		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		EngineLogger.debug("EVENT TOUCH DRAGGED");

		sceneScreen.touchEvent(TouchEventListener.DRAG, x, y, pointer, 0);

		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		// EngineLogger.debug("Event SCROLLED");
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		// EngineLogger.debug("Mouse MOVED");
		return false;
	}
}

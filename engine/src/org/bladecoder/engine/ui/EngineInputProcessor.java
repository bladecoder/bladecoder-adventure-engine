package org.bladecoder.engine.ui;

import org.bladecoder.engine.model.World;
import org.bladecoder.engine.ui.UI.State;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class EngineInputProcessor implements InputProcessor {

	UI ui;

	public EngineInputProcessor(UI ui) {
		this.ui = ui;
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
			if (ui.getState() == State.SCENE_SCREEN)
				ui.runCommand(CommandListener.CONFIG_COMMAND, null);
			else if (ui.getState() == State.COMMAND_SCREEN)
				ui.runCommand(CommandScreen.BACK_COMMAND, null);

			break;
		}

		return false;
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
			ui.toggleFullScreen();
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
			if (ui.getRecorder().isRecording())
				ui.getRecorder().setRecording(false);
			else
				ui.getRecorder().setRecording(true);
			break;
		case ',':
			if (ui.getRecorder().isPlaying())
				ui.getRecorder().setPlaying(false);
			else {
				ui.getRecorder().load();
				ui.getRecorder().setPlaying(true);
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

		ui.touchEvent(TouchEventListener.TOUCH_DOWN, x, y, pointer, button);

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		EngineLogger.debug("Event TOUCH UP button: " + button);

		ui.touchEvent(TouchEventListener.TOUCH_UP, x, y, pointer, button);

		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		EngineLogger.debug("EVENT TOUCH DRAGGED");

		ui.touchEvent(TouchEventListener.DRAG, x, y, pointer, 0);

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

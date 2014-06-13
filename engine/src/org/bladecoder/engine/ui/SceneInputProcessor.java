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
package org.bladecoder.engine.ui;

import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

public class SceneInputProcessor implements InputProcessor {
	public static final int TOUCH_DOWN = 0;
	public static final int TOUCH_UP = 1;
	public static final int DRAG = 2;

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
			sceneScreen.showMenu();
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

		sceneScreen.touchEvent(TOUCH_DOWN, x, y, pointer, button);

		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		EngineLogger.debug("Event TOUCH UP button: " + button);

		sceneScreen.touchEvent(TOUCH_UP, x, y, pointer, button);

		return false;
	}

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		EngineLogger.debug("EVENT TOUCH DRAGGED");

		sceneScreen.touchEvent(DRAG, x, y, pointer, 0);

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

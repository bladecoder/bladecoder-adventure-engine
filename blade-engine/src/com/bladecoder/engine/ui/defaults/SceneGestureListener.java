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
package com.bladecoder.engine.ui.defaults;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.ui.UI.InputMode;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.UIUtils;
import com.bladecoder.engine.util.UIUtils.PointerToNextType;

public class SceneGestureListener extends GestureDetector.GestureAdapter implements ControllerListener {
	public static final float THUMBSTICKVELOCITY = 13f;

	public enum ActionButton {
		LOOKAT, ACTION, INVENTORY, NONE
	}

	private DefaultSceneScreen dsc;

	public SceneGestureListener(DefaultSceneScreen dsc) {
		this.dsc = dsc;
	}

	@Override
	public boolean touchDown(float x, float y, int pointer, int button) {
		return true;
	}

	@Override
	public boolean tap(float x, float y, int count, int button) {
		EngineLogger.debug("Event TAP button: " + button + " count: " + count);

		ActionButton actionButton = UIUtils.mouseToAction(button);
		tap(actionButton, count);

		return true;
	}

	public void tap(ActionButton button, int count) {

		World w = dsc.getWorld();

		if (w.getAssetState() != AssetState.LOADED || w.isPaused() || dsc.getUI().getRecorder().isPlaying()
				|| dsc.getUI().getTesterBot().isEnabled())
			return;

		if (dsc.getPie().isVisible()) {
			dsc.getPie().hide();
		}

		if (dsc.getDrawHotspots()) {
			dsc.setDrawHotspots(false);
			return;
		}

		if (w.inCutMode() || (!TextManager.AUTO_HIDE_TEXTS && dsc.getTextManagerUI().isVisible())) {

			if (dsc.getUI().getRecorder().isRecording())
				return;

			w.getCurrentScene().getTextManager().next();
		} else if (dsc.getInventoryUI().isVisible()) {
			dsc.getInventoryUI().hide();
		} else if (!w.hasDialogOptions()) {
			if (button == ActionButton.INVENTORY) {
				// Show inventory with the middle button
				if (!dsc.getInventoryUI().isVisible()) {
					dsc.getInventoryUI().show();
				}
			} else {
				dsc.sceneClick(button, count);
			}
		}
	}

	@Override
	public boolean longPress(float x, float y) {
		EngineLogger.debug("Event LONG PRESS");

		if (dsc.isUiEnabled() && !dsc.getWorld().hasDialogOptions()) {
			dsc.setDrawHotspots(true);
		}

		return false;
	}

	@Override
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		return true;
	}

	@Override
	public boolean panStop(float x, float y, int pointer, int button) {
		tap(x, y, 1, button);

		return true;
	}

	//////////// GAMEPAD /////////////

	@Override
	public void connected(Controller controller) {
	}

	@Override
	public void disconnected(Controller controller) {
	}

	@Override
	public boolean buttonDown(Controller controller, int buttonCode) {
		return false;
	}

	@Override
	public boolean buttonUp(Controller controller, int buttonCode) {
		EngineLogger.debug(buttonCode + " gamepad button down.");

		dsc.getUI().setInputMode(InputMode.GAMEPAD);

		int x = Gdx.input.getX();
		int y = Gdx.input.getY();

		if (dsc.getInventoryUI().isDragging()
				&& (buttonCode == controller.getMapping().buttonA || buttonCode == controller.getMapping().buttonB)) {
			dsc.getInventoryUI().touchedUp(x, y, ActionButton.LOOKAT);
			return false;
		}

//		if ((buttonCode == controller.getMapping().buttonA || buttonCode == controller.getMapping().buttonB)
//				&& Desktop.IsMouseOverGUI) {
//			// Simulate click on UI
//			Desktop.IsTouchDown = true;
//			Desktop.IsTouchDown = false;
//			_clickOnUI = true;
//		} else {
//			_clickOnUI = false;
//		}

		if (buttonCode == controller.getMapping().buttonA) {
			tap(ActionButton.LOOKAT, 1);
		} else if (buttonCode == controller.getMapping().buttonB) {
//			if (dsc.getInventoryUI().isVisible() && dsc.getInventoryUI().IsMouseInside) {
//				dsc.getInventoryUI().startDragging(x, y);
//			} else {
//				tap(ActionButton.ACTION, 1);
//			}
		} else if (buttonCode == controller.getMapping().buttonY) {
			tap(ActionButton.INVENTORY, 1);
		} else if (buttonCode == controller.getMapping().buttonX) {
			if (dsc.getInventoryUI().isVisible()) {
				dsc.getInventoryUI().hide();
			} else if (dsc.getInventoryUI().isDragging()) {
				dsc.getInventoryUI().cancelDragging();
			}
		} else if (buttonCode == controller.getMapping().buttonR1 || buttonCode == controller.getMapping().buttonR2) {
			UIUtils.pointerToActor(dsc.getWorld(), PointerToNextType.RIGHT, dsc.getViewport());
		} else if (buttonCode == controller.getMapping().buttonL1 || buttonCode == controller.getMapping().buttonL2) {
			UIUtils.pointerToActor(dsc.getWorld(), PointerToNextType.LEFT, dsc.getViewport());
		} else if (buttonCode == controller.getMapping().buttonStart) {
			dsc.getUI().setCurrentScreen(Screens.MENU_SCREEN);
		}

		return false;
	}

	@Override
	public boolean axisMoved(Controller controller, int axisCode, float value) {
		// Batch can be disposed if the button action dispose the UI.
		if (dsc.getWorld().isDisposed())
			return false;

		dsc.getUI().setInputMode(InputMode.GAMEPAD);

		int x = Gdx.input.getX();
		int y = Gdx.input.getY();

		// Check the direction in X axis of left analog stick
		float v = value * THUMBSTICKVELOCITY * dsc.getViewport().getScreenWidth() / 1080f;

		if (axisCode == controller.getMapping().axisLeftX) {
			x += v;
		} else if (axisCode == controller.getMapping().axisLeftY) {
			y -= v;
		} else if (axisCode == controller.getMapping().axisRightX) {
			x += v / 2f;
		} else if (axisCode == controller.getMapping().axisRightY) {
			y -= v / 2f;
		}

		Gdx.input.setCursorPosition(MathUtils.clamp(x, 0, dsc.getViewport().getScreenWidth()),
				MathUtils.clamp(y, 0, dsc.getViewport().getScreenHeight()));

		return false;
	}
}

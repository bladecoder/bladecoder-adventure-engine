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
import com.badlogic.gdx.Input.Peripheral;
import com.badlogic.gdx.input.GestureDetector;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.ui.SceneScreen.ActionButton;
import com.bladecoder.engine.ui.UI.InputMode;
import com.bladecoder.engine.util.EngineLogger;

public class SceneGestureListener extends GestureDetector.GestureAdapter {
	private final DefaultSceneScreen dsc;

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

		if (Gdx.input.isPeripheralAvailable(Peripheral.MultitouchScreen)) {
			dsc.getUI().setInputMode(InputMode.TOUCHPANEL);
		} else {
			dsc.getUI().setInputMode(InputMode.MOUSE);
		}

		ActionButton actionButton = mouseToAction(button);
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

	public static ActionButton mouseToAction(int b) {
		if (b == 0) {
			return ActionButton.LOOKAT;
		}

		if (b == 1) {
			return ActionButton.ACTION;
		}

		if (b == 2) {
			return ActionButton.INVENTORY;
		}

		return ActionButton.NONE;
	}
}

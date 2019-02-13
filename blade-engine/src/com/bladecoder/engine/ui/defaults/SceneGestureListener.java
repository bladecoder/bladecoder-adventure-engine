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

import com.badlogic.gdx.input.GestureDetector;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.util.EngineLogger;

public class SceneGestureListener extends GestureDetector.GestureAdapter {

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

		World w = dsc.getUI().getWorld();

		if (w.getAssetState() != AssetState.LOADED || w.isPaused() || dsc.getUI().getRecorder().isPlaying()
				|| dsc.getUI().getTesterBot().isEnabled())
			return true;

		if (dsc.getPie().isVisible()) {
			dsc.getPie().hide();
		}

		if (dsc.getDrawHotspots())
			dsc.setDrawHotspots(false);
		else {
			if (w.inCutMode() || (!TextManager.AUTO_HIDE_TEXTS && dsc.getTextManagerUI().isVisible())) {

				if (dsc.getUI().getRecorder().isRecording())
					return true;

				w.getCurrentScene().getTextManager().next();
			} else if (dsc.getInventoryUI().isVisible()) {
				dsc.getInventoryUI().hide();
			} else if (!w.hasDialogOptions()) {
				if (button == 2) {
					// Show inventory with the middle button
					if (!dsc.getInventoryUI().isVisible()) {
						dsc.getInventoryUI().show();
					}
				} else {
					dsc.sceneClick(button, count);
				}
			}
		}

		return true;
	}

	@Override
	public boolean longPress(float x, float y) {
		EngineLogger.debug("Event LONG PRESS");

		if (dsc.isUiEnabled() && !dsc.getUI().getWorld().hasDialogOptions()) {
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
}

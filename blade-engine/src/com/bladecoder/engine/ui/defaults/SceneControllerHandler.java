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

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.UIUtils;
import com.bladecoder.engine.util.UIUtils.ActionButton;
import com.bladecoder.engine.util.UIUtils.PointerToNextType;

public class SceneControllerHandler extends ScreenControllerHandler {
	public static final float THUMBSTICKVELOCITY = 13f * 60f;

	private DefaultSceneScreen dsc;

	public SceneControllerHandler(DefaultSceneScreen dsc) {
		super(dsc.getUI(), dsc.getStage(), dsc.getViewport());
		this.dsc = dsc;
	}

	@Override
	protected boolean buttonUp(Controller controller, int buttonCode) {

		int x = Gdx.input.getX();
		int y = Gdx.input.getY();

		if (dsc.getInventoryUI().isDragging()
				&& (buttonCode == controller.getMapping().buttonA || buttonCode == controller.getMapping().buttonB)) {
			dsc.getInventoryUI().touchedUp(x, y, ActionButton.LOOKAT);
			return true;
		}

		if (super.buttonUp(controller, buttonCode)) {
			EngineLogger.debug("> Controller button handled by Stage.");
			return true;
		}

		if (buttonCode == controller.getMapping().buttonA) {
			dsc.tap(ActionButton.LOOKAT, 1);
		} else if (buttonCode == controller.getMapping().buttonB) {
			dsc.tap(ActionButton.ACTION, 1);
		} else if (buttonCode == controller.getMapping().buttonY) {
			dsc.tap(ActionButton.INVENTORY, 1);
		} else if (buttonCode == controller.getMapping().buttonX) {
			if (dsc.getInventoryUI().isVisible()) {
				dsc.getInventoryUI().hide();
			} else if (dsc.getInventoryUI().isDragging()) {
				dsc.getInventoryUI().cancelDragging();
			}
		}

		return false;
	}

	@Override
	protected void focusNext(PointerToNextType type) {
		if (type == PointerToNextType.RIGHT) {
			pointerToActor(dsc.getWorld(), PointerToNextType.RIGHT, dsc.getViewport());
		} else {
			pointerToActor(dsc.getWorld(), PointerToNextType.LEFT, dsc.getViewport());
		}
	}

	private void pointerToActor(World w, PointerToNextType type, Viewport viewport) {

		List<Vector2> positions = new ArrayList<>();

		Vector3 unprojectV = new Vector3();
		float scale = EngineAssetManager.getInstance().getScale();

		InteractiveActor actorUnderCursor = w.getInteractiveActorAtInput(viewport, 0f);

		for (InteractiveActor a : w.getUIActors().getActors()) {
			if (!a.canInteract() || actorUnderCursor == a)
				continue;

			Vector2 pos = new Vector2();
			a.getBBox().getBoundingRectangle().getCenter(pos);

			if (w.getUIActors().getActorAt(pos.x, pos.y) == a) {
				unprojectV.set(pos.x * scale, pos.y * scale, 0);
				w.getUIActors().getCamera().project(unprojectV, 0, 0, viewport.getScreenWidth(),
						viewport.getScreenHeight());
				positions.add(pos.set(unprojectV.x, viewport.getScreenHeight() - unprojectV.y));
			}
		}

		for (BaseActor a : w.getCurrentScene().getActors().values()) {
			if (!(a instanceof InteractiveActor) || !((InteractiveActor) a).canInteract() || actorUnderCursor == a)
				continue;

			Vector2 pos = new Vector2();
			a.getBBox().getBoundingRectangle().getCenter(pos);

			if (w.getUIActors().getActorAt(pos.x, pos.y) == null
					&& w.getCurrentScene().getInteractiveActorAt(pos.x, pos.y) == a) {
				unprojectV.set(pos.x * scale, pos.y * scale, 0);
				w.getCurrentScene().getCamera().project(unprojectV, 0, 0, viewport.getScreenWidth(),
						viewport.getScreenHeight());
				positions.add(pos.set(unprojectV.x, viewport.getScreenHeight() - unprojectV.y));
			}
		}

		UIUtils.setNextCursorPosition(positions, type);
	}
}

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
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.ui.UI.InputMode;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.EngineLogger;

public class ScreenControllerHandler {
	public static final float THUMBSTICKVELOCITY = 12f * 60f;

	private Stage stage;
	private UI ui;
	private Viewport viewport;

	private int pressed = -1;

	public ScreenControllerHandler(UI ui, Stage stage, Viewport viewport) {
		this.stage = stage;
		this.ui = ui;
		this.viewport = viewport;
	}

	public void update(float delta) {
		updateAxis(delta);
		updateDPad(delta);
		updateButtons();
	}

	private void updateButtons() {

		for (Controller controller : Controllers.getControllers()) {

			for (int buttonCode = controller.getMinButtonIndex(); buttonCode <= controller
					.getMaxButtonIndex(); buttonCode++) {
				boolean p = controller.getButton(buttonCode);

				if (p) {
					if (pressed != -1 && buttonCode != pressed) {
						buttonUp(controller, pressed);
					}

					pressed = buttonCode;
				} else if (buttonCode == pressed) {
					buttonUp(controller, pressed);
					pressed = -1;
				}
			}
		}
	}

	protected boolean buttonUp(Controller controller, int buttonCode) {
		EngineLogger.debug(buttonCode + " gamepad button up.");

		ui.setInputMode(InputMode.GAMEPAD);

		if (buttonCode == controller.getMapping().buttonStart) {
			if (ui.getCurrentScreen() != ui.getScreen(Screens.MENU_SCREEN)) {
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			}

			return true;
		}

		if (buttonCode == controller.getMapping().buttonA || buttonCode == controller.getMapping().buttonB) {
			// Simulate click on UI
			int x = Gdx.input.getX();
			int y = Gdx.input.getY();

			int pointer = buttonCode == controller.getMapping().buttonA ? 11 : 12;

			stage.touchDown(x, y, pointer, 0);
			boolean handled = stage.touchUp(x, y, pointer, 0);

			if (handled)
				return true;
		} else if (buttonCode == controller.getMapping().buttonR1 || buttonCode == controller.getMapping().buttonR2) {
			focusNext(PointerToNextType.RIGHT);
			return true;
		} else if (buttonCode == controller.getMapping().buttonL1 || buttonCode == controller.getMapping().buttonL2) {
			focusNext(PointerToNextType.LEFT);
			return true;
		}

		return false;
	}

	protected void focusNext(PointerToNextType type) {
		List<Vector2> positions = new ArrayList<>();

		Array<Actor> actors = stage.getActors();

		Button hit = getButtonUnderCursor(stage);

		if (hit != null)
			hit.getClickListener().exit(null, 0, 0, -1, null);

		addActors(actors, positions, hit);

		setNextCursorPosition(positions, type);

		hit = getButtonUnderCursor(stage);

		if (hit != null) {
			hit.getClickListener().enter(null, 0, 0, -1, null);
		}
	}

	protected Button getButtonUnderCursor(Stage stage) {
		Vector2 inputPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
		stage.screenToStageCoordinates(inputPos);
		Actor hit = stage.hit(inputPos.x, inputPos.y, true);

		if (hit != null) {
			while (!(hit instanceof Button) && hit != null)
				hit = hit.getParent();

			EngineLogger.debug("HIT!!: " + hit);
		}

		return (Button) hit;
	}

	protected void addActors(Array<Actor> actors, List<Vector2> positions, Actor hit) {
		for (Actor a : actors) {

			if (a == hit)
				continue;

			if (!a.isVisible())
				continue;

			if (a instanceof Button) {

				Vector2 pos = new Vector2();
				a.localToScreenCoordinates(pos);

				pos.x = pos.x + a.getWidth() / 2f;
				pos.y = pos.y - a.getHeight() / 2f;

				positions.add(pos);
				EngineLogger.debug("ADD: " + a + " pos: " + pos);
			} else if (a instanceof Group) {
				addActors(((Group) a).getChildren(), positions, hit);
			}
		}
	}

	public static void cursorToActor(Actor target) {
		Vector2 pos = new Vector2(target.getWidth() / 2f, target.getHeight() / 2f);
		target.localToScreenCoordinates(pos);

		Gdx.input.setCursorPosition((int) pos.x, (int) pos.y);

		target.getStage().mouseMoved(Gdx.input.getX(), Gdx.input.getY());
	}

	protected void setNextCursorPosition(List<Vector2> positions, PointerToNextType type) {
		if (positions.isEmpty())
			return;

		if (type == PointerToNextType.RIGHT) {
			positions.sort(new Comparator<Vector2>() {
				@Override
				public int compare(Vector2 o1, Vector2 o2) {
					int val = (int) (o1.x - o2.x);

					if (val == 0)
						val = (int) (o1.y - o2.y);

					return val;
				}
			});
		} else {
			positions.sort(new Comparator<Vector2>() {
				@Override
				public int compare(Vector2 o1, Vector2 o2) {
					int val = (int) (o2.x - o1.x);

					if (val == 0)
						val = (int) (o2.y - o1.y);

					return val;
				}
			});
		}

		int idx = 0;

		float minD = Float.MAX_VALUE;
		Vector2 mPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());

		// get the nearest actor
		for (int i = 0; i < positions.size(); i++) {
			Vector2 actPos = positions.get(i);
			float d = actPos.dst(mPos);
			if (d < minD) {
				minD = d;
				idx = i;
			}
		}

		EngineLogger.debug("Prev: " + positions.get(idx) + " IDX: " + idx + " mPos: " + mPos);

		if ((type == PointerToNextType.RIGHT && (int) positions.get(idx).x < (int) mPos.x)
				|| (type == PointerToNextType.LEFT && (int) positions.get(idx).x > (int) mPos.x)
				|| (type == PointerToNextType.RIGHT && (int) positions.get(idx).x == (int) mPos.x
						&& positions.get(idx).y < mPos.y)
				|| (type == PointerToNextType.LEFT && (int) positions.get(idx).x == (int) mPos.x
						&& positions.get(idx).y > mPos.y))
			idx = (idx + 1) % positions.size();

		EngineLogger.debug("Selected: " + positions.get(idx) + " IDX: " + idx);
		Gdx.input.setCursorPosition((int) positions.get(idx).x, (int) positions.get(idx).y);
	}

	private void updateAxis(float delta) {
		float v = THUMBSTICKVELOCITY * delta * viewport.getScreenWidth() / 1080f;

		int vx = 0, vy = 0;

		for (Controller controller : Controllers.getControllers()) {

			for (int axisCode = 0; axisCode < controller.getAxisCount(); axisCode++) {

				float inc = controller.getAxis(axisCode) * v;

				if (axisCode == controller.getMapping().axisLeftX) {
					vx += inc;
				} else if (axisCode == controller.getMapping().axisLeftY) {
					vy += inc;
				} else if (axisCode == controller.getMapping().axisRightX) {
					vx += inc / 2f;
				} else if (axisCode == controller.getMapping().axisRightY) {
					vy += inc / 2f;
				}
			}
		}

		if (vx != 0 || vy != 0) {
			int x = Gdx.input.getX() + vx;
			int y = Gdx.input.getY() + vy;

			ui.setInputMode(InputMode.GAMEPAD);

			Gdx.input.setCursorPosition(
					MathUtils.clamp(x, viewport.getScreenX(), viewport.getScreenWidth() + viewport.getScreenX()),
					MathUtils.clamp(y, viewport.getScreenY(), viewport.getScreenHeight() + viewport.getScreenY()));

			if (stage != null)
				stage.mouseMoved(Gdx.input.getX(), Gdx.input.getY());
		}
	}

	private void updateDPad(float delta) {
		float v = THUMBSTICKVELOCITY * delta * viewport.getScreenWidth() / 1080f;

		int vx = 0, vy = 0;

		for (Controller controller : Controllers.getControllers()) {

			if (controller.getButton(controller.getMapping().buttonDpadRight)) {
				vx += v;
			} else if (controller.getButton(controller.getMapping().buttonDpadLeft)) {
				vx -= v;
			}

			if (controller.getButton(controller.getMapping().buttonDpadUp)) {
				vy -= v;
			} else if (controller.getButton(controller.getMapping().buttonDpadDown)) {
				vy += v;
			}
		}

		if (vx != 0 || vy != 0) {
			int x = Gdx.input.getX() + vx;
			int y = Gdx.input.getY() + vy;

			ui.setInputMode(InputMode.GAMEPAD);

			Gdx.input.setCursorPosition(
					MathUtils.clamp(x, viewport.getScreenX(), viewport.getScreenWidth() + viewport.getScreenX()),
					MathUtils.clamp(y, viewport.getScreenY(), viewport.getScreenHeight() + viewport.getScreenY()));

			if (stage != null)
				stage.mouseMoved(Gdx.input.getX(), Gdx.input.getY());
		}
	}

	public enum PointerToNextType {
		LEFT, RIGHT
	}
}

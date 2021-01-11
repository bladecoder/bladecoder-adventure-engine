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
package com.bladecoder.engine.util;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.defaults.SceneGestureListener.ActionButton;

public class UIUtils {
	private final static Vector3 unprojectTmp = new Vector3();
	private final static Vector2 unproject2Tmp = new Vector2();

	public static void showUIText(Stage stage, Skin skin, World w, Text t) {
		// Type UI texts will show at the same time that TextManagerUI texts.

		String style = t.style == null ? "ui-text" : t.style;
		Label msg = new Label(t.str, skin, style);

		msg.setWrap(true);
		msg.setAlignment(Align.center, Align.center);

		if (t.color != null)
			msg.setColor(t.color);

		msg.setSize(msg.getWidth() + DPIUtils.getMarginSize() * 2, msg.getHeight() + DPIUtils.getMarginSize() * 2);

		stage.addActor(msg);
		unprojectTmp.set(t.x, t.y, 0);
		w.getSceneCamera().scene2screen(stage.getViewport(), unprojectTmp);

		float posx, posy;

		if (t.x == TextManager.POS_CENTER) {
			posx = (stage.getViewport().getScreenWidth() - msg.getWidth()) / 2;
		} else if (t.x == TextManager.POS_SUBTITLE) {
			posx = DPIUtils.getMarginSize();
		} else {
			posx = unprojectTmp.x;
		}

		if (t.y == TextManager.POS_CENTER) {
			posy = (stage.getViewport().getScreenHeight() - msg.getHeight()) / 2;
		} else if (t.y == TextManager.POS_SUBTITLE) {
			posy = stage.getViewport().getScreenHeight() - msg.getHeight() - DPIUtils.getMarginSize() * 3;
		} else {
			posy = unprojectTmp.y;
		}

		msg.setPosition(posx, posy);
		msg.getColor().a = 0;
		msg.addAction(sequence(Actions.fadeIn(0.4f, Interpolation.fade),
				Actions.delay(t.time, sequence(fadeOut(0.4f, Interpolation.fade), Actions.removeActor()))));
	}

	/**
	 * Calcs the rotation based in the actor screen position
	 */
	public static float calcLeaveArrowRotation(Viewport viewport, InteractiveActor actor) {
		Verb verb = actor.getVerb(Verb.LEAVE_VERB);

		if (verb == null || verb.getIcon() == null) {

			actor.getBBox().getBoundingRectangle().getCenter(unproject2Tmp);

			if (unproject2Tmp.x < viewport.getWorldWidth() / 3f) {
				return 180; // LEFT
			}

			if (unproject2Tmp.x > viewport.getWorldWidth() / 3f * 2f) {
				return 0; // RIGHT
			}

			if (unproject2Tmp.y < viewport.getWorldHeight() / 5f) {
				return -90; // DOWN
			}

			return 90; // UP
		} else {
			String dir = verb.getIcon();

			if (dir.equals("left")) {
				return 180; // LEFT
			}

			if (dir.equals("right")) {
				return 0; // RIGHT
			}

			if (dir.equals("down")) {
				return -90; // DOWN
			}

			return 90; // UP
		}
	}

	public static void pointerToActor(World w, PointerToNextType type, Viewport viewport) {

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

		idx = (idx + 1) % positions.size();

		EngineLogger.debug("Next: " + positions.get(idx));
		Gdx.input.setCursorPosition((int) positions.get(idx).x, (int) positions.get(idx).y);
	}

	public enum PointerToNextType {
		LEFT, RIGHT
	}

	public static final ActionButton mouseToAction(int b) {
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

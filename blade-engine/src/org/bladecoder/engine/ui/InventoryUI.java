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

import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Inventory;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.SpriteRenderer;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.Config;
import org.bladecoder.engine.util.DPIUtils;
import org.bladecoder.engine.util.RectangleRenderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

public class InventoryUI extends com.badlogic.gdx.scenes.scene2d.Actor {
	private final static int TOP = 0;
	private final static int DOWN = 1;
	private final static int LEFT = 2;
	private final static int RIGHT = 3;

	private final static String MENU_BUTTON = "config";

	// private final static Color GRAY = new Color(0.3f, 0.3f, 0.3f, 1f);
	private final static Color BG_COLOR = new Color(0, 0, 0, 0.8f);
	// private final static Color BG_COLOR = new Color(0, 0, 0, 1f);

	private Rectangle configBbox = new Rectangle();

	private int tileSize;
	private int margin;
	private int inventoryPos = DOWN;

	private SpriteActor draggedActor = null;

	private AtlasRegion configIcon;

	private final SceneScreen sceneScreen;

	public InventoryUI(SceneScreen scr) {
		this.sceneScreen = scr;

		String pos = Config.getProperty(Config.INVENTORY_POS_PROP, "down");

		if (pos.trim().equals("top"))
			inventoryPos = TOP;
		else if (pos.trim().equals("left"))
			inventoryPos = LEFT;
		else if (pos.trim().equals("right"))
			inventoryPos = RIGHT;
		else
			inventoryPos = DOWN;

		addListener(new InputListener() {
			@Override
			public void touchUp(InputEvent event, float x, float y,
					int pointer, int button) {

				if (draggedActor != null) {
					stopDragging((int) x, (int) y);
				} else if (configBbox.contains(x, y)) {
					sceneScreen.showMenu();
				} else {
					Actor actor = getItemAt(x, y);

					if (actor != null) {
						sceneScreen.actorClick(actor, button == 1);
					} else {
						hide();
					}
				}
			}

			@Override
			public boolean mouseMoved(InputEvent event, float x, float y) {

				return true;
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y,
					int pointer) {
				if (draggedActor == null)
					startDragging(x, y);
			}

			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				return true;
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer,
					com.badlogic.gdx.scenes.scene2d.Actor toActor) {

				// EngineLogger.debug("EXIT EVENT: " + toActor);
				if (!(toActor instanceof PieMenu)
						&& !(toActor instanceof InventoryUI)
						&& draggedActor != null)
					hide();
			}
		});
	}

	public void show() {
		if (!isVisible()) {
			setVisible(true);

			addAction(Actions
					.moveTo(getX(), getY() + getHeight() + margin, .1f));
		}
	}

	public void hide() {
		if(isVisible())
			addAction(Actions.sequence(Actions.moveTo(getX(), -getHeight(), .1f),
				Actions.hide()));
	}

	public void resize(int width, int height) {
		tileSize = DPIUtils.getButtonPrefSize() * 2;

		int w = (int) (width * .8f / tileSize) * tileSize;
		int h = (int) (height * .8f / tileSize) * tileSize;
		margin = (height - h) / 2;

		setVisible(false);

		setBounds((width - w) / 2, -h, w, h);
	}

	public void retrieveAssets(TextureAtlas atlas) {
		configIcon = atlas.findRegion(MENU_BUTTON);
	}

	@Override
	public void draw(Batch batch, float alpha) {
		Inventory inventory = World.getInstance().getInventory();

		if (!inventory.isVisible()) {
			setVisible(false);
			return;
		}

		RectangleRenderer.draw(batch, getX(), getY(), getWidth(), getHeight(),
				BG_COLOR);

		batch.draw(configIcon, configBbox.x, configBbox.y, configBbox.width,
				configBbox.height);

		int cols = (int) getWidth() / tileSize;
		int rows = (int) getHeight() / tileSize - 1;

		// DRAW ITEMS
		for (int i = 0; i < inventory.getNumItems(); i++) {

			SpriteActor a = inventory.getItem(i);
			SpriteRenderer r = a.getRenderer();

			float size = tileSize
					/ (r.getHeight() > r.getWidth() ? r.getHeight() : r
							.getWidth());

			float x = i % cols;
			float y = rows - i / cols;

			r.draw((SpriteBatch) batch, getX() + x * tileSize + tileSize / 2,
					getY() + y * tileSize, size);
		}
	}

	public void cancelDragging() {
		draggedActor = null;
		sceneScreen.getUI().getPointer().drag(null);
	}

	private void startDragging(float x, float y) {
		draggedActor = getItemAt(x, y);
		if (draggedActor != null)
			sceneScreen.getUI().getPointer().drag(draggedActor.getRenderer());
	}

	private final Vector3 mousepos = new Vector3();

	private void stopDragging(int inputX, int inputY) {
		World.getInstance().getSceneCamera()
				.getInputUnProject(sceneScreen.getViewport(), mousepos);

		Actor targetActor = sceneScreen.getCurrentActor();

		if (targetActor != null) {
			use(targetActor, draggedActor);
		}

		draggedActor = null;
		sceneScreen.getUI().getPointer().drag(null);
	}

	private void use(Actor a1, Actor a2) {
		if (a1.getVerb("use", a2.getId()) != null) {
			sceneScreen.runVerb(a1, "use", a2.getId());
		} else {
			sceneScreen.runVerb(a2, "use", a1.getId());
		}
	}

	public SpriteActor getItemAt(float x, float y) {

		Inventory inventory = World.getInstance().getInventory();

		int cols = (int) getWidth() / tileSize;
		int rows = (int) getHeight() / tileSize - 1;

		int i = (rows - ((int) y / tileSize)) * cols + (int) x / tileSize;

		if (i >= 0 && i < inventory.getNumItems()) {
			// EngineLogger.debug(" X: " + x + " Y:" + y + " DESC:" +
			// inventory.getItem(i).getDesc());
			return inventory.getItem(i);
		}

		return null;
	}

	public int getInventoryPos() {
		return inventoryPos;
	}
}

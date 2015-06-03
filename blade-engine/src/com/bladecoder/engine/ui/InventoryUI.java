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
package com.bladecoder.engine.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engine.model.ActorRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class InventoryUI extends com.badlogic.gdx.scenes.scene2d.Group {
	public final static int TOP = 0;
	public final static int DOWN = 1;
	public final static int LEFT = 2;
	public final static int RIGHT = 3;
	public final static int CENTER = 4;

	private final Rectangle configBbox = new Rectangle();

	private int tileSize;
	private int margin;
	private float rowSpace;
	private int cols, rows;
	private int inventoryPos = CENTER;
	private boolean autosize = true;

	private SpriteActor draggedActor = null;

	private final SceneScreen sceneScreen;

	private InventoryUIStyle style;

	private CustomImageButton menuButton;

	private final Vector2 orgPos = new Vector2();
	private final Vector2 targetPos = new Vector2();

	public InventoryUI(SceneScreen scr) {
		style = scr.getUI().getSkin().get(InventoryUIStyle.class);
		sceneScreen = scr;

		String pos = Config.getProperty(Config.INVENTORY_POS_PROP, "down");

		autosize = Config.getProperty(Config.INVENTORY_AUTOSIZE_PROP, true);

		if (pos.trim().equals("top"))
			inventoryPos = TOP;
		else if (pos.trim().equals("left"))
			inventoryPos = LEFT;
		else if (pos.trim().equals("right"))
			inventoryPos = RIGHT;
		else if (pos.trim().equals("down"))
			inventoryPos = DOWN;
		else
			inventoryPos = CENTER;

		addListener(new InputListener() {
			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {

				if (draggedActor != null) {
					stopDragging(button);
				} else if (configBbox.contains(x, y)) {
					sceneScreen.showMenu();
				} else {
					BaseActor actor = getItemAt(x, y);

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
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if (draggedActor == null)
					startDragging(x, y);
			}

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				return true;
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer,
					com.badlogic.gdx.scenes.scene2d.Actor toActor) {

				// EngineLogger.debug("EXIT EVENT: " + toActor);
				if (!(toActor instanceof PieMenu) && !(toActor instanceof InventoryUI) && draggedActor != null)
					hide();
			}
		});

		menuButton = new CustomImageButton(style.menuButtonStyle);

		addActor(menuButton);
		menuButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
				sceneScreen.getUI().setCurrentScreen(UI.Screens.MENU_SCREEN);
			}
		});
	}

	public void show() {
		if (!isVisible()) {
			resize((int) getStage().getCamera().viewportWidth, (int) getStage().getCamera().viewportHeight);
			setVisible(true);
			setPosition(orgPos.x, orgPos.y);

			addAction(Actions.moveTo(targetPos.x, targetPos.y, .1f));
		}
	}

	public void hide() {
		if (isVisible())
			addAction(Actions.sequence(Actions.moveTo(orgPos.x, orgPos.y, .1f), Actions.hide()));
	}

	public void resize(int width, int height) {

		Inventory inventory = World.getInstance().getInventory();

		tileSize = (int) DPIUtils.getTouchMinSize() * 2;
		margin = (int) DPIUtils.getMarginSize();

		rowSpace = DPIUtils.getSpacing();

		int w = (int) (width * .8f / tileSize) * tileSize;
		int h = (int) (height * .7f / tileSize) * tileSize;

		if (autosize) {
			if (inventoryPos == LEFT || inventoryPos == RIGHT) {
				int w2 = tileSize * (inventory.getNumItems() / (h / tileSize) + 1);

				if (w2 < w)
					w = w2;
			} else {

				int h2 = tileSize * (inventory.getNumItems() / (w / tileSize) + 1);

				if (h2 < h)
					h = h2;
			}
		}

		cols = w / tileSize;
		rows = h / tileSize;

		setVisible(false);
		setSize(w + (cols - 1) * rowSpace + margin * 2, h + (rows - 1) * rowSpace + margin * 2);

		int capacity = cols * rows;
		if (inventory.getNumItems() > capacity)
			EngineLogger.error("Items in inventory excees the UI capacity");

		if (inventoryPos == TOP) {
			orgPos.set((width - getWidth()) / 2, height + getHeight());
			targetPos.set((width - getWidth()) / 2, height - getHeight() - DPIUtils.getSpacing());
		} else if (inventoryPos == DOWN) {
			orgPos.set((width - getWidth()) / 2, -getHeight());
			targetPos.set((width - getWidth()) / 2, DPIUtils.getSpacing());
		} else if (inventoryPos == LEFT) {
			orgPos.set(-getWidth(), (height - getHeight()) / 2);
			targetPos.set(DPIUtils.getSpacing(), (height - getHeight()) / 2); // TODO
		} else if (inventoryPos == RIGHT) {
			orgPos.set(width + getWidth(), (height - getHeight()) / 2); // TODO
			targetPos.set(width - getWidth() - DPIUtils.getSpacing(), (height - getHeight()) / 2); // TODO
		} else {
			orgPos.set((width - getWidth()) / 2, -getHeight());
			targetPos.set((width - getWidth()) / 2, (height - getHeight()) / 2);
		}

		setX(orgPos.x);
		setY(orgPos.y);

		menuButton.setPosition(getWidth() - menuButton.getWidth() / 2, (getHeight() - menuButton.getHeight()) / 2);
	}

	public void retrieveAssets(TextureAtlas atlas) {
	}

	@Override
	public void draw(Batch batch, float alpha) {
		Inventory inventory = World.getInstance().getInventory();

		if (!inventory.isVisible()) {
			setVisible(false);
			return;
		}

		if (style.background != null) {
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}

		// DRAW ITEMS
		int capacity = cols * rows;

		for (int i = 0; i < inventory.getNumItems() && i < capacity; i++) {

			SpriteActor a = inventory.getItem(i);
			ActorRenderer r = a.getRenderer();

			float size = (tileSize - rowSpace) / (r.getHeight() > r.getWidth() ? r.getHeight() : r.getWidth());

			float x = i % cols;
			float y = (rows - 1) - i / cols;

			if (style.itemBackground != null) {
				style.itemBackground.draw(batch, getX() + x * tileSize + x * rowSpace + margin, getY() + y * tileSize
						+ y * rowSpace + margin, tileSize, tileSize);
			}

			r.draw((SpriteBatch) batch, getX() + x * tileSize + x * rowSpace + tileSize / 2 + margin, getY()
					+ (tileSize - r.getHeight() * size) / 2 + y * tileSize + y * rowSpace + margin, size);
		}

		super.draw(batch, alpha);
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

	private void stopDragging(int button) {
		World.getInstance().getSceneCamera().getInputUnProject(sceneScreen.getViewport(), mousepos);

		BaseActor targetActor = sceneScreen.getCurrentActor();

		if (targetActor != null) {
			if (targetActor != draggedActor)
				use(targetActor, draggedActor);
			else
				sceneScreen.actorClick(targetActor, button == 1);
		}

		draggedActor = null;
		sceneScreen.getUI().getPointer().drag(null);
	}

	private void use(BaseActor a1, BaseActor a2) {
		if (a1.getVerb("use", a2.getId()) != null) {
			sceneScreen.runVerb(a1, "use", a2.getId());
		} else {
			sceneScreen.runVerb(a2, "use", a1.getId());
		}
	}

	public SpriteActor getItemAt(float x, float y) {
		if (x < margin || y < margin || x >= getWidth() - margin || y >= getHeight() - margin)
			return null;

		Inventory inventory = World.getInstance().getInventory();

		int i = ((rows - 1) - ((int) (y - margin) / (tileSize + (int) rowSpace))) * cols + (int) (x - margin)
				/ (tileSize + (int) rowSpace);

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

	/**
	 * The style for the InventoryUI.
	 * 
	 * @author Rafael Garcia
	 */
	static public class InventoryUIStyle {
		/** Optional. */
		public Drawable background;
		/** Optional. */
		public Drawable itemBackground;
		public ImageButtonStyle menuButtonStyle;

		public InventoryUIStyle() {
		}

		public InventoryUIStyle(InventoryUIStyle style) {
			background = style.background;
			menuButtonStyle = style.menuButtonStyle;
			itemBackground = style.itemBackground;
		}
	}

	public int getInvPosition() {
		return inventoryPos;
	}
}

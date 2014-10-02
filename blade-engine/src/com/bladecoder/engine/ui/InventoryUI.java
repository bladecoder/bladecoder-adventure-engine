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

import com.bladecoder.engine.ui.InventoryUI;
import com.bladecoder.engine.ui.PieMenu;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.UI;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.bladecoder.engine.model.Actor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.SpriteRenderer;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;

public class InventoryUI extends com.badlogic.gdx.scenes.scene2d.Group {
	private final static int TOP = 0;
	private final static int DOWN = 1;
	private final static int LEFT = 2;
	private final static int RIGHT = 3;

	private final static String MENU_BUTTON = "config";

	private Rectangle configBbox = new Rectangle();

	private int tileSize;
	private int margin;
	private int inventoryPos = DOWN;

	private SpriteActor draggedActor = null;

	private AtlasRegion configIcon;

	private final SceneScreen sceneScreen;
	
	private InventoryUIStyle style;
	
	private TextButton menuButton;

	public InventoryUI(SceneScreen scr) {
		style = scr.getUI().getSkin().get(InventoryUIStyle.class);		
		sceneScreen = scr;

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
					stopDragging(button);
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
		
		menuButton = new TextButton("MENU", scr.getUI().getSkin());
		menuButton.setPosition(0, 0);
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
		tileSize = (int)DPIUtils.getMinSize(width, height) * 2;

		int w = (int) (width * .8f / tileSize) * tileSize;
		int h = (int) (height * .7f / tileSize) * tileSize;
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

		
		if (style.background != null) {
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}

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
		World.getInstance().getSceneCamera()
				.getInputUnProject(sceneScreen.getViewport(), mousepos);

		Actor targetActor = sceneScreen.getCurrentActor();

		if (targetActor != null) {
			if(targetActor != draggedActor)
				use(targetActor, draggedActor);
			else
				sceneScreen.actorClick(targetActor, button == 1);
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
		if(x < 0 || y < 0 || x >= getWidth() || y >= getHeight())
			return null;

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
	
	/** The style for the InventoryUI.
	 * @author Rafael Garcia */
	static public class InventoryUIStyle {
		/** Optional. */
		public Drawable background;

		public InventoryUIStyle () {
		}

		public InventoryUIStyle (InventoryUIStyle style) {
			background = style.background;
		}
	}
}

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

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.DPIUtils;

public class LoadSaveScreen implements BladeScreen {
	private static final int ROW_SLOTS = 3;
	private static final int COL_SLOTS = 2;

	private UI ui;

	private Stage stage;
	private Texture bgTexFile = null;

	private boolean loadScreenMode = true;
	
	private int slotWidth = 0;
	private int slotHeight = 0;
	
	// texture list for final dispose
	private final ArrayList<Texture> textureList = new ArrayList<Texture>();

	public LoadSaveScreen() {
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();

		ui.getBatch().setProjectionMatrix(stage.getViewport().getCamera().combined);
		ui.getBatch().begin();
		ui.getPointer().draw(ui.getBatch(), stage.getViewport());
		ui.getBatch().end();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void dispose() {
		stage.dispose();
		stage = null;

		if (bgTexFile != null) {
			bgTexFile.dispose();
		}

		bgTexFile = null;
		
		for(Texture t:textureList)
			t.dispose();
	}

	@Override
	public void show() {
		if (ui.getScreen(UI.Screens.LOAD_GAME) == this)
			loadScreenMode = true;
		else
			loadScreenMode = false;

		stage = new Stage(new ScreenViewport());
		
		float pad = DPIUtils.getMarginSize();
		
		
		slotWidth = (int) (stage.getViewport().getWorldWidth() / (ROW_SLOTS + 1) - 2 * pad);
		slotHeight = (int)(slotWidth * ((float)World.getInstance().getHeight()/(float)World.getInstance().getWidth()));

		LoadSaveScreenStyle style = ui.getSkin().get(LoadSaveScreenStyle.class);
		// BitmapFont f = ui.getSkin().get(style.textButtonStyle,
		// TextButtonStyle.class).font;
		// float buttonWidth = f.getCapHeight() * 15f;

		// Image background = new Image(style.background);
		Drawable bg = style.background;

		if (bg == null && style.bgFile != null) {
			bgTexFile = new Texture(EngineAssetManager.getInstance().getResAsset(style.bgFile));
			bgTexFile.setFilter(TextureFilter.Linear, TextureFilter.Linear);

			bg = new TextureRegionDrawable(new TextureRegion(bgTexFile));
		}

		Table table = new Table(ui.getSkin());
		table.setFillParent(true);
		table.center();
		table.pad(pad);

		Label title = new Label(loadScreenMode ? "LOAD SCREEN" : "SAVE SCREEN", ui.getSkin(), "title");

		TextButton back = new TextButton("Back", ui.getSkin(), "menu");
		back.getLabelCell().padLeft(DPIUtils.MARGIN_SIZE);
		back.getLabelCell().padRight(DPIUtils.MARGIN_SIZE);		
		back.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			}
		});

		back.pad(4);

		if (bg != null)
			table.setBackground(bg);

		table.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK)
					if (World.getInstance().getCurrentScene() != null)
						ui.setCurrentScreen(Screens.SCENE_SCREEN);

				return true;
			}
		});

		PagedScrollPane scroll = new PagedScrollPane();
		scroll.setFlingTime(0.1f);
		scroll.setPageSpacing(25);

		Table slots = new Table().pad(pad);
		slots.defaults().pad(pad, pad, pad, pad);

		int c = 0;
		while (slotExists(c)) {

			if (c % ROW_SLOTS == 0 && c % (ROW_SLOTS * COL_SLOTS) != 0)
				slots.row();

			if (c != 0 && c % (ROW_SLOTS * COL_SLOTS) == 0) {
				scroll.addPage(slots);
				slots = new Table().pad(pad);
				slots.defaults().pad(pad, pad, pad, pad);
			}

			slots.add(getSlotButton(c)).expand().fill();
			c++;
		}

		// Add "new slot" slot for save screen
		if (!loadScreenMode) {
			if (c % ROW_SLOTS == 0 && c % (ROW_SLOTS * COL_SLOTS) != 0)
				slots.row();

			if (c != 0 && c % (ROW_SLOTS * COL_SLOTS) == 0) {
				scroll.addPage(slots);
				slots = new Table().pad(50);
				slots.defaults().pad(20, 40, 20, 40);
			}

			slots.add(getSlotButton(c)).expand().fill();
		}

		// Add last page
		if (slots.getCells().size > 0)
			scroll.addPage(slots);

		table.add(title);
		table.row();

		if (loadScreenMode && !slotExists(0)) {
			Label lbl = new Label("No Saved Games Found", ui.getSkin(), "title");
			lbl.setAlignment(Align.center);
			table.add(lbl).expand().fill();
		} else {
			table.add(scroll).expand().fill();
		}

		table.row();
		table.add(back);
		table.pack();

		stage.setKeyboardFocus(table);
		stage.addActor(table);

		Gdx.input.setInputProcessor(stage);
	}

	private boolean slotExists(int slot) {
		String filename = slot + World.GAMESTATE_EXT;
		return World.getInstance().savedGameExists(filename);
	}

	/**
	 * Creates a button to represent one slot
	 * 
	 * @param slot
	 * @return The button to use for one slot
	 */
	private Button getSlotButton(int slot) {
		Skin skin = ui.getSkin();
		Button button = new Button(skin);
//		ButtonStyle style = button.getStyle();
//		style.up = style.down = skin.getDrawable("black");

		String textLabel = "New Slot";

		if (slotExists(slot)) {
			button.add(getScreenshot(slot)).size(slotWidth, slotHeight);
			textLabel = Integer.toString(slot);
		} else {
			Image fg = new Image(skin.getDrawable("plus"));
			button.add(fg).size(slotHeight/2, slotHeight/2);
			button.setSize(slotWidth, slotHeight);
		}

		button.row();
		
		Label label = new Label(textLabel, skin);
		label.getStyle().background = skin.getDrawable("black");
		label.setAlignment(Align.center);
		
		button.add(label).fillX();

		button.setName(Integer.toString(slot));
		button.addListener(levelClickListener);
		return button;
	}
	
	private Image getScreenshot(int slot) {
		String filename = slot + World.GAMESTATE_EXT + ".png";
		
		Texture t = new Texture(EngineAssetManager.getInstance().getUserFile(filename));
		
		// add to the list for proper dispose when hide the screen
		textureList.add(t);
		
		return new Image(t);
	}

	/**
	 * Handle the click - in real life, we'd go to the level
	 */
	private ClickListener levelClickListener = new ClickListener() {
		@Override
		public void clicked(InputEvent event, float x, float y) {

			if (loadScreenMode == true) {
				World.getInstance().loadGameState(event.getListenerActor().getName() + World.GAMESTATE_EXT);
			} else {
				World.getInstance().saveGameState(event.getListenerActor().getName() + World.GAMESTATE_EXT);
			}

			ui.setCurrentScreen(Screens.SCENE_SCREEN);
		}
	};

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void setUI(UI ui) {
		this.ui = ui;
	}

	/** The style for the MenuScreen */
	static public class LoadSaveScreenStyle {
		/** Optional. */
		public Drawable background;
		/** if 'bg' not specified try to load the bgFile */
		public String bgFile;

		public String textButtonStyle;

		public LoadSaveScreenStyle() {
		}

		public LoadSaveScreenStyle(LoadSaveScreenStyle style) {
			background = style.background;
			bgFile = style.bgFile;
			textButtonStyle = style.textButtonStyle;
		}
	}
}

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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.SnapshotArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.serialization.WorldSerialization;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.ui.defaults.ScreenControllerHandler;
import com.bladecoder.engine.ui.defaults.ScreenControllerHandler.PointerToNextType;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class LoadSaveScreen extends ScreenAdapter implements BladeScreen {
	private static final int ROW_SLOTS = 3;
	private static final int COL_SLOTS = 2;

	private UI ui;

	private Stage stage;
	private Texture bgTexFile = null;

	private boolean loadScreenMode = true;

	private int slotWidth = 0;
	private int slotHeight = 0;

	// texture list for final dispose
	private final ArrayList<Texture> textureList = new ArrayList<>();

	private Pointer pointer;

	private ScreenControllerHandler controller;

	public LoadSaveScreen() {
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
		controller.update(delta);
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		pointer.resize();
	}

	@Override
	public void dispose() {
		if (stage != null) {
			stage.dispose();
			stage = null;

			if (bgTexFile != null) {
				bgTexFile.dispose();
			}

			bgTexFile = null;

			for (Texture t : textureList)
				t.dispose();

			textureList.clear();
		}
	}

	@Override
	public void show() {
		float size = DPIUtils.getPrefButtonSize();
		float pad = DPIUtils.getMarginSize();
		final Skin skin = ui.getSkin();
		final World world = ui.getWorld();

		// loadScreenMode = ui.getScreen(Screens.LOAD_GAME_SCREEN) == this;
		loadScreenMode = world.getCurrentScene() == null;

		stage = new Stage(new ScreenViewport());

		slotWidth = (int) (stage.getViewport().getWorldWidth() / (ROW_SLOTS + 1) - 2 * pad);
		slotHeight = slotWidth * stage.getViewport().getScreenHeight() / stage.getViewport().getScreenWidth();

		LoadSaveScreenStyle style = skin.get(LoadSaveScreenStyle.class);

		Drawable bg = style.background;

		if (bg == null && style.bgFile != null) {
			bgTexFile = new Texture(EngineAssetManager.getInstance().getResAsset(style.bgFile));
			bgTexFile.setFilter(TextureFilter.Linear, TextureFilter.Linear);

			bg = new TextureRegionDrawable(new TextureRegion(bgTexFile));
		}

		Table table = new Table(skin);
		table.setFillParent(true);
		table.center();
		table.pad(pad);

		Label title = new Label(
				loadScreenMode ? world.getI18N().getString("ui.load") : world.getI18N().getString("ui.save"), skin,
				"title");

		Button back = new Button(skin, "back");

		back.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			}
		});

		Table header = new Table();
		// header.padBottom(pad);
		Container<Button> cont = new Container<>(back);
		cont.size(size);
		header.add(cont);
		header.add(title).fillX().expandX().left();
		table.add(header).fillX().expandX().left();

		if (bg != null)
			table.setBackground(bg);

		table.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
					if (world.getCurrentScene() != null)
						ui.setCurrentScreen(Screens.SCENE_SCREEN);
					else
						ui.setCurrentScreen(Screens.MENU_SCREEN);
				}

				return true;
			}
		});

		final PagedScrollPane scroll = new PagedScrollPane();
		scroll.setFlingTime(0.1f);
		scroll.setPageSpacing(0);

		Table slots = new Table().pad(pad);
		slots.defaults().pad(pad).size(slotWidth + pad, slotHeight + pad * 2).top();
		int c = 0;

		// Add "new slot" slot for save screen
		if (!loadScreenMode) {
			slots.add(getSlotButton(Long.toString(new Date().getTime()))).fill().expand();
			c++;
		}

		final List<String> sl = getSlots();

		Collections.sort(sl);

		for (int j = sl.size() - 1; j >= 0; j--) {

			String s = sl.get(j);

			if (c % ROW_SLOTS == 0 && c % (ROW_SLOTS * COL_SLOTS) != 0)
				slots.row();

			if (c != 0 && c % (ROW_SLOTS * COL_SLOTS) == 0) {
				scroll.addPage(slots);
				slots = new Table().pad(pad);
				slots.defaults().pad(pad).size(slotWidth + pad, slotHeight + pad * 2).top();
			}

			Button removeButton = new Button(skin, "delete_game");
			removeButton.setName(s);
			removeButton.addListener(removeClickListener);

			Container<Button> container = new Container<>(removeButton);
			container.size(DPIUtils.getPrefButtonSize() * .75f);
			container.align(Align.topRight);

			slots.stack(getSlotButton(s), container).fill().expand();

			c++;
		}

		// Add last page
		if (slots.getCells().size > 0)
			scroll.addPage(slots);

		table.row();

		if (loadScreenMode && sl.size() == 0) {
			Label lbl = new Label(world.getI18N().getString("ui.noSavedGames"), skin, "title");
			lbl.setAlignment(Align.center);
			lbl.setWrap(true);
			table.add(lbl).expand().fill();
		} else {
			table.add(scroll).expand().fill();
		}

		table.pack();

		stage.setKeyboardFocus(table);
		stage.addActor(table);

		pointer = new Pointer(ui);
		stage.addActor(pointer);

		Gdx.input.setInputProcessor(stage);

		controller = new ScreenControllerHandler(ui, stage, stage.getViewport()) {
			@Override
			protected void focusNext(PointerToNextType type) {
				SnapshotArray<Actor> content = ((Table) scroll.getActor()).getChildren();

				// find content cursor
				int idx = -1;
				for (int i = 0; i < content.size; i++) {
					Vector2 inputPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
					content.get(i).screenToLocalCoordinates(inputPos);
					if (content.get(i).hit(inputPos.x, inputPos.y, false) != null) {
						idx = i;
					}
				}

				// find the slot
				Button b = getButtonUnderCursor(stage);
				if (b != null && idx != -1) {
					int find = ((Table) content.get(idx)).getChildren()
							.indexOf(b.getParent() instanceof Stack ? b.getParent() : b, true);

					if (type == PointerToNextType.LEFT) {
						if (find == 0) {
							EngineLogger.debug(">>>Previous page!!");
							if (idx > 0) {
								idx--;
								scroll.scrollToPage(idx);

								find = ((Table) content.get(idx)).getChildren().size - 1;
							}
						} else if (find == -1) {
							find = 0;
						} else {
							find--;
						}
					} else if (type == PointerToNextType.RIGHT) {

						if (find == ((Table) content.get(idx)).getChildren().size - 1) {
							EngineLogger.debug(">>>Next page!!");
							if (idx < content.size - 1) {
								idx++;
								scroll.scrollToPage(idx);
								find = 0;
							}
						} else if (find == -1) {
							find = 0;
						} else {
							find++;
						}
					}

					cursorToActor(((Table) content.get(idx)).getChildren().get(find));
				} else {
					super.focusNext(type);
				}
			}
		};
	}

	private boolean slotExists(String slot) {
		String filename = slot + WorldSerialization.GAMESTATE_EXT;
		return ui.getWorld().savedGameExists(filename);
	}

	/**
	 * Creates a button to represent one slot
	 *
	 * @param slot
	 * @return The button to use for one slot
	 */
	private Button getSlotButton(String slot) {
		final Skin skin = ui.getSkin();
		final Button button = new Button(new ButtonStyle());
		final ButtonStyle style = button.getStyle();
		style.up = style.down = skin.getDrawable("black");

		String textLabel = ui.getWorld().getI18N().getString("ui.newSlot");
		button.setSize(slotWidth, slotHeight);

		if (slotExists(slot)) {
			button.add(getScreenshot(slot)).maxSize(slotWidth * .95f, slotHeight * .95f);

			try {
				long l = Long.parseLong(slot);

				Date d = new Date(l);
				textLabel = (new SimpleDateFormat()).format(d);
			} catch (Exception e) {
				textLabel = slot;
			}

			button.addListener(loadClickListener);
		} else {
			Image fg = new Image(skin.getDrawable("plus"));
			button.add(fg).maxSize(slotHeight / 2, slotHeight / 2);

			button.addListener(saveClickListener);
		}

		button.row();

		Label label = new Label(textLabel, skin);
		label.setAlignment(Align.center);

		button.add(label).fillX();

		button.setName(slot);
		return button;
	}

	private List<String> getSlots() {
		final List<String> al = new ArrayList<>();

		FileHandle[] list = EngineAssetManager.getInstance().getUserFolder().list();

		for (FileHandle file : list)
			if (file.name().endsWith(WorldSerialization.GAMESTATE_EXT)) {
				String name = file.name().substring(0, file.name().indexOf(WorldSerialization.GAMESTATE_EXT));
				if (!name.equals("default"))
					al.add(name);
			}

		// Add savedgames in '/tests' folder
		if (EngineLogger.debugMode()) {
			String[] list2 = EngineAssetManager.getInstance().listAssetFiles("tests");

			for (String file : list2)
				if (file.endsWith(WorldSerialization.GAMESTATE_EXT)) {
					String name = file.substring(0, file.indexOf(WorldSerialization.GAMESTATE_EXT));
					al.add(name);
				}
		}

		return al;
	}

	private Image getScreenshot(String slot) {
		String filename = slot + WorldSerialization.GAMESTATE_EXT + ".png";

		FileHandle savedFile = null;

		if (EngineAssetManager.getInstance().getUserFile(filename).exists())
			savedFile = EngineAssetManager.getInstance().getUserFile(filename);
		else if (EngineAssetManager.getInstance().assetExists("tests/" + filename))
			savedFile = EngineAssetManager.getInstance().getAsset("tests/" + filename);
		else {
			Drawable d = ui.getSkin().getDrawable("black");

			return new Image(d);
		}

		Texture t = new Texture(savedFile);

		// add to the list for proper dispose when hide the screen
		textureList.add(t);

		return new Image(t);
	}

	private ClickListener loadClickListener = new ClickListener() {
		@Override
		public void clicked(InputEvent event, float x, float y) {
			final World world = ui.getWorld();
			final String filename = event.getListenerActor().getName() + WorldSerialization.GAMESTATE_EXT;

			if (world.savedGameExists() || world.getCurrentScene() != null) {
				Dialog d = new Dialog("", ui.getSkin()) {
					@Override
					protected void result(Object object) {
						if (((Boolean) object).booleanValue()) {
							try {
								// if (loadScreenMode) {
								world.loadGameState(filename);

								ui.setCurrentScreen(Screens.SCENE_SCREEN);

							} catch (IOException e) {
								EngineLogger.error(e.getMessage());
							}
						}
					}
				};

				d.pad(DPIUtils.getMarginSize());
				d.getButtonTable().padTop(DPIUtils.getMarginSize());
				d.getButtonTable().defaults().padLeft(DPIUtils.getMarginSize()).padRight(DPIUtils.getMarginSize());

				Label l = new Label(world.getI18N().getString("ui.override_load"), ui.getSkin(), "ui-dialog");
				l.setWrap(true);
				l.setAlignment(Align.center);

				d.getContentTable().add(l).prefWidth(Gdx.graphics.getWidth() * .7f);

				d.button(world.getI18N().getString("ui.yes"), true,
						ui.getSkin().get("ui-dialog", TextButtonStyle.class));
				d.button(world.getI18N().getString("ui.no"), false,
						ui.getSkin().get("ui-dialog", TextButtonStyle.class));
				d.key(Keys.ENTER, true).key(Keys.ESCAPE, false);

				d.show(stage);
			} else {

				try {
					// if (loadScreenMode) {
					world.loadGameState(filename);

					ui.setCurrentScreen(Screens.SCENE_SCREEN);

				} catch (IOException e) {
					EngineLogger.error(e.getMessage());
				}
			}
		}
	};

	private ClickListener removeClickListener = new ClickListener() {
		@Override
		public void clicked(InputEvent event, float x, float y) {
			final Actor listenerActor = event.getListenerActor();

			Dialog d = new Dialog("", ui.getSkin()) {
				@Override
				protected void result(Object object) {
					if (((Boolean) object).booleanValue()) {
						final World world = ui.getWorld();
						final String filename = listenerActor.getName() + WorldSerialization.GAMESTATE_EXT;

						try {
							world.removeGameState(filename);

							listenerActor.getParent().getParent().getParent()
									.removeActor(listenerActor.getParent().getParent());

						} catch (IOException e) {
							EngineLogger.error(e.getMessage());
						}
					}
				}
			};

			d.pad(DPIUtils.getMarginSize());
			d.getButtonTable().padTop(DPIUtils.getMarginSize());
			d.getButtonTable().defaults().padLeft(DPIUtils.getMarginSize()).padRight(DPIUtils.getMarginSize());

			Label l = new Label(ui.getWorld().getI18N().getString("ui.remove"), ui.getSkin(), "ui-dialog");
			l.setWrap(true);
			l.setAlignment(Align.center);

			d.getContentTable().add(l).prefWidth(Gdx.graphics.getWidth() * .7f);

			d.button(ui.getWorld().getI18N().getString("ui.yes"), true,
					ui.getSkin().get("ui-dialog", TextButtonStyle.class));
			d.button(ui.getWorld().getI18N().getString("ui.no"), false,
					ui.getSkin().get("ui-dialog", TextButtonStyle.class));
			d.key(Keys.ENTER, true).key(Keys.ESCAPE, false);

			d.show(stage);
		}
	};

	private ClickListener saveClickListener = new ClickListener() {
		@Override
		public void clicked(InputEvent event, float x, float y) {
			final World world = ui.getWorld();
			final String filename = event.getListenerActor().getName() + WorldSerialization.GAMESTATE_EXT;

			try {
				world.getSerializer().saveGameState(filename, true);

				world.getCurrentScene().getTextManager().addText("@ui.gamesaved", TextManager.POS_SUBTITLE,
						TextManager.POS_SUBTITLE, false, Text.Type.UI, null, null, null, null, null, null);

				ui.setCurrentScreen(Screens.SCENE_SCREEN);

			} catch (IOException e) {
				EngineLogger.error(e.getMessage());
			}
		}
	};

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void setUI(UI ui) {
		this.ui = ui;
	}

	/**
	 * The style for the MenuScreen
	 */
	public static class LoadSaveScreenStyle {
		/**
		 * Optional.
		 */
		public Drawable background;
		/**
		 * if 'bg' not specified try to load the bgFile
		 */
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

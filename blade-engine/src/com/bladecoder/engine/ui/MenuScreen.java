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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class MenuScreen extends ScreenAdapter implements BladeScreen {
	private final static float BUTTON_PADDING = DPIUtils.UI_SPACE;

	private UI ui;

	private Stage stage;
	private Texture bgTexFile = null;
	private Pointer pointer;
	private Button credits;
	private Button help;
	private Button debug;

	public MenuScreen() {
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		pointer.resize();
		
		float size = DPIUtils.getPrefButtonSize();	
		credits.setSize(size, size);
		help.setSize(size, size);
		debug.setSize(size, size);
	}

	@Override
	public void dispose() {
		stage.dispose();
		stage = null;

		if (bgTexFile != null) {
			bgTexFile.dispose();
		}

		bgTexFile = null;
	}

	@Override
	public void show() {
		stage = new Stage(new ScreenViewport());

		final Skin skin = ui.getSkin();
		final World world = World.getInstance();

		final MenuScreenStyle style = skin.get(MenuScreenStyle.class);
		final BitmapFont f = skin.get(style.textButtonStyle, TextButtonStyle.class).font;
		float buttonWidth = f.getCapHeight() * 15f;

		// Image background = new Image(style.background);
		Drawable bg = style.background;

		if (bg == null && style.bgFile != null) {
			bgTexFile = new Texture(EngineAssetManager.getInstance().getResAsset(style.bgFile));
			bgTexFile.setFilter(TextureFilter.Linear, TextureFilter.Linear);

			bg = new TextureRegionDrawable(new TextureRegion(bgTexFile));
		}

		final Table table = new Table();
		table.setFillParent(true);
		table.center();

		if (bg != null)
			table.setBackground(bg);

		table.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK)
					if (world.getCurrentScene() != null)
						ui.setCurrentScreen(Screens.SCENE_SCREEN);

				return true;
			}
		});

		table.defaults().pad(BUTTON_PADDING).width(buttonWidth);

		stage.setKeyboardFocus(table);

		if (style.showTitle) {

			Label title = new Label(Config.getProperty(Config.TITLE_PROP, "Adventure Blade Engine"), skin,
					style.titleStyle);

			table.add(title).padBottom(DPIUtils.getMarginSize() * 2);
			table.row();
		}

		if (world.savedGameExists() || world.getCurrentScene() != null) {
			TextButton continueGame = new TextButton(I18N.getString("ui.continue"), skin, style.textButtonStyle);

			continueGame.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					if (world.getCurrentScene() == null)
						try {
							world.load();
						} catch (Exception e) {
							Gdx.app.exit();
						}

					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				}
			});

			table.add(continueGame);
		}

		TextButton newGame = new TextButton(I18N.getString("ui.new"), skin, style.textButtonStyle);
		newGame.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				if (world.savedGameExists()) {
					Dialog d = new Dialog("", skin) {
						protected void result(Object object) {
							if (((Boolean) object).booleanValue()) {
								try {
									world.newGame();
								} catch (Exception e) {
									Gdx.app.exit();
								}
								ui.setCurrentScreen(Screens.SCENE_SCREEN);
							}
						}
					};
					
					d.pad(DPIUtils.getMarginSize());
					d.getButtonTable().padTop(DPIUtils.getMarginSize());
					d.getButtonTable().defaults().padLeft(DPIUtils.getMarginSize()).padRight(DPIUtils.getMarginSize());
					
					Label l = new Label( I18N.getString("ui.override"), ui.getSkin(), "ui-dialog");
				    l.setWrap(true);
				    l.setAlignment(Align.center);

				    d.getContentTable().add( l ).prefWidth( Gdx.graphics.getWidth() * .7f);
					
					d.button(I18N.getString("ui.yes"), true, ui.getSkin().get("ui-dialog", TextButtonStyle.class));
					d.button(I18N.getString("ui.no"), false, ui.getSkin().get("ui-dialog", TextButtonStyle.class));
					d.key(Keys.ENTER, true).key(Keys.ESCAPE, false);
					
					d.show(stage);
				} else {

					try {
						world.newGame();
					} catch (Exception e) {
						Gdx.app.exit();
					}
					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				}
			}
		});

		table.row();
		table.add(newGame);

		TextButton loadGame = new TextButton(I18N.getString("ui.load"), skin, style.textButtonStyle);
		loadGame.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.LOAD_GAME_SCREEN);
			}
		});

		table.row();
		table.add(loadGame);

		if (world.getCurrentScene() != null) {
			TextButton saveGame = new TextButton(I18N.getString("ui.save"), skin, style.textButtonStyle);
			saveGame.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					ui.setCurrentScreen(Screens.SAVE_GAME_SCREEN);
				}
			});

			table.row();
			table.add(saveGame);
		}

		TextButton quit = new TextButton(I18N.getString("ui.quit"), skin, style.textButtonStyle);
		quit.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});

		table.row();
		table.add(quit);

		table.pack();

		stage.addActor(table);

		// BOTTOM-RIGHT BUTTON STACK
		credits = new Button(skin, "credits");
		credits.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.CREDIT_SCREEN);
			}
		});

		help = new Button(skin, "help");
		help.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.HELP_SCREEN);
			}
		});

		debug = new Button(skin, "debug");
		debug.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				DebugScreen debugScr = new DebugScreen();
				debugScr.setUI(ui);
				ui.setCurrentScreen(debugScr);
			}
		});

		Table buttonStack = new Table();
		buttonStack.defaults().pad(DPIUtils.getSpacing()).size(DPIUtils.getPrefButtonSize(),
				DPIUtils.getPrefButtonSize());
		buttonStack.pad(DPIUtils.getMarginSize() * 2);

		if (EngineLogger.debugMode() && world.getCurrentScene() != null) {
			buttonStack.add(debug);
			buttonStack.row();
		}

		buttonStack.add(help);
		buttonStack.row();
		buttonStack.add(credits);
		buttonStack.bottom().right();
		buttonStack.setFillParent(true);
		buttonStack.pack();
		stage.addActor(buttonStack);

		pointer = new Pointer(skin);
		stage.addActor(pointer);

		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void setUI(UI ui) {
		this.ui = ui;
	}

	/** The style for the MenuScreen */
	public static class MenuScreenStyle {
		/** Optional. */
		public Drawable background;
		/** if 'bg' not specified try to load the bgFile */
		public String bgFile;
		public String textButtonStyle;
		public String titleStyle;
		public boolean showTitle;

		public MenuScreenStyle() {
		}

		public MenuScreenStyle(MenuScreenStyle style) {
			background = style.background;
			bgFile = style.bgFile;
			textButtonStyle = style.textButtonStyle;
			showTitle = style.showTitle;
			titleStyle = style.titleStyle;
		}
	}
}

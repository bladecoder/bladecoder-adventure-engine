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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class MenuScreen implements BladeScreen {
	private final static float BUTTON_PADDING = DPIUtils.UI_SPACE;
	
	private UI ui;

	private Stage stage;
	private Texture bgTexFile = null;

	public MenuScreen() {
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();

		ui.getBatch().setProjectionMatrix(
				stage.getViewport().getCamera().combined);
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
	}

	@Override
	public void show() {
		// int wWidth =
		// EngineAssetManager.getInstance().getResolution().portraitWidth;
		// int wHeight =
		// EngineAssetManager.getInstance().getResolution().portraitHeight;
		//
		// stage = new Stage(new ExtendViewport(wWidth, wHeight/2));

		stage = new Stage(new ScreenViewport());

		MenuScreenStyle style = ui.getSkin().get(MenuScreenStyle.class);
		BitmapFont f = ui.getSkin().get(style.textButtonStyle, TextButtonStyle.class).font;	
		float buttonWidth = f.getCapHeight() * 15f;
		
		
		// Image background = new Image(style.background);
		Drawable bg = style.background;

		if (bg == null && style.bgFile != null) {
			bgTexFile = new Texture(EngineAssetManager.getInstance()
					.getResAsset(style.bgFile));
			bgTexFile.setFilter(TextureFilter.Linear, TextureFilter.Linear);

			bg = new TextureRegionDrawable(new TextureRegion(bgTexFile));
		}

		Table table = new Table();
		table.setFillParent(true);
		table.center();

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

		stage.setKeyboardFocus(table);

		if (style.showTitle) {

			Label title = new Label(Config.getProperty(Config.TITLE_PROP,
					"Adventure Blade Engine"), ui.getSkin(), style.titleStyle);

			table.add(title).padBottom(DPIUtils.getMarginSize() * 2);
			table.row();
		}

		if (World.getInstance().savedGameExists()
				|| World.getInstance().getCurrentScene() != null) {
			TextButton continueGame = new TextButton("Continue", ui.getSkin(),
					style.textButtonStyle);

			continueGame.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					if (World.getInstance().getCurrentScene() == null)
						World.getInstance().load();

					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				}
			});

			table.add(continueGame).pad(BUTTON_PADDING).width(buttonWidth);
		}

		TextButton newGame = new TextButton("New Game", ui.getSkin(),
				style.textButtonStyle);
		newGame.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				World.getInstance().newGame();
				ui.setCurrentScreen(Screens.SCENE_SCREEN);
			}
		});

		table.row();
		table.add(newGame).pad(BUTTON_PADDING).width(buttonWidth);

		TextButton help = new TextButton("Help", ui.getSkin(),
				style.textButtonStyle);
		help.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.HELP_SCREEN);
			}
		});

		table.row();
		table.add(help).pad(BUTTON_PADDING).width(buttonWidth);

		TextButton credits = new TextButton("Credits", ui.getSkin(),
				style.textButtonStyle);
		credits.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.CREDIT_SCREEN);
			}
		});

		table.row();
		table.add(credits).pad(BUTTON_PADDING).width(buttonWidth);

		if (EngineLogger.debugMode()) {
			TextButton debug = new TextButton("[RED]Debug[]", ui.getSkin(),
					style.textButtonStyle);
			debug.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					DebugScreen debugScr = new DebugScreen();
					debugScr.setUI(ui);
					ui.setCurrentScreen(debugScr);
				}
			});

			table.row();
			table.add(debug).pad(BUTTON_PADDING).width(buttonWidth);
		}

		TextButton quit = new TextButton("Quit Game", ui.getSkin(),
				style.textButtonStyle);
		quit.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});

		table.row();
		table.add(quit).pad(BUTTON_PADDING).width(buttonWidth);

		table.pack();

		stage.addActor(table);

		Gdx.input.setInputProcessor(stage);
	}

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
	static public class MenuScreenStyle {
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

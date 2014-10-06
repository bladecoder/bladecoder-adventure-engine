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

import com.bladecoder.engine.ui.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class MenuScreenTextButtons implements Screen {
	private UI ui;

	private Stage stage;

	public MenuScreenTextButtons(UI ui) {
		this.ui = ui;
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

		Table table = new Table();
		table.setFillParent(true);
		table.center();

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

		Label title = new Label(Config.getProperty(Config.TITLE_PROP, "Adventure Blade Engine"), ui.getSkin(), "title");

		table.add(title).padBottom(DPIUtils.getMarginSize() * 2);

		if (World.getInstance().savedGameExists()) {
			TextButton continueGame = new TextButton("Continue", ui.getSkin(), "menu");

			continueGame.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					if (World.getInstance().getCurrentScene() == null)
						World.getInstance().load();

					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				}
			});

			table.row();
			table.add(continueGame);
		}

		TextButton newGame = new TextButton("New Game", ui.getSkin(), "menu");
		newGame.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				World.getInstance().newGame();
				ui.setCurrentScreen(Screens.SCENE_SCREEN);
			}
		});

		table.row();
		table.add(newGame);

		TextButton help = new TextButton("Help", ui.getSkin(), "menu");
		help.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.HELP_SCREEN);
			}
		});

		table.row();
		table.add(help);

		TextButton credits = new TextButton("Credits", ui.getSkin(), "menu");
		credits.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.CREDIT_SCREEN);
			}
		});

		table.row();
		table.add(credits);
		
		if(EngineLogger.debugMode()) {
			TextButton debug = new TextButton("[RED]Debug[]", ui.getSkin(), "menu");
			debug.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					ui.setCurrentScreen(new DebugScreen(ui));
				}
			});

			table.row();
			table.add(debug);
		}

		TextButton quit = new TextButton("Quit Game", ui.getSkin(), "menu");
		quit.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				World.getInstance().dispose();
				Gdx.app.exit();
			}
		});

		table.row();
		table.add(quit);

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
}

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

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.ui.UI.State;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class MenuScreen implements Screen {

	public static final String BACK_COMMAND = "back";
	public static final String QUIT_COMMAND = "quit";
	public static final String RELOAD_COMMAND = "reload";
	public static final String HELP_COMMAND = "help";
	public static final String CREDITS_COMMAND = "credits";

	private static final float MARGIN = 15;

	UI ui;

	Stage stage;

	public MenuScreen(UI ui) {
		this.ui = ui;
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
		ui.getPointer().resize(width, height);
	}

	@Override
	public void dispose() {
		stage.dispose();
		stage = null;
	}

	@Override
	public void show() {
		int wWidth = EngineAssetManager.getInstance().getResolution().portraitWidth;
		int wHeight = EngineAssetManager.getInstance().getResolution().portraitHeight;

		stage = new Stage(new ExtendViewport(wWidth, wHeight));

		Table table = new Table();
		table.setFillParent(true);
		table.center();

		table.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE
						|| keycode == Input.Keys.BACK)
					ui.setScreen(State.SCENE_SCREEN);
				return true;
			}
		});

		stage.setKeyboardFocus(table);

		ImageButton back = new ImageButton(new TextureRegionDrawable(ui
				.getUIAtlas().findRegion(BACK_COMMAND)));
		back.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setScreen(State.SCENE_SCREEN);
			}
		});

		table.add(back).pad(MARGIN);

		ImageButton reload = new ImageButton(new TextureRegionDrawable(ui
				.getUIAtlas().findRegion(RELOAD_COMMAND)));
		reload.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setScreen(State.RESTART_SCREEN);
			}
		});

		table.add(reload).pad(MARGIN);

		ImageButton help = new ImageButton(new TextureRegionDrawable(ui
				.getUIAtlas().findRegion(HELP_COMMAND)));
		help.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setScreen(State.HELP_SCREEN);
			}
		});

		table.add(help).pad(MARGIN);

		ImageButton credits = new ImageButton(new TextureRegionDrawable(ui
				.getUIAtlas().findRegion(CREDITS_COMMAND)));
		credits.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setScreen(State.CREDIT_SCREEN);
			}
		});

		table.add(credits).pad(MARGIN);

		ImageButton quit = new ImageButton(new TextureRegionDrawable(ui
				.getUIAtlas().findRegion(QUIT_COMMAND)));
		quit.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				Gdx.app.exit();
			}
		});

		table.add(quit).pad(MARGIN);
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

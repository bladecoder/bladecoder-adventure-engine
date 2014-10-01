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

import org.bladecoder.engine.ui.UI.Screens;
import org.bladecoder.engine.util.DPIUtils;

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

public class DebugScreen implements Screen {
	private UI ui;

	private Stage stage;

	public DebugScreen(UI ui) {
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

		stage = new Stage(new ScreenViewport());

		Table table = new Table();
		table.setFillParent(true);
		table.center();

		table.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK)
					ui.setCurrentScreen(Screens.MENU_SCREEN);
				return true;
			}
		});

		stage.setKeyboardFocus(table);

		Label title = new Label("DEBUG SCREEN", ui.getSkin(), "title");

		table.add(title).padBottom(DPIUtils.getMarginSize() * 2);

		TextButton back = new TextButton("Back", ui.getSkin(), "menu");
		back.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			}
		});

		table.row();
		table.add(back);

		Recorder r = ((SceneScreen) ui.getScreen(Screens.SCENE_SCREEN)).getRecorder();
		TextButton play = new TextButton(r.isPlaying() ? "Stop" : "Play", ui.getSkin(), "menu");
		play.addListener(new ClickListener() {
			
			public void clicked(InputEvent event, float x, float y) {
				SceneScreen scnScr = (SceneScreen) ui.getScreen(Screens.SCENE_SCREEN);
				Recorder r = scnScr.getRecorder();
				
				if (!r.isPlaying()) {
					scnScr.setSpeed(4.0f);
					r.load("full");
					r.setPlaying(true);
					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				} else {
					scnScr.setSpeed(1.0f);
					r.setPlaying(false);
					ui.setCurrentScreen(Screens.MENU_SCREEN);
				}
			}
		});

		table.row();
		table.add(play);

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

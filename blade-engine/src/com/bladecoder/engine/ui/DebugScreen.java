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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.DPIUtils;

public class DebugScreen implements BladeScreen {
	private UI ui;

	private Stage stage;

	private TextField speedText;
	SelectBox<String> recordings;
	SelectBox<String> scenes;

	public DebugScreen() {
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

		Table table = new Table(ui.getSkin());
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

		table.add(title).padBottom(DPIUtils.getMarginSize() * 2).colspan(3);

		// ------------- SPEED
		speedText = new TextField(Float.toString(((SceneScreen) ui.getScreen(Screens.SCENE_SCREEN)).getSpeed()),
				ui.getSkin());
		TextButton speedButton = new TextButton("Set Speed", ui.getSkin());
		speedButton.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				SceneScreen scnScr = (SceneScreen) ui.getScreen(Screens.SCENE_SCREEN);
				scnScr.setSpeed(Float.parseFloat(speedText.getText()));
			}
		});
		
		speedButton.pad(2,3,2,3);
		table.row().pad(5).align(Align.left);
		table.add("Game Speed: ");
		table.add(speedText);
		table.add(speedButton);

		// ------------- RECORDING

		Recorder r = ((SceneScreen) ui.getScreen(Screens.SCENE_SCREEN)).getRecorder();
		TextButton play = new TextButton(r.isPlaying() ? "Stop" : "Play", ui.getSkin());
		TextButton rec = new TextButton(r.isRecording() ? "Stop Rec" : "Rec", ui.getSkin());
		play.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				SceneScreen scnScr = (SceneScreen) ui.getScreen(Screens.SCENE_SCREEN);
				Recorder r = scnScr.getRecorder();

				if (!r.isPlaying()) {
					r.load(recordings.getSelected());
					r.setPlaying(true);
					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				} else {
					r.setPlaying(false);
					ui.setCurrentScreen(Screens.MENU_SCREEN);
				}
			}
		});

		rec.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				SceneScreen scnScr = (SceneScreen) ui.getScreen(Screens.SCENE_SCREEN);
				Recorder r = scnScr.getRecorder();

				if (r.isPlaying()) {
					r.setPlaying(false);
				}

				r.setRecording(!r.isRecording());
			}
		});

		recordings = new SelectBox<String>(ui.getSkin());
		
		String[] testFiles = EngineAssetManager.getInstance().listAssetFiles("/tests");
		ArrayList<String> al = new ArrayList<String>();
		
		for(String file:testFiles)
			if(file.endsWith(".verbs.rec"))
				al.add(file.substring(0,file.indexOf(".verbs.rec")));
		
		recordings.setItems(al.toArray(new String[al.size()]));
		
		play.pad(2,3,2,3);
		rec.pad(2,3,2,3);

		table.row().pad(5).align(Align.left);
		table.add("Game Recording: ");
		table.add(recordings);
		table.add(play);
		table.add(rec);

		// ------------- SCENES
		TextButton go = new TextButton("Go", ui.getSkin());
		go.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				World.getInstance().setCurrentScene(scenes.getSelected());
				ui.setCurrentScreen(Screens.SCENE_SCREEN);
			}
		});
		
		go.pad(2,3,2,3);

		scenes = new SelectBox<String>(ui.getSkin());
		scenes.setItems(World.getInstance().getScenes().keySet().toArray(new String[World.getInstance().getScenes().size()]));

		table.row().pad(5).align(Align.left);
		table.add("Go to Scene: ");
		table.add(scenes);
		table.add(go);

		// ------------- BACK BUTTON

		TextButton back = new TextButton("Back", ui.getSkin(), "menu");
		back.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.MENU_SCREEN);
			}
		});
		
		back.pad(4,4,4,4);

		table.row().pad(5);
		table.add(back).colspan(3);

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
}

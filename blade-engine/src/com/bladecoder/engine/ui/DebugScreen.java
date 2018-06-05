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
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.DPIUtils;
import com.bladecoder.engine.util.EngineLogger;

public class DebugScreen implements BladeScreen {
	private UI ui;

	private Stage stage;

	private TextField speedText;
	private SelectBox<String> recordings;
	private SelectBox<String> scenes;
	private TextField recFilename;
	private TextButton rec;

	private TextField testerTimeConf;
	private TextField inSceneTimeConf;
	private TextField testerExcludeList;

	private Pointer pointer;

	public DebugScreen() {
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
		table.invalidate();
	}

	@Override
	public void dispose() {
		if (stage != null) {
			stage.dispose();
			stage = null;
		}
	}

	Table table;

	@Override
	public void show() {
		float size = DPIUtils.getPrefButtonSize();
		float margin = DPIUtils.getMarginSize();

		stage = new Stage(new ScreenViewport());

		table = new Table(ui.getSkin());
		table.setFillParent(true);
		table.left().top();
		table.pad(margin);

		table.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, int keycode) {
				if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK)
					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				return true;
			}
		});

		stage.setKeyboardFocus(table);

		Button back = new Button(ui.getSkin(), "back");

		back.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				ui.setCurrentScreen(Screens.SCENE_SCREEN);
			}
		});

		Label title = new Label("DEBUG SCREEN", ui.getSkin(), "title");

		Table header = new Table();
		header.padBottom(margin);
		Container<Button> cont = new Container<Button>(back);
		cont.size(size);
		header.add(cont);
		header.add(title).fillX().expandX().left();
		table.add(header).colspan(3).fillX().expandX().left();

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

		speedButton.pad(2, 3, 2, 3);
		HorizontalGroup sGroup = new HorizontalGroup();
		sGroup.space(10);
		sGroup.addActor(speedText);
		sGroup.addActor(speedButton);

		table.row().pad(5).align(Align.left);
		table.add(new Label("Game Speed: ", ui.getSkin(), "debug"));
		table.add(sGroup);

		// ------------- RECORDING

		final Recorder r = ui.getRecorder();
		TextButton play = new TextButton(r.isPlaying() ? "Stop" : "Play", ui.getSkin());
		rec = new TextButton(r.isRecording() ? "Stop Rec" : "Rec", ui.getSkin());
		play.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				final Recorder r = ui.getRecorder();

				if (!r.isPlaying()) {
					r.setFilename(recordings.getSelected());
					r.load();
					r.setPlaying(true);
					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				} else {
					r.setPlaying(false);
					ui.setCurrentScreen(Screens.SCENE_SCREEN);
				}
			}
		});

		rec.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				final Recorder r = ui.getRecorder();

				if (r.isPlaying()) {
					r.setPlaying(false);
				}

				if (!r.isRecording())
					r.setFilename(recFilename.getText());

				r.setRecording(!r.isRecording());
				rec.setText(r.isRecording() ? "Stop Rec" : "Rec");

				if (r.isRecording())
					ui.setCurrentScreen(Screens.SCENE_SCREEN);
			}
		});

		recordings = new SelectBox<String>(ui.getSkin());

		String[] testFiles = EngineAssetManager.getInstance().listAssetFiles("tests");
		ArrayList<String> al = new ArrayList<String>();

		for (String file : testFiles)
			if (file.endsWith(Recorder.RECORD_EXT))
				al.add(file.substring(0, file.indexOf(Recorder.RECORD_EXT)));

		FileHandle[] testFiles2 = EngineAssetManager.getInstance().getUserFolder().list();

		for (FileHandle file : testFiles2)
			if (file.name().endsWith(Recorder.RECORD_EXT))
				al.add(file.name().substring(0, file.name().indexOf(Recorder.RECORD_EXT)));

		recordings.setItems(al.toArray(new String[al.size()]));

		play.pad(2, 3, 2, 3);
		rec.pad(2, 3, 2, 3);

		recFilename = new TextField(r.getFileName(), ui.getSkin());

		HorizontalGroup rGroup = new HorizontalGroup();
		rGroup.space(10);
		rGroup.addActor(recordings);
		rGroup.addActor(play);
		rGroup.addActor(new Label("Rec. Filename", ui.getSkin(), "debug"));
		rGroup.addActor(recFilename);
		rGroup.addActor(rec);

		table.row().pad(5).align(Align.left);
		table.add(new Label("Game Recording: ", ui.getSkin(), "debug"));
		table.add(rGroup);
		
		// ------------- LOAD CHAPTER
		table.row().pad(5).align(Align.left);
		table.add(new Label("Load Chapter: ", ui.getSkin(), "debug"));
		
		HorizontalGroup chGroup = new HorizontalGroup();
		chGroup.space(10);
		
		final TextField chapter = new TextField("", ui.getSkin());
		chGroup.addActor(chapter);
		
		TextButton loadButton = new TextButton("Load", ui.getSkin());
		loadButton.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				String c = chapter.getText();
				
				if(!c.isEmpty()) {
					try {
						ui.getWorld().getSerializer().loadChapter(c);
						ui.setCurrentScreen(Screens.SCENE_SCREEN);
					} catch (IOException e) {
						EngineLogger.error("Loading chapter.", e);
					}
				}
			}
		});
		
		chGroup.addActor(loadButton);
		
		table.add(chGroup);

		// ------------- SCENES
		final TextButton testScene = new TextButton("Run Test Verb", ui.getSkin(), "toggle");
		final TextButton initScene = new TextButton("Init", ui.getSkin(), "toggle");
		
		initScene.setChecked(true);
		
		TextButton go = new TextButton("Go", ui.getSkin());
		go.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				ui.getWorld().resume();
				ui.getWorld().setCutMode(false);
				
				if(testScene.isChecked())
					ui.getWorld().setTestScene(scenes.getSelected());
				
				ui.getWorld().enterScene(scenes.getSelected(), initScene.isChecked());
				ui.setCurrentScreen(Screens.SCENE_SCREEN);
			}
		});

		go.pad(2, 3, 2, 3);

		scenes = new SelectBox<String>(ui.getSkin());
		scenes.setItems(
				ui.getWorld().getScenes().keySet().toArray(new String[ui.getWorld().getScenes().size()]));

		HorizontalGroup scGroup = new HorizontalGroup();
		scGroup.space(10);
		scGroup.addActor(scenes);
		scGroup.addActor(go);
		scGroup.addActor(testScene);
		scGroup.addActor(initScene);

		table.row().pad(5).align(Align.left);
		table.add(new Label("Go to Scene: ", ui.getSkin(), "debug"));
		table.add(scGroup);

		// ------------- TESTERBOT
		final TesterBot bot = ui.getTesterBot();

		TextButton runBot = new TextButton(bot.isEnabled() ? "Stop" : "Run", ui.getSkin());
		runBot.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				final TesterBot bot = ui.getTesterBot();

				bot.setMaxWaitInverval(Float.parseFloat(testerTimeConf.getText()));
				bot.setInSceneTime(Float.parseFloat(inSceneTimeConf.getText()));
				bot.setExcludeList(testerExcludeList.getText());
				bot.setEnabled(!bot.isEnabled());

				ui.setCurrentScreen(Screens.SCENE_SCREEN);
			}
		});

		runBot.pad(2, 3, 2, 3);

		testerTimeConf = new TextField(Float.toString(bot.getMaxWaitInverval()), ui.getSkin());
		inSceneTimeConf = new TextField(Float.toString(bot.getInSceneTime()), ui.getSkin());
		testerExcludeList = new TextField(bot.getExcludeList(), ui.getSkin());

		TextButton testerLeaveConf = new TextButton("Leave", ui.getSkin(), "toggle");
		testerLeaveConf.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				final TesterBot bot = ui.getTesterBot();

				bot.setRunLeaveVerbs(!bot.isRunLeaveVerbs());
			}
		});

		testerLeaveConf.setChecked(bot.isRunLeaveVerbs());

		TextButton testerGotoConf = new TextButton("Goto", ui.getSkin(), "toggle");
		testerGotoConf.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				final TesterBot bot = ui.getTesterBot();

				bot.setRunGoto(!bot.isRunGoto());
			}
		});

		testerGotoConf.setChecked(bot.isRunGoto());

		TextButton testerPassText = new TextButton("Pass Texts", ui.getSkin(), "toggle");
		testerPassText.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				final TesterBot bot = ui.getTesterBot();

				bot.setPassTexts(!bot.isPassTexts());
			}
		});

		testerPassText.setChecked(bot.isPassTexts());

		TextButton testerWaitWhenWalking = new TextButton("Wait When Walking", ui.getSkin(), "toggle");
		testerWaitWhenWalking.addListener(new ClickListener() {

			public void clicked(InputEvent event, float x, float y) {
				final TesterBot bot = ui.getTesterBot();

				bot.setWaitWhenWalking(!bot.isWaitWhenWalking());
			}
		});

		testerWaitWhenWalking.setChecked(bot.isWaitWhenWalking());

		HorizontalGroup botGroup = new HorizontalGroup();
		botGroup.space(10);

		botGroup.addActor(testerLeaveConf);
		botGroup.addActor(testerGotoConf);
		botGroup.addActor(testerPassText);
		botGroup.addActor(testerWaitWhenWalking);

		HorizontalGroup botGroup2 = new HorizontalGroup();
		botGroup2.space(10);

		botGroup2.addActor(new Label("Excl. List: ", ui.getSkin(), "debug"));
		botGroup2.addActor(testerExcludeList);
		botGroup2.addActor(new Label("Interval: ", ui.getSkin(), "debug"));
		botGroup2.addActor(testerTimeConf);
		botGroup2.addActor(new Label("Scn Time: ", ui.getSkin(), "debug"));
		botGroup2.addActor(inSceneTimeConf);
		botGroup2.addActor(runBot);

		table.row().pad(5).align(Align.left);
		table.add(new Label("Tester Bot: ", ui.getSkin(), "debug"));
		table.add(botGroup);
		table.row().pad(5).align(Align.left);
		table.add();
		table.add(botGroup2);

		// ------------- VERSION LABEL NOT IN TABLE
		String versionString = Config.getProperty(Config.TITLE_PROP, "title unspecified") + " v"
				+ Config.getProperty(Config.VERSION_PROP, "unspecified") + "\n" + "Blade Engine: v"
				+ Config.getProperty(Config.BLADE_ENGINE_VERSION_PROP, "unspecified") + "\n" + "libGdx: v"
				+ Config.getProperty("gdxVersion", "unspecified") + "\n" + "RoboVM: v"
				+ Config.getProperty("roboVMVersion", "unspecified") + "\n";
		// + "Gdx.app.getVersion: " + Gdx.app.getVersion();

		Label version = new Label(versionString, ui.getSkin(), "debug");
		version.setColor(Color.LIGHT_GRAY);
		Table versionStack = new Table();
		versionStack.defaults().pad(DPIUtils.getSpacing());
		versionStack.pad(0);
		versionStack.add(version);
		versionStack.bottom().left();
		versionStack.setFillParent(true);
		versionStack.pack();
		table.row();
		table.add(versionStack).colspan(3).left();

		table.pack();

		ScrollPane scrollPane = new ScrollPane(table);
		scrollPane.setFillParent(true);
		stage.addActor(scrollPane);

		pointer = new Pointer(ui.getSkin());
		stage.addActor(pointer);

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

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
package com.bladecoder.engine;

import java.nio.IntBuffer;
import java.text.MessageFormat;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.ui.SceneScreen;
import com.bladecoder.engine.ui.UI;
import com.bladecoder.engine.ui.UI.Screens;
import com.bladecoder.engine.util.Config;
import com.bladecoder.engine.util.EngineLogger;

public class BladeEngine implements ApplicationListener {

	private String chapter;
	private String gameState;
	private String testScene;
	private String recordName;
	private String forceRes;
	private boolean debug = false;
	private boolean restart = false;
	private UI ui;

	public static UI getAppUI() {
		return ((BladeEngine) Gdx.app.getApplicationListener()).getUI();
	}

	public void setTestMode(String s) {
		testScene = s;
	}

	public void loadGameState(String s) {
		gameState = s;
	}

	public void setPlayMode(String recordName) {
		this.recordName = recordName;
	}

	public void setDebugMode() {
		debug = true;
	}

	public void setRestart() {
		restart = true;
	}

	public void setChapter(String chapter) {
		this.chapter = chapter;
	}

	public void forceResolution(String forceRes) {
		this.forceRes = forceRes;
	}

	public UI getUI() {
		return ui;
	}

	@Override
	public void create() {
		if (!debug)
			debug = Config.getProperty(Config.DEBUG_PROP, debug);

		if (debug)
			EngineLogger.setDebug();

		EngineLogger.debug("GAME CREATE");

		if (forceRes == null)
			forceRes = Config.getProperty(Config.FORCE_RES_PROP, forceRes);

		if (forceRes != null) {
			EngineAssetManager.getInstance().forceResolution(forceRes);
		}

		World.getInstance().loadXMLWorld();

		ui = new UI();

		if (chapter == null)
			chapter = Config.getProperty(Config.CHAPTER_PROP, chapter);

		if (testScene == null) {
			testScene = Config.getProperty(Config.TEST_SCENE_PROP, testScene);
		}

		if (testScene != null || chapter != null) {
			World.getInstance().loadXMLChapter(chapter, testScene);
			ui.setCurrentScreen(UI.Screens.SCENE_SCREEN);
		}

		if (gameState == null)
			gameState = Config.getProperty(Config.LOAD_GAMESTATE_PROP, gameState);

		if (gameState != null) {
			World.getInstance().loadGameState(gameState);
		}

		if (restart) {
			try {
				World.getInstance().loadXMLChapter(null);
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);
				dispose();
				Gdx.app.exit();
			}
		}

		if (recordName == null)
			recordName = Config.getProperty(Config.PLAY_RECORD_PROP, recordName);

		if (recordName != null) {
			SceneScreen scr = (SceneScreen) ui.getScreen(Screens.SCENE_SCREEN);
			scr.getRecorder().setFilename(recordName);
			scr.getRecorder().load();
			scr.getRecorder().setPlaying(true);
		}

		if (EngineLogger.debugMode()) {
			IntBuffer size = BufferUtils.newIntBuffer(16);
			Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, size);
			int maxSize = size.get();

			EngineLogger.debug("Max. texture Size: " + maxSize);
			EngineLogger.debug("Density: " + Gdx.graphics.getDensity());
		}
	}

	@Override
	public void dispose() {
		EngineLogger.debug("GAME DISPOSE");
		World.getInstance().dispose();
		ui.dispose();
	}

	@Override
	public void render() {
		ui.render();

		// Pause the game and save state when an error is found
		if (EngineLogger.lastError != null && EngineLogger.debugMode() && !World.getInstance().isPaused()) {
			ui.pause();
			World.getInstance().saveGameState();
		}
	}

	@Override
	public void resize(int width, int height) {
		EngineLogger.debug(MessageFormat.format("GAME RESIZE {0}x{1}", width, height));
		ui.resize(width, height);
	}

	@Override
	public void pause() {
		SceneScreen scnScr = (SceneScreen) ui.getScreen(Screens.SCENE_SCREEN);
		boolean bot = scnScr.getTesterBot().isEnabled();
		boolean r = scnScr.getRecorder().isPlaying();

		if (!bot && !r) {
			EngineLogger.debug("GAME PAUSE");
			ui.pause();
			World.getInstance().saveGameState();
		} else {
			EngineLogger.debug("NOT PAUSING WHEN BOT IS RUNNING OR PLAYING RECORDED GAME");
		}
	}

	@Override
	public void resume() {
		EngineLogger.debug("GAME RESUME");
		ui.resume();

		// resets the error when continue
		if (EngineLogger.lastError != null && EngineLogger.debugMode()) {
			EngineLogger.lastError = null;
			EngineLogger.lastException = null;
		}
	}

}

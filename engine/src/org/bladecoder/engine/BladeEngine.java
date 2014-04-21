package org.bladecoder.engine;

import java.nio.IntBuffer;
import java.text.MessageFormat;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.ui.UI;
import org.bladecoder.engine.util.Config;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;

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
		return ((BladeEngine)Gdx.app.getApplicationListener()).getUI();
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
		if(!debug)
			debug = Config.getProperty(Config.DEBUG_PROP, debug);
		
		if(debug)
			EngineLogger.setDebug();
		
		EngineLogger.debug("GAME CREATE");
		
		if(forceRes == null)
			forceRes = Config.getProperty(Config.FORCE_RES_PROP, forceRes);
		
		if(forceRes != null) {
			EngineAssetManager.getInstance().forceResolution(Integer.parseInt(forceRes));
		}
		
		ui = new UI();

		if(chapter == null)
			chapter = Config.getProperty(Config.CHAPTER_PROP, chapter);
		
		if(testScene == null)
			testScene = Config.getProperty(Config.TEST_SCENE_PROP, testScene);
		
		if (testScene != null || chapter != null) {
			World.getInstance().loadXML(chapter, testScene);
		}
		
		if(gameState == null)
			gameState = Config.getProperty(Config.LOAD_GAMESTATE_PROP, gameState);
		
		if (gameState != null) {
			World.getInstance().loadGameState(gameState);
		}
		
		if(restart) {
			try {
				World.getInstance().loadXML(null);
			} catch (Exception e) {
				EngineLogger.error("ERROR LOADING GAME", e);
				dispose();
				Gdx.app.exit();
			}
		}
		
		if(recordName == null)
			recordName = Config.getProperty(Config.PLAY_RECORD_PROP, recordName);
		
		if (recordName != null) {
			ui.getRecorder().load(recordName);
			ui.getRecorder().setPlaying(true);
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
		ui.update();
		ui.draw();
	}

	@Override
	public void resize(int width, int height) {
		EngineLogger.debug(MessageFormat.format("GAME RESIZE {0}x{1}", width, height));
		ui.resize(width, height);
	}

	@Override
	public void pause() {
		EngineLogger.debug("GAME PAUSE");
		World.getInstance().pause();
		World.getInstance().saveGameState();
	}

	@Override
	public void resume() {
		EngineLogger.debug("GAME RESUME");
		
		ui.restoreGLContext();
		World.getInstance().resume();
	}

}

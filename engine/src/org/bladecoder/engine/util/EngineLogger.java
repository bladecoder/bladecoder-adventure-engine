package org.bladecoder.engine.util;

import org.bladecoder.engine.assets.EngineAssetManager;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class EngineLogger {
	private static String TAG = "ENGINE";
	private static int level = Application.LOG_ERROR;

	public static final int DEBUG0 = 0;
	public static final int DEBUG1 = 1;
	public static final int DEBUG2 = 2;

	public static int debugLevel = DEBUG0;

	private static final String FONT = "DEBUG_FONT";
	private static BitmapFont debugFont = null;

	public static void debug(String message) {
		Gdx.app.debug(TAG, message);
	}

	public static void error(String message) {
		if(message != null)
			Gdx.app.error(TAG, message);
	}

	public static void error(String message, Exception e) {
		if(message != null && e != null)
			Gdx.app.error(TAG, message, e);
	}

	public static void toggle() {
		if (level == Application.LOG_DEBUG)
			level = Application.LOG_ERROR;
		else
			level = Application.LOG_DEBUG;

		Gdx.app.setLogLevel(level);
	}

	public static boolean debugMode() {
		if (level == Application.LOG_DEBUG)
			return true;

		return false;
	}

	public static int getDebugLevel() {
		return debugLevel;
	}

	public static void setDebugLevel(int level) {
		debugLevel = level;
	}
	
	public static void setDebug() {
		level = Application.LOG_DEBUG;

		Gdx.app.setLogLevel(level);
	}

	public static BitmapFont getDebugFont() {
		if (debugFont == null) {
			debugFont = EngineAssetManager.getInstance().loadFont(FONT);
		}

		return debugFont;
	}
	
	public static void dispose() {
		if(debugFont != null)
			debugFont.dispose();
		debugFont = null;
	}
}

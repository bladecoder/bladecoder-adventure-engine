package org.bladecoder.engine.util;

import java.io.IOException;
import java.util.Properties;

import org.bladecoder.engine.assets.EngineAssetManager;

public class Config {
	public static final String PIE_MODE_DESKTOP_PROP="pie_mode.desktop";
	public static final String INVENTORY_POS_PROP="inventory_pos";
	public static final String TITLE_PROP="title";
	public static final String LOAD_GAMESTATE_PROP="load_gamestate";
	public static final String PLAY_RECORD_PROP="play_recording";
	public static final String TEST_SCENE_PROP="test_scene";
	public static final String FORCE_RES_PROP = "force_res";
	public static final String DEBUG_PROP = "debug";
	public static final String CHAPTER_PROP = "chapter";
	
	public static final String PROPERTIES_FILENAME = "BladeEngine.properties";
	private static Properties config = null;
	
	public static String getProperty(String key, String defaultValue) {
		if(config == null) {
			config = new Properties();
			
			try {
				config.load(EngineAssetManager.getInstance().getAsset(PROPERTIES_FILENAME).reader());
			} catch (IOException e) {
				EngineLogger.error("ERROR LOADING PROPERTIES: " + e.getMessage());
			}
		}
		
		return config.getProperty(key, defaultValue);
	}
	
	public static boolean getProperty(String key, boolean defaultValue) {
		boolean result = false;
		
		try {
			result = Boolean.parseBoolean(getProperty(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
		}
		
		return result;
	}
	
	public static int getProperty(String key, int defaultValue) {
		int result = 0;
		
		try {
			result = Integer.parseInt(getProperty(key, String.valueOf(defaultValue)));
		} catch (Exception e) {
		}
		
		return result;
	}
}

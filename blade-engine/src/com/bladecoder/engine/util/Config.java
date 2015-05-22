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
package com.bladecoder.engine.util;

import java.util.Properties;

import com.bladecoder.engine.util.EngineLogger;

import com.bladecoder.engine.assets.EngineAssetManager;

public class Config {
	public static final String PIE_MODE_DESKTOP_PROP="pie_mode.desktop";
	public static final String INVENTORY_POS_PROP="inventory_pos";
	public static final String INVENTORY_AUTOSIZE_PROP="inventory_autosize";
	public static final String TITLE_PROP="title";
	public static final String LOAD_GAMESTATE_PROP="load_gamestate";
	public static final String PLAY_RECORD_PROP="play_recording";
	public static final String TEST_SCENE_PROP="test_scene";
	public static final String FORCE_RES_PROP = "force_res";
	public static final String DEBUG_PROP = "debug";
	public static final String CHAPTER_PROP = "chapter";
	public static final String SHOW_DESC_PROP = "show_desc";
	public static final String MENU_SCREEN_CLASS_PROP = "menu_screen";
	public static final String HELP_SCREEN_CLASS_PROP = "help_screen";
	public static final String CREDIT_SCREEN_CLASS_PROP = "credit_screen";
	public static final String INIT_SCREEN_CLASS_PROP = "init_screen";
	
	public static final String PROPERTIES_FILENAME = "BladeEngine.properties";

	private static Properties config = null;
	
	public static String getProperty(String key, String defaultValue) {
		if(config == null) {
			config = new Properties();
			
			try {
				config.load(EngineAssetManager.getInstance().getAsset(PROPERTIES_FILENAME).reader());
			} catch (Exception e) {
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

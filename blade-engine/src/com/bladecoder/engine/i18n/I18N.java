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
package com.bladecoder.engine.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import com.bladecoder.engine.util.EngineLogger;

public class I18N {
	public static final char PREFIX = '@';
	
	private static ResourceBundle i18nWorld;
	private static ResourceBundle i18nChapter;
	
	public static void loadChapter(String i18nChapterFilename) {
		Locale locale = Locale.getDefault();

		try {
			i18nChapter = ResourceBundle.getBundle(i18nChapterFilename, locale,
					new I18NControl("ISO-8859-1"));
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nChapter);
		}
	}
	
	public static void loadWorld(String i18nWorldFilename) {
		Locale locale = Locale.getDefault();
		
		try {
			i18nWorld = ResourceBundle.getBundle(i18nWorldFilename, locale,
					new I18NControl("ISO-8859-1"));
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nWorld);
		}
	}
	
	public static String getString(String key) {
		try {
			return i18nChapter.getString(key);
		} catch (Exception e) {
			try {
				return i18nWorld.getString(key);
			} catch (Exception e2) {
				EngineLogger.error("MISSING TRANSLATION KEY: " + key);
				return key;
			}
		}
	}
}

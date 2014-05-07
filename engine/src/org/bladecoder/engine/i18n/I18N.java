package org.bladecoder.engine.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.I18NControl;

public class I18N {
	private static ResourceBundle i18nWorld;
	private static ResourceBundle i18nChapter;
	
	public static void load(String i18nWorldFilename, String i18nChapterFilename) {
		Locale locale = Locale.getDefault();

		try {
			i18nChapter = ResourceBundle.getBundle(i18nChapterFilename, locale,
					new I18NControl("ISO-8859-1"));
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nChapter);
		}
		
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

package com.bladecoder.engine.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

import com.bladecoder.engine.util.EngineLogger;

public class I18N {
	public static final char PREFIX = '@';
	public static final String ENCODING = "UTF-8";
	// public static final String ENCODING = "ISO-8859-1";

	private ResourceBundle i18nWorld;
	private ResourceBundle i18nChapter;
	private Locale locale = Locale.getDefault();

	private String i18nChapterFilename = null;
	private String i18nWorldFilename = null;

	public void loadChapter(String i18nChapterFilename) {
		try {
			i18nChapter = getBundle(i18nChapterFilename, false);
			this.i18nChapterFilename = i18nChapterFilename;
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nChapterFilename);
		}
	}

	public void loadWorld(String i18nWorldFilename) {
		try {
			i18nWorld = getBundle(i18nWorldFilename, true);
			this.i18nWorldFilename = i18nWorldFilename;
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nWorldFilename);
		}
	}

	public ResourceBundle getBundle(String filename, boolean clearCache) {
		ResourceBundle rb = null;

		try {
			if (clearCache)
				ResourceBundle.clearCache();

			rb = ResourceBundle.getBundle(filename, locale, new I18NControl(ENCODING));
		} catch (Exception e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + filename);
		}

		return rb;
	}

	public void setLocale(Locale l) {
		locale = l;

		// RELOAD TRANSLATIONS
		if (i18nWorld != null) {
			loadWorld(i18nWorldFilename);
		}

		if (i18nChapter != null) {
			loadChapter(i18nChapterFilename);
		}
	}

	public String getString(String key) {
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

	public Locale getCurrentLocale() {
		return locale;
	}
}

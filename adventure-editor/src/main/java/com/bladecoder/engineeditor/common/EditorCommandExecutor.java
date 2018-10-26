package com.bladecoder.engineeditor.common;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger.Levels;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.qa.QA;
import com.strongjoshua.console.CommandExecutor;

public class EditorCommandExecutor extends CommandExecutor {
	public void qa() {
		QA qa = new QA();
		qa.run(Ctx.project.getWorld());
	}

	public void checkI18NMissingKeys() {
		try {
			ModelTools.checkI18NMissingKeys();
			EditorLogger.msg("PROCCESS FINISHED: checkI18NMissingKeys.");
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			EditorLogger.printStackTrace(e);
		}
	}

	public void debug(boolean value) {
		if (!value) {
			EditorLogger.setDebugLevel(Levels.ERROR);
			console.setLoggingToSystem(false);
		} else {
			EditorLogger.setDebugLevel(Levels.DEBUG);
			console.setLoggingToSystem(true);
		}
	}

	public void extractDialogs() {
		ModelTools.extractDialogs();
		EditorLogger.msg("PROCCESS FINISHED.");
	}

	public void printUnusedSounds() {
		ModelTools.printUnusedSounds();
		EditorLogger.msg("PROCCESS FINISHED.");
	}
	
	public void checkI18N() {
		try {
			EditorLogger.msg("Check for MODEL missing keys in default translation file for current chapter.");
			ModelTools.checkI18NMissingKeys();
			
			EditorLogger.msg("Compare translation files with the base file:");
			String[] files = new File(Ctx.project.getAssetPath() + Project.MODEL_PATH).list(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {

					if (arg1.contains("_") && arg1.endsWith(".properties"))
						return true;

					return false;
				}
			});
			
			for(String f: files) {
				int idx = f.indexOf('_');
				String base = f.substring(0, idx);
				String lang = f.substring(idx + 1, idx + 3);
				EditorLogger.msg("Checking " + base + " LANG: " + lang);
				I18NUtils.compare(Ctx.project.getAssetPath() + Project.MODEL_PATH, base, null,
					lang);
			}
		} catch (Exception e) {
			EditorLogger.printStackTrace(e);
		}

		EditorLogger.msg("PROCCESS FINISHED.");
	}

	public void extractInkTexts(String story, String lang) {
		try {
			ModelTools.extractInkTexts(story, lang);
		} catch (Exception e) {
			EditorLogger.printStackTrace(e);
		}

		EditorLogger.msg("PROCCESS FINISHED.");
	}

	public void importInkTSV(String tsvFile, String storyName) {
		try {
			I18NUtils.importTSV(Ctx.project.getAssetPath() + Project.MODEL_PATH, tsvFile,
					storyName + "-ink", "default");

			EditorLogger.msg( tsvFile + " imported sucessfully.");

		} catch (IOException e) {
			EditorLogger.error( "There was a problem importing the .tsv file.");
			EditorLogger.printStackTrace(e);
		}

		EditorLogger.msg("PROCCESS FINISHED.");
	}
}

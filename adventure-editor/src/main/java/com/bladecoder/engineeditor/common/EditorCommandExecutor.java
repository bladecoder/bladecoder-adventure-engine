package com.bladecoder.engineeditor.common;

import java.io.IOException;

import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger.Levels;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.qa.QA;
import com.strongjoshua.console.CommandExecutor;

public class EditorCommandExecutor extends CommandExecutor {
	public void qa() {
		QA qa = new QA();
		qa.run(World.getInstance());
	}

	public void exit() {
		super.exitApp();
	}

	public void saveLog(String path) {
		super.printLog(path);
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
	
	public void compareI18N(String lang) {
		try {
			I18NUtils.compare(Ctx.project.getAssetPath() + Project.MODEL_PATH, Ctx.project.getChapter().getId(), null, lang);
		} catch (IOException e) {
			EditorLogger.printStackTrace(e);
		}
		
		EditorLogger.msg("PROCCESS FINISHED.");
	}
	
	public void extractInkTexts(String story) {
		try {
			ModelTools.extractInkTexts(story);
		} catch (Exception e) {
			EditorLogger.printStackTrace(e);
		}
		
		EditorLogger.msg("PROCCESS FINISHED.");
	}
}

package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engineeditor.Ctx;

public class ChapterInputPanel extends StringOptionsInputPanel {
	ChapterInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue, Ctx.project.getChapter().getChapters());
	}
}

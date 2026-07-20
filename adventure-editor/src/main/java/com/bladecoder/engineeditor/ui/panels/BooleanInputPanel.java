package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class BooleanInputPanel extends StringOptionsInputPanel {
	private static final String[] booleanValues = {"true", "false"};

	BooleanInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue, booleanValues);
	}
}

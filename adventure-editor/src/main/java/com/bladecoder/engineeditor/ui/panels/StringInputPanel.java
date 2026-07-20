package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class StringInputPanel extends InputPanel {
	TextField input;
	
	StringInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		input = new TextField("", skin);
		init(skin, title, desc, input, mandatory, defaultValue);
	}

	public String getText() {
		String text = ((TextField)field).getText();
		return text.isEmpty()?null:text;
	}

	public void setText(String s) {
		((TextField)field).setText(s);
	}
}

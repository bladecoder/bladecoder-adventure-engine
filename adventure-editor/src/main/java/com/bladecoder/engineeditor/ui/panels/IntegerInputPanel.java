package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class IntegerInputPanel extends StringInputPanel {
	
	IntegerInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		super(skin, title, desc, mandatory, defaultValue);
	}
	
	@Override
	public boolean validateField() {
		String s = getText();
		
		if(s == null || s.trim().isEmpty()) {
			if(isMandatory()) {
				setError(true);
				return false;
			} else {
				setError(false);
				return true;
			}
		}
		
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			setError(true);
			return false;
		}

		setError(false);
		return true;
	}
}

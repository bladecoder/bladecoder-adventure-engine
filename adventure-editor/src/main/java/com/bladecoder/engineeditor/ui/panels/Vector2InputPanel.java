package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.bladecoder.engine.actions.Param;

public class Vector2InputPanel extends InputPanel {
	TextField x;
	TextField y;
	Table dimPanel;
	
	Vector2InputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		dimPanel = new Table(skin);
		x = new TextField("", skin);
		y = new TextField("", skin);

		dimPanel.add(new Label(" x ", skin));
		dimPanel.add(x);
		dimPanel.add(new Label("  y ", skin));
		dimPanel.add(y);

		init(skin, title, desc, dimPanel, mandatory, defaultValue);
	}

	public String getText() {
		if (x.getText().trim().isEmpty()
				|| y.getText().trim().isEmpty())
			return "";

		return x.getText() + "," + y.getText();
	}

	public void setText(String s) {
		if (s == null || s.isEmpty()) {
			x.setText("");
			y.setText("");
		} else {
			Vector2 v = Param.parseVector2(s);
			x.setText(Float.toString(v.x));
			y.setText(Float.toString(v.y));
		}
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
			Float.parseFloat(x.getText());
			Float.parseFloat(y.getText());
		} catch (NumberFormatException e) {
			setError(true);
			return false;
		}

		setError(false);
		return true;
	}
}

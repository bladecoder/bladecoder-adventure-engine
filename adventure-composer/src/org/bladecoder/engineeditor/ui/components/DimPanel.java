package org.bladecoder.engineeditor.ui.components;

import org.bladecoder.engine.actions.Param;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class DimPanel extends Table {

	TextField width;
	TextField height;

	public DimPanel(Skin skin) {
		width = new TextField("", skin);
		height = new TextField("", skin);

		add(new Label(" width ", skin));
		add(width);
		add(new Label("  height ", skin));
		add(height);
	}

	public String getText() {
		if (width.getText().trim().isEmpty()
				|| height.getText().trim().isEmpty())
			return "";

		return width.getText() + "," + height.getText();
	}

	public void setText(String s) {
		if (s == null || s.isEmpty()) {
			width.setText("");
			height.setText("");
		} else {
			Vector2 v = Param.parseVector2(s);
			width.setText(Integer.toString((int) v.x));
			height.setText(Integer.toString((int) v.y));
		}
	}

	public boolean validateField() {
		try {
			Integer.parseInt(width.getText());
			Integer.parseInt(height.getText());
		} catch (NumberFormatException e) {
			return false;
		}

		return true;
	}
}

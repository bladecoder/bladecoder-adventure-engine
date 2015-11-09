/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.bladecoder.engine.actions.Param;

public class DimensionInputPanel extends InputPanel {
	TextField width;
	TextField height;
	Table dimPanel;
	
	DimensionInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		dimPanel = new Table(skin);
		width = new TextField("", skin);
		height = new TextField("", skin);

		dimPanel.add(new Label(" width ", skin));
		dimPanel.add(width);
		dimPanel.add(new Label("  height ", skin));
		dimPanel.add(height);

		init(skin, title, desc, dimPanel, mandatory, defaultValue);
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
			Integer.parseInt(width.getText());
			Integer.parseInt(height.getText());
		} catch (NumberFormatException e) {
			setError(true);
			return false;
		}

		setError(false);
		return true;
	}
}

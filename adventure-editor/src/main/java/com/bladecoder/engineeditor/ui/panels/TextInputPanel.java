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
package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;

public class TextInputPanel extends InputPanel {
	private TextArea input;
	private float prefRows = 10;
	
	private ScrollPane scroll;
	
	TextInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		input = new TextArea("", skin) {
			@Override
			public float getPrefHeight () {
				
				calculateOffsets();
				
				float prefHeight =  Math.max(prefRows, getLines()) * textHeight;
				
				if (getStyle().background != null) {
					prefHeight = Math.max(prefHeight + getStyle().background.getBottomHeight() + getStyle().background.getTopHeight(),
							getStyle().background.getMinHeight());
				}
				return prefHeight;
			}
			
			@Override
			public void moveCursorLine (int line) {
				super.moveCursorLine(line);
				
				scroll.setScrollPercentY((line)/(float)input.getLines());
			}
        };
        
        
		input.setPrefRows(prefRows);
		
		scroll = new ScrollPane(input, skin);
		
		scroll.setFadeScrollBars(false);
		
		init(skin, title, desc, scroll, mandatory, defaultValue);
		
		getCell(scroll).maxHeight(input.getStyle().font.getLineHeight() * prefRows + input.getStyle().background.getBottomHeight() + input.getStyle().background.getTopHeight());
	}

	public String getText() {
		return input.getText();
	}

	public void setText(String s) {
		if (s == null) s = "";
		input.setText(s.replace("\\n", "\n"));
		
		scroll.invalidate();
	}
	
	public void setRows(float rows) {
		prefRows = rows;
		input.setPrefRows(rows);
		
		getCell(scroll).maxHeight(input.getStyle().font.getLineHeight() * prefRows + input.getStyle().background.getBottomHeight() + input.getStyle().background.getTopHeight());
	}
}

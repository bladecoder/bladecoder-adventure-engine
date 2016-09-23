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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class TextInputPanel extends InputPanel {
	private TextArea input;
	private float prefRows = 10;
	
	private int oldRows;
	
	private ScrollPane scroll;
	
	TextInputPanel(Skin skin, String title, String desc, boolean mandatory, String defaultValue) {
		input = new TextArea("", skin) {
			@Override
			public float getPrefHeight () {
				
				float prefHeight =  Math.max(prefRows, getLines() + 1) * textHeight;
				
				if (getStyle().background != null) {
					prefHeight = Math.max(prefHeight + getStyle().background.getBottomHeight() + getStyle().background.getTopHeight(),
							getStyle().background.getMinHeight());
				}
				return prefHeight;
			}
			
			@Override
			public void moveCursorLine (int line) {
				super.moveCursorLine(line);
				
				scroll.setScrollPercentY((line + 1)/(float)input.getLines());
			}
        };
        
        
		input.setPrefRows(prefRows);
		
		scroll = new ScrollPane(input, skin);
		
		scroll.setFadeScrollBars(false);
		
		init(skin, title, desc, scroll, mandatory, defaultValue);
		
		input.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {						
				if(input.getLines() != oldRows) {
					scroll.layout();
					oldRows = input.getLines();
					
					int cursorLine = input.getCursorLine();
					
					scroll.setScrollPercentY((cursorLine + 1)/(float)input.getLines());
				}
				

			}
		});
	}

	public String getText() {
		return input.getText();
	}

	public void setText(String s) {
		if (s == null) s = "";
		input.setText(s.replace("\\n", "\n"));
		
		oldRows = input.getLines();
		
		scroll.layout();
	}
	
	public void setRows(float rows) {
		prefRows = rows;
		input.setPrefRows(rows);
	}
}

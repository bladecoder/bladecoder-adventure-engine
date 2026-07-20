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

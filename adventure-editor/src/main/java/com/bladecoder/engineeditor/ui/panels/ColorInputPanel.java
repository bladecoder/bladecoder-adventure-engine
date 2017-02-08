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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bladecoder.engine.actions.Param;
import com.kotcrab.vis.ui.widget.color.ColorPicker;
import com.kotcrab.vis.ui.widget.color.ColorPickerAdapter;

public class ColorInputPanel extends InputPanel {
	
	private final static String SELECT_TEXT = "Select Color";

	private ColorPicker colorPicker;
	private TextButton button;
	private Color selected;
	
	private TextButtonStyle buttonStyle;
	private TextButtonStyle oldButtonStyle;

	public ColorInputPanel(Skin skin, String title, String desc) {
		this(skin, title, desc, false, null);
	}

	public ColorInputPanel(Skin skin, String title, String desc, boolean mandatory) {
		this(skin, title, desc, mandatory, null);
	}

	public ColorInputPanel(Skin skin, String title, String desc, boolean mandatory, String current ) {
		
		Table t = new Table();
		
		oldButtonStyle = skin.get("no-toggled", TextButtonStyle.class);
		buttonStyle = new TextButtonStyle(oldButtonStyle);
		
		buttonStyle.up = skin.newDrawable("white_pixel", Color.WHITE);
		
		button = new TextButton(SELECT_TEXT, buttonStyle);
		
		t.add(button);
		
		init(skin, title, desc, t,
				mandatory, null);
		
		colorPicker = new ColorPicker(new ColorPickerAdapter() {
		    @Override
		    public void finished (Color newColor) {
		    	setText(newColor.toString());
		    }
		});

		
		button.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				if(selected != null)
					colorPicker.setColor(selected);
				
				//displaying picker with fade in animation
		        getStage().addActor(colorPicker.fadeIn());
			}
		});
		
		setText(current);
		
		// Adds clear button if not mandatory
		if(!mandatory) {
			TextButton clearButton = new TextButton("Clear", skin, "no-toggled");
			
			clearButton.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					setText(null);
				}
			});
			
			t.add(clearButton);
		}
	}

	@Override
	public String getText() {
		return selected==null?null:selected.toString();
	}

	@Override
	public void setText(String text) {
		selected = Param.parseColor(text);
		
		if(selected != null) {
			button.setText(text);
			button.setColor(selected);
			button.setStyle(buttonStyle);
		} else {
			button.setText(SELECT_TEXT);
			button.setColor(Color.WHITE);
			button.setStyle(oldButtonStyle);
		}
	}

}

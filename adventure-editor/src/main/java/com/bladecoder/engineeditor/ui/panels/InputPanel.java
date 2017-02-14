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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;

public abstract class InputPanel extends Table {
	private static final boolean USE_TOOLTIPS = true;
	
	protected Actor field;
    private Label title;
    private Label desc;
    private boolean mandatory = false;

    InputPanel() {
    }
    
    protected void init(Skin skin, String title, String desc, Actor c, boolean mandatory, String defaultValue) {
    	//debug();
    	
    	this.mandatory = mandatory;
    	
       	this.setSkin(skin);
    	LabelStyle style = new LabelStyle(skin.get(LabelStyle.class));
    	this.title = new Label(title, style);
    	
        this.desc = new Label(desc,skin, "subtitle");
        this.desc.setWrap(false);  
    	     	
       	this.field = c;
       	
//       	row().expand();
       	float titleWidth = this.title.getStyle().font.getSpaceWidth() * 35;
       	add(this.title).width(titleWidth).left().top();
       	this.title.setWidth(titleWidth);
       	this.title.setWrap(true);
       	//row().expand();
       	add(field).expandX().left().top();
       	
       	if(USE_TOOLTIPS) {
       		TextTooltip t = new TextTooltip(desc, skin);
    		this.title.addListener(t);
    		this.field.addListener(t);
       	} else {
       		row().expand();
       		add(this.desc).colspan(2).left();
       	}
    	
       	if(defaultValue != null)
    		setText(defaultValue);
    }
    
    
    public void setMandatory(boolean value) {
    	mandatory = value;
    }
    
    public boolean isMandatory() {
    	return mandatory;
    }

	public void setError(boolean value) {
    	if(value)
    		title.getStyle().fontColor = Color.RED;
    	else
    		title.getStyle().fontColor = Color.WHITE;
    }
    
	public abstract String getText();
	
	public abstract void setText(String text);
    
    public String getTitle() {
    	return title.getText().toString();
    }
    
    public Actor getField() {
    	return field;
    }
	
	public boolean validateField() {
	
		String s = getText();
		
		if(mandatory) {
			if(s == null || s.trim().isEmpty()) {
				setError(true);
				return false;
			}		
		}
		
		setError(false);	
		return true;
	}
}

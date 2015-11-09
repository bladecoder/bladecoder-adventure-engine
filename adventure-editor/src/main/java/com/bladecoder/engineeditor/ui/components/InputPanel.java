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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public abstract class InputPanel extends Table {
	
	protected Actor field;
    private Label title;
    private Label desc;
    private boolean mandatory = false;

    InputPanel() {
    }
    
    protected void init(Skin skin, String title, String desc, Actor c, boolean mandatory, String defaultValue) {
    	this.mandatory = mandatory;
    	
       	this.setSkin(skin);
    	LabelStyle style = new LabelStyle(skin.get(LabelStyle.class));
    	this.title = new Label(title, style);
    	
        this.desc = new Label(desc,skin, "subtitle");
        this.desc.setWrap(false);  
    	     	
       	this.field = c;
       	
       	add(this.title).left();
       	row().expand();
       	add(field).left();
       	row().expand();
       	add(this.desc).left();
    	
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

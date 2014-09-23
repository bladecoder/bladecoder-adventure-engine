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
package org.bladecoder.engineeditor.ui.components;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engineeditor.Ctx;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class InputPanel extends Table {
	private static final String[] booleanValues = {"true", "false"};
	private static final String[] booleanNotMandatoryValues = {"", "true", "false"};
	
    private Actor field;
    private Label title;
    private Label desc;
    private Param.Type type = Type.STRING;
    private boolean mandatory = false;

    @SuppressWarnings("unchecked")
	public InputPanel(Skin skin, String title, String desc, String[] options) {
    	init(skin, title, desc, new SelectBox<String>(skin), mandatory, null);
    	
    	if(options != null)
    		((SelectBox<String>)field).setItems(options);
    }
    
    public InputPanel(Skin skin, String title, String desc) {
    	init(skin, title, desc, new TextField("", skin), mandatory, null);
    } 
    
    public InputPanel(Skin skin, String title, String desc, boolean mandatory) {
    	init(skin, title, desc, new TextField("", skin), mandatory, null);
    }
    
    public InputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory) {
    	this(skin, title, desc, type, mandatory, null, null);
    }
    
    public InputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory, String defaultValue) {
    	this(skin, title, desc, type, mandatory, defaultValue, null);
    }
    
    @SuppressWarnings("unchecked")
	public InputPanel(Skin skin, String title, String desc, Param.Type type, boolean mandatory, String defaultValue, String[] options) {
    	this.type = type;
    	
    	if(options != null) {
    		init(skin, title, desc, new SelectBox<String>(skin), mandatory, defaultValue);
    		
        	((SelectBox<String>)field).setItems(options);
    		return;
    	}
    	
		switch(type){
		case BOOLEAN:
			init(skin, title, desc, new SelectBox<String>(skin), mandatory, defaultValue);
			((SelectBox<String>)field).setItems(mandatory?booleanValues:booleanNotMandatoryValues);
	       	if(defaultValue != null)
	    		setText(defaultValue);			
			break;
			
		case VECTOR2:
			init(skin, title, desc, new Vector2Panel(skin), mandatory, defaultValue);
			break;
			
		case DIMENSION:
			init(skin, title, desc, new DimPanel(skin), mandatory, defaultValue);
			break;
			
		case ACTOR:
		{
			NodeList actors = Ctx.project.getSelectedChapter().getActors(Ctx.project.getSelectedScene());
			int l = actors.getLength();
			if(!mandatory) l++;
			String values[] = new String[l];
			
			if(!mandatory) {
				values[0] = "";
			}
			
			for(int i = 0; i < actors.getLength(); i++) {
				if(mandatory)
					values[i] = ((Element)actors.item(i)).getAttribute("id");
				else
					values[i+1] = ((Element)actors.item(i)).getAttribute("id");
			}
			
			init(skin, title, desc, new SelectBox<String>(skin), mandatory, defaultValue);
			((SelectBox<String>)field).setItems(values);
	       	if(defaultValue != null)
	    		setText(defaultValue);
		}
			break;
			
		case SCENE:
		{
			NodeList scenes = Ctx.project.getSelectedChapter().getScenes();
			int l = scenes.getLength();
			if(!mandatory) l++;
			String values[] = new String[l];
			
			if(!mandatory) {
				values[0] = "";
			}
			
			for(int i = 0; i < scenes.getLength(); i++) {
				if(mandatory)
					values[i] = ((Element)scenes.item(i)).getAttribute("id");
				else
					values[i+1] = ((Element)scenes.item(i)).getAttribute("id");
			}
			
			init(skin, title, desc, new SelectBox<String>(skin), mandatory, defaultValue);
			((SelectBox<String>)field).setItems(values);
	       	if(defaultValue != null)
	    		setText(defaultValue);
		}
			break;			
			
		case CHAPTER:
		{
			String[] chapters = Ctx.project.getWorld().getChapters();
			int l = chapters.length;
			if(!mandatory) l++;
			String values[] = new String[l];
			
			if(!mandatory) {
				values[0] = "";
			}
			
			for(int i = 0; i < chapters.length; i++) {
				if(mandatory)
					values[i] = chapters[i];
				else
					values[i+1] = chapters[i];
			}
			
			init(skin, title, desc, new SelectBox<String>(skin), mandatory, defaultValue);
			((SelectBox<String>)field).setItems(values);
	       	if(defaultValue != null)
	    		setText(defaultValue);	
		}
			break;			
			
		default:
			init(skin, title, desc, new TextField("", skin), mandatory, defaultValue);
			break;
		
		}
	}     
    
    public InputPanel(Skin skin, String title, String desc, Actor c, String defaultValue) {
    	init(skin, title, desc, c, false, defaultValue);
    }
    
    private void init(Skin skin, String title, String desc, Actor c, boolean mandatory, String defaultValue) {
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

	public void setError(boolean value) {
    	if(value)
    		title.getStyle().fontColor = Color.RED;
    	else
    		title.getStyle().fontColor = Color.WHITE;
    }
    
    @SuppressWarnings("unchecked")
	public String getText() {
    	
    	if(field instanceof TextField)
    		return ((TextField)field).getText();
    	else if(field instanceof SelectBox)
    		return (String)((SelectBox<String>)field).getSelected();
    	else if(field instanceof Vector2Panel)
    		return ((Vector2Panel)field).getText();
    	else if(field instanceof DimPanel)
    		return ((DimPanel)field).getText();
    	
    	return null;
    }
    
    public String getTitle() {
    	return title.getText().toString();
    }
    
    @SuppressWarnings("unchecked")
	public int getSelectedIndex() {
    	if(field instanceof SelectBox)
    		return ((SelectBox<String>)field).getSelectedIndex();
    	
    	return 0;
    }
    
    public Actor getField() {
    	return field;
    }

	@SuppressWarnings("unchecked")
	public void setText(String text) {
		if(field instanceof TextField)
    		((TextField)field).setText(text);
		else if(field instanceof SelectBox) {
			int idx = ((SelectBox<String>)field).getItems().indexOf(text, false);
			if(idx != -1)
				((SelectBox<String>)field).setSelectedIndex(idx);
		} else if(field instanceof Vector2Panel)
    		((Vector2Panel)field).setText(text);
		else if(field instanceof DimPanel)
    		((DimPanel)field).setText(text);
		else if(field instanceof TextButton)
			((TextButton)field).setText(text);
	}
	
	public boolean validateField() {
	
		String s = getText();
		
		if(mandatory) {
			if(s == null || s.trim().isEmpty()) {
				setError(true);
				return false;
			}		
		}
		
		if(field instanceof SelectBox<?>) return true;
		
		if(!mandatory && (s==null || s.trim().isEmpty())) {
			setError(false);	
			return true;
		}
		
		switch(type){
		case FLOAT:			
			try {
				Float.parseFloat(s);
			} catch (NumberFormatException e) {
				setError(true);
				return false;
			}
			break;
		case INTEGER:
			try {
				Integer.parseInt(s);
			} catch (NumberFormatException e) {
				setError(true);
				return false;
			}				
			break;
		case VECTOR2:
		
			if(!((Vector2Panel)field).validateField()) {
				setError(true);
				return false;
			}
			break;
		case DIMENSION:
			
			if(!((DimPanel)field).validateField()) {
				setError(true);
				return false;
			}			
				
			break;
		default:
			break;
		
		}		
		
		setError(false);	
		return true;
	}
}

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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener;

public class PropertyTable extends Container<Table> {
	private static final String[] BOOLEAN_VALUES = {"", "true", "false"};

	Skin skin;
	Table table;

	public enum Types {
		INTEGER, BOOLEAN, FLOAT, STRING
	}

	public PropertyTable(Skin skin) {
//		super(skin);
		table = new Table(skin);
		this.skin = skin;
		top().left();
		table.top().left();
		
		table.add(new Label("Name", skin));
		table.add(new Label("Value", skin));
		table.setFillParent(true);
		
		fill();
		prefHeight(1000);
		
		setActor(table);
	}

	public void addProperty(String name, String value, Types type) {
		
		table.row();
		table.add(new Label(name, skin)).expandX().left();
		
		if(type == Types.BOOLEAN) {
			SelectBox<String> sb= new SelectBox<String>(skin);
			sb.setItems(BOOLEAN_VALUES);
			if(value!=null)
				sb.setSelected(value);
			sb.setName(name);
			table.add(sb).expandX().left();
			
			sb.addListener(new ChangeListener() {
				
				@SuppressWarnings("unchecked")
				@Override
				public void changed(
						ChangeEvent event,	Actor actor) {
					updateModel(actor.getName(), ((SelectBox<String>)actor).getSelected());
					
				}
			});
		} else {
			TextField tf = new TextField( value==null?"":value, skin);
			tf.setName(name);
			table.add(tf).expandX().left();
			
			if(type == Types.INTEGER)
				tf.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
			
			tf.setTextFieldListener(new TextFieldListener() {
				@Override
				public void keyTyped(TextField actor, char c) {
					updateModel(actor.getName(), ((TextField)actor).getText());
				}
			});			
			
			
//			tf.addListener(new FocusListener() {
//				
//				@Override
//				public void keyboardFocusChanged (FocusEvent event, Actor actor, boolean focused) {
//					if(!focused)
//						updateModel(actor.getName(), ((TextField)actor).getText());
//				}
//			});
		}
	}

	public void addProperty(String name, int value) {
		addProperty(name, Integer.toString(value), Types.INTEGER);
	}

	public void addProperty(String name, float value) {
		addProperty(name, Float.toString(value), Types.FLOAT);
	}

	public void addProperty(String name, String value) {
		addProperty(name, value, Types.STRING);
	}

	public void addProperty(String name, boolean value) {
		addProperty(name, Boolean.toString(value), Types.BOOLEAN);
	}
	
	protected void updateModel(String property, String value) {
		
	}


	protected void clearProps() {
		table.clear();
	}
}

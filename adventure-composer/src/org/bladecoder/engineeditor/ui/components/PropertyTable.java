package org.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;

public class PropertyTable extends Table {
	private static final String[] BOOLEAN_VALUES = {"", "true", "false"};

	Skin skin;

	public enum Types {
		INTEGER, BOOLEAN, FLOAT, STRING
	}

	public PropertyTable(Skin skin) {
		super(skin);
		this.skin = skin;
		top().left();
		
		add(new Label("Name", skin));
		add(new Label("Value", skin));
	}

	public void addProperty(String name, String value, Types type) {
		
		row();
		add(new Label(name, skin)).expandX().left();
		
		if(type == Types.BOOLEAN) {
			SelectBox<String> sb= new SelectBox<String>(skin);
			sb.setItems(BOOLEAN_VALUES);
			sb.setSelected(value);
			sb.setName(name);
			add(sb).expandX().left();
			
			sb.addListener(new ChangeListener() {
				
				@SuppressWarnings("unchecked")
				@Override
				public void changed(
						com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent event,
						Actor actor) {
					updateModel(actor.getName(), ((SelectBox<String>)actor).getSelected());
					
				}
			});
		} else {
			TextField tf = new TextField( value, skin);
			tf.setName(name);
			add(tf).expandX().left();
			
			tf.addListener(new FocusListener() {
				
				@Override
				public void keyboardFocusChanged (FocusEvent event, Actor actor, boolean focused) {
					if(!focused)
						updateModel(actor.getName(), ((TextField)actor).getText());
				}
			});
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

}

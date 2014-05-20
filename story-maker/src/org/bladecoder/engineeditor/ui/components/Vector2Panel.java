package org.bladecoder.engineeditor.ui.components;

import org.bladecoder.engine.actions.Param;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class Vector2Panel extends Table {
	
	TextField x;
	TextField y;
	
	public Vector2Panel(Skin skin) {
		x = new TextField("", skin);
		y = new TextField("", skin);
		
    	add(new Label(" x ", skin));
    	add(x);
    	add(new Label(" y ", skin));
    	add(y);
	}
	
	public String getText() {
		
		if(x.getText().isEmpty() && y.getText().isEmpty())
			return "";
		
		return x.getText() + "," + y.getText();
	}
	
	public void setText(String s) {
		Vector2 v = Param.parseVector2(s);
		
		if(v != null) {
			x.setText(Float.toString(v.x));
			y.setText(Float.toString(v.y));
		} else {
			x.setText("");
			y.setText("");
		}
	}

	public boolean validateField() {
		try {
			Float.parseFloat(x.getText());
			Float.parseFloat(y.getText());
		} catch (NumberFormatException e) {
			return false;
		}
		
		return true;
	}
}

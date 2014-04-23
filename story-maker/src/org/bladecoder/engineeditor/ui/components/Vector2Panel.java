package org.bladecoder.engineeditor.ui.components;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.bladecoder.engine.actions.Param;

import com.badlogic.gdx.math.Vector2;

@SuppressWarnings("serial")
public class Vector2Panel extends JPanel {
	
	JTextField x = new JTextField();
	JTextField y = new JTextField();
	
	public Vector2Panel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    	setOpaque(false);
    	
    	add(new JLabel(" x "));
    	add(x);
    	add(new JLabel(" y "));
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

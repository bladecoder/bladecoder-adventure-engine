package org.bladecoder.engineeditor.ui.components;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.bladecoder.engine.actions.Param;

import com.badlogic.gdx.math.Vector2;

@SuppressWarnings("serial")
public class DimPanel extends JPanel {
	
	JTextField width = new JTextField();
	JTextField height = new JTextField();
	
	public DimPanel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    	setOpaque(false);
    	
    	add(new JLabel(" width "));
    	add(width);
    	add(new JLabel("  height "));
    	add(height);
	}
	
	public String getText() {
		if(width.getText().trim().isEmpty() || height.getText().trim().isEmpty())
			return "";
		
		return width.getText() + "," + height.getText();
	}
	
	public void setText(String s) {
		Vector2 v = Param.parseVector2(s);
		width.setText(Integer.toString((int)v.x));
		height.setText(Integer.toString((int)v.y));
	}

	public boolean validateField() {
		try {
			Integer.parseInt(width.getText());
			Integer.parseInt(height.getText());
		} catch (NumberFormatException e) {
			return false;
		}
		
		return true;
	}
}

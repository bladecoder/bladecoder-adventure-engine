package org.bladecoder.engineeditor.ui.components;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.bladecoder.engine.actions.Param;
import org.bladecoder.engine.actions.Param.Type;

@SuppressWarnings("serial")
public class InputPanel extends JPanel {
	private static final String[] booleanValues = {"true", "false"};
	private static final String[] booleanNotMandatoryValues = {"", "true", "false"};
	
    private JComponent field;
    private JLabel title;
    private JLabel desc;
    private Param.Type type = Type.STRING;
    private boolean mandatory = false;

    public InputPanel(String title, String desc, String[] options) {
    	init(title, desc, new JComboBox<String>(options), mandatory, null);
    }
    
    public InputPanel(String title, String desc) {
    	init(title, desc, new JTextField(), mandatory, null);
    } 
    
    public InputPanel(String title, String desc, boolean mandatory) {
    	init(title, desc, new JTextField(), mandatory, null);
    }
    
    public InputPanel(String title, String desc, Param.Type type, boolean mandatory) {
    	this(title, desc, type, mandatory, null, null);
    }
    
    public InputPanel(String title, String desc, Param.Type type, boolean mandatory, String defaultValue, String[] options) {
    	this.type = type;
    	
    	if(options != null) {
    		init(title, desc, new JComboBox<String>(options), mandatory, defaultValue);
    		return;
    	}
    	
		switch(type){
		case BOOLEAN:
			init(title, desc, new JComboBox<String>(mandatory?booleanValues:booleanNotMandatoryValues), mandatory, defaultValue);
			break;
			
		case VECTOR2:
			init(title, desc, new Vector2Panel(), mandatory, defaultValue);
			break;
			
		case DIMENSION:
			init(title, desc, new DimPanel(), mandatory, defaultValue);
			break;			

		default:
			init(title, desc, new JTextField(), mandatory, defaultValue);
			break;
		
		}
	}     
    
    public InputPanel(String title, String desc, JComponent c, String defaultValue) {
    	init(title, desc, c, false, defaultValue);
    }
    
    private void init(String title, String desc, JComponent c, boolean mandatory, String defaultValue) {
    	this.mandatory = mandatory;
    	
    	if(desc.startsWith("<html")) {
        	this.desc = new JLabel(desc);
    	} else {
    		this.desc = new JLabel("<html>" + desc + "</html>");
    	}
    	
    	
       	this.title = new JLabel(title);
    	field = c;
    	
    	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    	setOpaque(false);
    	setBorder(new EmptyBorder(10, 10, 10, 10));
    	this.title.setAlignmentX(LEFT_ALIGNMENT);
    	this.desc.setAlignmentX(LEFT_ALIGNMENT);
    	this.field.setAlignmentX(LEFT_ALIGNMENT);
    	
    	this.title.setFont(Theme.BOLD_FONT);
    	this.title.setForeground(Theme.FG_LABEL);
    	this.desc.setFont(Theme.FONT);
    	this.desc.setForeground(Theme.FG_LABEL);
    	
    	add(this.title);
    	add(Box.createRigidArea(new Dimension(0,5)));
    	add(field);
    	add(Box.createRigidArea(new Dimension(0,5)));
    	add(this.desc);
    	
    	field.setMaximumSize( new Dimension(200, field.getPreferredSize().height ));  
    	
    	setPreferredSize(new Dimension(250, (int)getPreferredSize().getHeight()));
    	
    	if(defaultValue != null)
    		setText(defaultValue);
    }
    
    
    public void setMandatory(boolean value) {
    	mandatory = value;
    }

	public void setError(boolean value) {
    	if(value)
    		desc.setForeground(Color.RED);
    	else
    		desc.setForeground(Theme.FG_LABEL);
    }
    
    @SuppressWarnings("unchecked")
	public String getText() {
    	
    	if(field instanceof JTextField)
    		return ((JTextField)field).getText();
    	else if(field instanceof JComboBox)
    		return (String)((JComboBox<String>)field).getSelectedItem();
    	else if(field instanceof Vector2Panel)
    		return ((Vector2Panel)field).getText();
    	else if(field instanceof DimPanel)
    		return ((DimPanel)field).getText();
    	
    	return null;
    }
    
    public String getTitle() {
    	return title.getText();
    }
    
    @SuppressWarnings("unchecked")
	public int getSelectedIndex() {
    	if(field instanceof JComboBox)
    		return ((JComboBox<String>)field).getSelectedIndex();
    	
    	return 0;
    }
    
    public JComponent getField() {
    	return field;
    }

	@SuppressWarnings("unchecked")
	public void setText(String text) {
		if(field instanceof JTextField)
    		((JTextField)field).setText(text);
		else if(field instanceof JComboBox) 
			((JComboBox<String>)field).setSelectedItem(text);
		else if(field instanceof Vector2Panel)
    		((Vector2Panel)field).setText(text);
		else if(field instanceof DimPanel)
    		((DimPanel)field).setText(text);
	}
	
	public boolean validateField() {
	
		String s = getText();
		
		if(mandatory) {
			if(s == null || s.trim().isEmpty()) {
				setError(true);
				return false;
			}		
		}
		
		if(field instanceof JComboBox) return true;
		
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
	
	@Override
	public void requestFocus() {
		field.requestFocus();
	}
}

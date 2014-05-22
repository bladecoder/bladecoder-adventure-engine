package org.bladecoder.engineeditor.ui.components;

import java.awt.Component;
import java.io.IOException;
import java.net.URL;

import javax.swing.ImageIcon;

import org.bladecoder.engineeditor.utils.ImageUtils;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public abstract class EditDialog extends Dialog {
    
    private Label infoLbl;
    
    private Table centerPanel;
    
    boolean cancelled = false;
    
    private Skin skin;

    public EditDialog(String title, Skin skin) {
        super(title, skin);
        
    	this.skin = skin;
        
        setResizable(false);
         
        infoLbl = new Label("", skin);
        infoLbl.setWrap(true);

        centerPanel = new Table(skin);
        getContentTable().add(infoLbl).width(200);
        getContentTable().add(new ScrollPane(centerPanel));
		
		button("OK", true);
		button("Cancel", false);
		key(Keys.ENTER, true);
		key(Keys.ESCAPE, false);
		
		padBottom(10);
		padLeft(10);
		padRight(10);
    }
    
    public Skin getSkin() {
    	return skin;
    }
    
    public Table getCenterPanel() {
    	return centerPanel;
    }
    
    public void setInfo(String text) {
        infoLbl.setText(text);
    }
    
    public void setInfoIcon(URL u) {
    	ImageIcon icon = null;
    	
    	try {
			icon = ImageUtils.getImageIcon(u, 300);
		} catch (IOException e) {
		}
    	
//    	if(icon != null)
//    		infoLbl.setIcon(icon);
    }
    
    public void setInfoComponent(Component c) {
//    	headerPanel.remove(infoLbl);
//    	headerPanel.add(c, java.awt.BorderLayout.WEST);
//    	c.setPreferredSize(new Dimension(300,(int)getContentPane().getPreferredSize().getHeight()));
    }
    
    public void setTitle(String title) {
        super.setTitle(title);
    }
    
    public boolean isCancel() {
    	return cancelled;
    }
    
	protected void result (Object object) {
		if(((boolean)object) == true) {
			if(validateFields()) {
				ok();
			} else {
				cancel();
			}
		} else {
			cancelled = true;
		}
	}
    
    abstract protected boolean validateFields();

	abstract protected void ok();
}

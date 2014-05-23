package org.bladecoder.engineeditor.ui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.esotericsoftware.tablelayout.Cell;

public abstract class EditDialog extends Dialog {
    
	private Cell<Widget> infoCell;
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
        infoCell = getContentTable().add((Widget)infoLbl).prefWidth(200);
        getContentTable().add(new ScrollPane(centerPanel, skin)).maxHeight(Gdx.graphics.getHeight() * 0.8f).maxWidth(Gdx.graphics.getWidth() * 0.6f);
		
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
    
    
    public void setInfoWidget(Widget c) {
    	infoCell.setWidget(null);
    	infoCell.setWidget(c);
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

package org.bladecoder.engineeditor.ui.components;

import org.bladecoder.engineeditor.glcanvas.Assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class EditToolbar extends Table {
	
    private ImageButton createBtn;
    private ImageButton deleteBtn;
    private ImageButton editBtn;
    private ImageButton copyBtn;	
    private ImageButton pasteBtn;	
    
    private Skin skin;
	
	public EditToolbar(Skin skin) {
		super();
		
		this.skin = skin;
		this.left();
		
        createBtn = new ImageButton(skin);
        editBtn = new ImageButton(skin);
        deleteBtn = new ImageButton(skin);
        copyBtn = new ImageButton(skin);
        pasteBtn = new ImageButton(skin);
		
        addToolBarButton(createBtn, "res/images/ic_add.png","New", "Create a new Element");
        addToolBarButton(editBtn, "res/images/ic_edit.png","Edit", "Edit the selected Element");
        addToolBarButton(deleteBtn, "res/images/ic_delete.png","Delete", "Delete and put in the clipboard"); 
        addToolBarButton(copyBtn, "res/images/ic_copy.png","Copy", "Copy to the clipboard");
        addToolBarButton(pasteBtn, "res/images/ic_paste.png","Paste", "Paste from the clipboard");
    }
	
	public void hideCopyPaste() {
		copyBtn.setVisible(false);
		pasteBtn.setVisible(false);
	}
    
	public void disableCreate(boolean v) {
		createBtn.setDisabled(v);
	}
	
	public void disableEdit(boolean v) {
		deleteBtn.setDisabled(v);
		editBtn.setDisabled(v);
		copyBtn.setDisabled(v);
	}
	
	public void disablePaste(boolean v) {
		pasteBtn.setDisabled(v);
	}
	
	public void addToolBarButton(ImageButton button, String icon, String text, String tooltip) {
		
		Texture image = Assets.inst().get(icon, Texture.class);
		Texture imageDisabled = Assets.inst().get(icon.substring(0,icon.indexOf(".")) + "_disabled.png", Texture.class);
		
		ImageButtonStyle style = new ImageButtonStyle(skin.get(ButtonStyle.class));
		style.imageUp = new TextureRegionDrawable(new TextureRegion(image));
		style.imageDisabled = new TextureRegionDrawable(new TextureRegion(imageDisabled));
		button.setStyle(style);
				
        add(button);
        button.setDisabled(true);
	}
	
	
	public void addCreateListener(EventListener e) {
		createBtn.addListener(e);
	}	
	
	public void addEditListener(EventListener e) {
		editBtn.addListener(e);
	}
	
	public void addDeleteListener(EventListener e) {
		deleteBtn.addListener(e);
	}

	public void addCopyListener(EventListener e) {
		copyBtn.addListener(e);
	}
	
	public void addPasteListener(EventListener e) {
		pasteBtn.addListener(e);
	}
}

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
package com.bladecoder.engineeditor.ui.panels;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.bladecoder.engineeditor.Ctx;

public class EditToolbar extends HorizontalGroup {
	
    private ImageButton createBtn;
    private ImageButton deleteBtn;
    private ImageButton editBtn;
    private ImageButton copyBtn;	
    private ImageButton pasteBtn;	
    
    private Skin skin;
	
	public EditToolbar(Skin skin) {
		super();
		
		this.skin = skin;
		pad(0);
		
        createBtn = new ImageButton(skin);
        editBtn = new ImageButton(skin);
        deleteBtn = new ImageButton(skin);
        copyBtn = new ImageButton(skin);
        pasteBtn = new ImageButton(skin);
		
        addToolBarButton(createBtn, "ic_add","New", "Create a new Element");
        addToolBarButton(editBtn, "ic_edit","Edit", "Edit the selected Element");
        addToolBarButton(deleteBtn, "ic_delete","Delete", "Delete and put in the clipboard"); 
        addToolBarButton(copyBtn, "ic_copy","Copy", "Copy to the clipboard");
        addToolBarButton(pasteBtn, "ic_paste","Paste", "Paste from the clipboard");
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
		
		TextureRegion image = Ctx.assetManager.getIcon(icon);
		TextureRegion imageDisabled = Ctx.assetManager.getIcon(icon + "_disabled");
		
		ImageButtonStyle style = new ImageButtonStyle(skin.get("plain", ButtonStyle.class));
		style.imageUp = new TextureRegionDrawable(image);
		
		if(imageDisabled != null)
			style.imageDisabled = new TextureRegionDrawable(imageDisabled);
		button.setStyle(style);
		button.pad(6,3,6,3);
        addActor(button);
        button.setDisabled(true);
        TextTooltip t = new TextTooltip(tooltip, skin);
		button.addListener(t);
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

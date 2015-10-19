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
package com.bladecoder.engineeditor.ui;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditElementDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;

public class DialogList extends ModelList<Dialog> {	
	
    private DialogOptionList options;

	@Override
	protected EditElementDialog getEditElementDialogInstance(Dialog e) {
//		return new EditDialogDialog(skin, doc, parent, e);
		
		return null;
	}
	
    public DialogList(Skin skin) {
    	super(skin, true);
    	
    	options = new DialogOptionList(skin);
    	row();
    	add(options).expand().fill();

		list.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();
				
				addOptions();
				
				toolbar.disableEdit(pos == -1);
			}
		});
		
		list.setCellRenderer(listCellRenderer);
		listCellRenderer.layout(list.getStyle());
		container.minHeight(listCellRenderer.getItemHeight() * 5);
		container.maxHeight(listCellRenderer.getItemHeight() * 5);
    }
    
    public void addOptions() {
		int pos = list.getSelectedIndex();

		Dialog d = null;

		if (pos != -1) {
			d = list.getItems().get(pos);
			options.addElements(d);
		} else { 
			options.addElements((Dialog)null);
		}    	
    }
    
    
	@Override
	public void addElements(List<Dialog> elements) {
		super.addElements(elements);
		addOptions();
    }	


	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<Dialog> listCellRenderer = new CellRenderer<Dialog>() {

		@Override
		protected String getCellTitle(Dialog e) {
			return e.getId();
		}

		@Override
		public TextureRegion getCellImage(Dialog e) {
			return Ctx.assetManager.getIcon("ic_talkto");
		}
		
		@Override
		protected boolean hasSubtitle() {
			return false;
		}
		
		@Override
		protected boolean hasImage() {
			return true;
		}
	};

}

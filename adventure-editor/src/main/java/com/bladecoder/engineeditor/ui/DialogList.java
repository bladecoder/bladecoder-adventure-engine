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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteDialog;

public class DialogList extends ModelList<CharacterActor, Dialog> {	
	
    private OptionList options;

	@Override
	protected EditModelDialog<CharacterActor, Dialog> getEditElementDialogInstance(Dialog e) {
		return new EditDialogDialog(skin, parent, e);
	}
	
    public DialogList(Skin skin) {
    	super(skin, true);
    	
    	options = new OptionList(skin);
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
		
		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ELEMENT_CREATED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() instanceof Dialog && !(evt.getSource() instanceof EditDialogDialog) && parent instanceof CharacterActor) {
					addElements(parent, Arrays.asList(parent.getDialogs().values().toArray(new Dialog[0])));
				}
			}
		});
    }
    
    public void addOptions() {
		int pos = list.getSelectedIndex();

		Dialog d = null;

		if (pos != -1) {
			d = list.getItems().get(pos);
			options.addElements(d, d.getOptions());
		} else { 
			options.addElements(null, null);
		}    	
    }
    
    
	@Override
	public void addElements(CharacterActor a, List<Dialog> elements) {
		super.addElements(a, elements);
		addOptions();
    }	
	
	@Override
	protected void delete() {
			
		Dialog d = removeSelected();
			
		parent.getDialogs().remove(d.getId());
		
		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(d);
			
		// UNDO
		Ctx.project.getUndoStack().add(new UndoDeleteDialog(parent, d));

		// Clear options here because change event doesn't call when deleting
		// the last element
		if (list.getSelectedIndex() == -1)
			addOptions();
		
		Ctx.project.setModified();
	}
	
	@Override
	protected void copy() {
		Dialog e = list.getSelected();

		if (e == null)
			return;

		clipboard = (Dialog)ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(clipboard);
	}

	@Override
	protected void paste() {
		Dialog newElement = (Dialog)ElementUtils.cloneElement(clipboard);
		
		newElement.setId(ElementUtils.getCheckedId(newElement.getId(), parent.getDialogs().keySet().toArray(new String[0])));
		
		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		parent.addDialog(newElement);
		Ctx.project.getI18N().extractStrings(Ctx.project.getSelectedScene().getId(),parent.getId(), newElement);

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();
		
		Ctx.project.setModified();
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

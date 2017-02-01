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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteOption;

public class OptionList extends ModelList<Dialog, DialogOption> {
	Skin skin;

	private ImageButton upBtn;
	private ImageButton downBtn;

	public OptionList(Skin skin) {
		super(skin, false);
		this.skin = skin;

		setCellRenderer(listCellRenderer);

		upBtn = new ImageButton(skin);
		downBtn = new ImageButton(skin);

		toolbar.addToolBarButton(upBtn, "ic_up", "Move up", "Move up");
		toolbar.addToolBarButton(downBtn, "ic_down", "Move down", "Move down");
		toolbar.pack();

		list.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				toolbar.disableEdit(pos == -1);
				upBtn.setDisabled(pos == -1 || pos == 0);
				downBtn.setDisabled(pos == -1 || pos == list.getItems().size - 1);
			}
		});

		upBtn.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				up();
			}
		});

		downBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				down();
			}
		});

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_ELEMENT_CREATED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() instanceof DialogOption && !(evt.getSource() instanceof EditOptionDialog)) {
					addElements(parent, parent.getOptions());
				}
			}
		});
	}

	@Override
	protected EditModelDialog<Dialog, DialogOption> getEditElementDialogInstance(DialogOption e) {
		return new EditOptionDialog(skin, parent, e, list.getSelectedIndex());
	}

	@Override
	protected void create() {
		EditOptionDialog dialog = (EditOptionDialog) getEditElementDialogInstance(null);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex() + 1;

				DialogOption e = ((EditOptionDialog) actor).getElement();
				list.getItems().insert(pos, e);
				list.setSelectedIndex(pos);
				list.invalidateHierarchy();
			}
		});
	}

	private void up() {
		int pos = list.getSelectedIndex();

		if (pos == -1 || pos == 0)
			return;

		Array<DialogOption> items = list.getItems();
		DialogOption e = items.get(pos);
		DialogOption e2 = items.get(pos - 1);

		items.set(pos - 1, e);
		items.set(pos, e2);

		parent.getOptions().set(pos - 1, e);
		parent.getOptions().set(pos, e2);

		list.setSelectedIndex(pos - 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		Ctx.project.setModified();
	}

	private void down() {
		int pos = list.getSelectedIndex();
		Array<DialogOption> items = list.getItems();

		if (pos == -1 || pos == items.size - 1)
			return;

		DialogOption e = items.get(pos);
		DialogOption e2 = items.get(pos + 1);

		parent.getOptions().set(pos + 1, e);
		parent.getOptions().set(pos, e2);

		items.set(pos + 1, e);
		items.set(pos, e2);
		list.setSelectedIndex(pos + 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		Ctx.project.setModified();
	}

	@Override
	protected void delete() {

		DialogOption option = removeSelected();

		int idx = parent.getOptions().indexOf(option);

		parent.getOptions().remove(option);

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(option);

		// UNDO
		Ctx.project.getUndoStack().add(new UndoDeleteOption(parent, option, idx));

		Ctx.project.setModified();
	}

	@Override
	protected void copy() {
		DialogOption e = list.getSelected();

		if (e == null)
			return;

		clipboard = (DialogOption) ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(clipboard);
	}

	@Override
	protected void paste() {
		DialogOption newElement = (DialogOption) ElementUtils.cloneElement(clipboard);

		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		parent.addOption(newElement);

		Ctx.project.getI18N().extractStrings(Ctx.project.getSelectedScene().getId(),
				Ctx.project.getSelectedActor().getId(), parent.getId(), pos, newElement);

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();

		Ctx.project.setModified();
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<DialogOption> listCellRenderer = new CellRenderer<DialogOption>() {

		@Override
		protected String getCellTitle(DialogOption e) {
			String text = e.getText();

			int i = parent.getOptions().indexOf(e);

			String str = i + ". " + Ctx.project.translate(text);
			
			if(e.isOnce())
				str = "[[ONCE]" + str;
			
			if(!e.isVisible())
				str = "[GRAY]" + str + "[]";
			
			return str;
		}

		@Override
		protected String getCellSubTitle(DialogOption e) {

			StringBuilder sb = new StringBuilder();
			String response = e.getResponseText();

			if (response != null && !response.isEmpty())
				sb.append("R: ").append(Ctx.project.translate(response).replace("\n", "|")).append(' ');

			if (e.getVerbId() != null)
				sb.append(" verb: ").append(e.getVerbId());

			if (e.getNext() != null)
				sb.append(" next: ").append(e.getNext());

			return sb.toString();
		}

		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};

}

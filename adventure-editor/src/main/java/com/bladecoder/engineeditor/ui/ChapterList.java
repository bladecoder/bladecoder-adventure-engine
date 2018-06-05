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

import java.util.Comparator;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditList;

public class ChapterList extends EditList<String> {

	private ImageButton initBtn;

	public ChapterList(Skin skin) {
		super(skin);

		list.setCellRenderer(listCellRenderer);

		initBtn = new ImageButton(skin);
		toolbar.addToolBarButton(initBtn, "ic_check", "Set init chapter",
				"Set init chapter");

		initBtn.setDisabled(false);
		toolbar.hideCopyPaste();

		initBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setDefault();
			}
		});

	}

	private void setDefault() {
		String e = list.getSelected();

		if (e == null)
			return;
		
		Ctx.project.getWorld().setInitChapter(e);
		Ctx.project.setModified();
	}

	@Override
	protected void delete() {

		int pos = list.getSelectedIndex();

		if (pos == -1)
			return;

		if (list.getItems().size < 2) {
			String msg = "The chapter will not be deleted, at least one chapter must exists";
			Message.showMsg(getStage(), msg, 4);

			return;
		}

		String e = list.getItems().removeIndex(pos);

		if (e.equals(Ctx.project.getChapter().getInitChapter())) {
			Ctx.project.getWorld().setInitChapter(list.getItems().get(0));
			Ctx.project.setModified();
		}

		try {
			Ctx.project.getChapter().deleteChapter(e);
		} catch (Exception ex) {
			String msg = "Something went wrong.\n\n"
					+ ex.getClass().getSimpleName() + " - " + ex.getMessage();
			Message.showMsgDialog(getStage(), "Error deleting chapter", msg);

			EditorLogger.printStackTrace(ex);
		}
		
		list.setSelectedIndex(0);
		
		Ctx.project.notifyPropertyChange(Project.CHAPTER_PROPERTY);
	}

	@Override
	protected void create() {
		EditChapterDialog dialog = new EditChapterDialog(skin, Ctx.project.getChapter(), null);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String e = ((EditChapterDialog) actor).getNewId();
				addItem(e);
				int i = getItems().indexOf(e, true);
				if (i != -1)
					list.setSelectedIndex(i);

				list.invalidateHierarchy();
				
				Ctx.project.notifyPropertyChange(Project.CHAPTER_PROPERTY);
			}
		});
	}

	@Override
	protected void edit() {

		String e = list.getSelected();

		if (e == null)
			return;

		EditChapterDialog dialog = new EditChapterDialog(skin, Ctx.project.getChapter(), e);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				String e = ((EditChapterDialog) actor).getNewId();
				list.getItems().removeIndex(list.getSelectedIndex());
				list.getItems().add(e);
				list.setSelectedIndex(list.getItems().indexOf(e, true));
				Ctx.project.notifyPropertyChange(Project.CHAPTER_PROPERTY);
			}
		});
	}

	@Override
	protected void copy() {
	}

	@Override
	protected void paste() {
	}
	

	public void addElements() {

		list.getItems().clear();
		list.getSelection().clear();
		toolbar.disableCreate(false);

		String nl[] = Ctx.project.getChapter().getChapters();

		for (int i = 0; i < nl.length; i++) {
			addItem(nl[i]);
		}

		if (getItems().size > 0)
			list.setSelectedIndex(0);

		toolbar.disableEdit(list.getSelectedIndex() < 0);

		list.getItems().sort(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});

		invalidateHierarchy();
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<String> listCellRenderer = new CellRenderer<String>() {

		@Override
		protected String getCellTitle(String e) {
			String id = e;

			String init = Ctx.project.getWorld().getInitChapter();

			if (init.equals(id))
				id += " <init>";

			return id;
		}

	};

}

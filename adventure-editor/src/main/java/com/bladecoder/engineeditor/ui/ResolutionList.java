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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.DesktopUtils;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.CellRenderer;
import com.bladecoder.engineeditor.ui.panels.EditList;

public class ResolutionList extends EditList<String> {

	public ResolutionList(Skin skin) {
		super(skin);
		toolbar.hideCopyPaste();		

		list.setCellRenderer(listCellRenderer);
		
		list.addListener(new ChangeListener() {
			
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				toolbar.disableEdit(pos == -1);
			}
		});

		Ctx.project.addPropertyChangeListener(Project.NOTIFY_PROJECT_LOADED,
				new PropertyChangeListener() {
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						toolbar.disableCreate(!Ctx.project.isLoaded());
						addResolutions();
					}
				});
	}

	private void addResolutions() {
		if (Ctx.project.isLoaded()) {

			list.getItems().clear();

			ArrayList<String> tmp = new ArrayList<String>();

			for (String scn : Ctx.project.getResolutions()) {
				tmp.add(scn);
			}

			Collections.sort(tmp);

			for (String s : tmp)
				list.getItems().add(s);

			if (list.getItems().size > 0) {
				list.setSelectedIndex(0);
			}
		}

		toolbar.disableCreate(!Ctx.project.isLoaded());
	}

	@Override
	public void create() {
		CreateResolutionDialog dialog = new CreateResolutionDialog(skin);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				addResolutions();
			}});
	}

	@Override
	public void edit() {

	}

	@Override
	public void delete() {
		int index = list.getSelectedIndex();
		String r = list.getItems().get(index);
		
		if(r.equals("1")) {
			Message.showMsg(getStage(),"Initial resolution cannot be deleted", 3);
			
			return;
		}

		removeDir(Ctx.project.getAssetPath() + Project.IMAGE_PATH
				+ "/" + r);
		removeDir(Ctx.project.getAssetPath() + Project.UI_PATH + "/"
				+ r);
		removeDir(Ctx.project.getAssetPath() + Project.ATLASES_PATH
				+ "/" + r);

		addResolutions();
	}

	private void removeDir(String dir) {
		try {
			DesktopUtils.removeDir(dir);
		} catch (IOException e) {
			String msg = "Something went wrong while deleting the resolution.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Message.showMsg(getStage(),msg, 2);
			EditorLogger.printStackTrace(e);
		}
	}
	
	@Override
	protected void copy() {
	
	}

	@Override
	protected void paste() {
	
	}

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private static final CellRenderer<String> listCellRenderer = new CellRenderer<String>() {
		@Override
		protected String getCellTitle(String r) {
			return r;
		}
	};
}

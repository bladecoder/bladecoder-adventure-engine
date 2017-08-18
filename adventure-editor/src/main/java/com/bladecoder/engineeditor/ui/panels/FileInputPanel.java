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

import java.io.File;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engineeditor.Ctx;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooser.Mode;
import com.kotcrab.vis.ui.widget.file.FileChooser.SelectionMode;
import com.kotcrab.vis.ui.widget.file.FileChooser.ViewMode;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;

public class FileInputPanel extends InputPanel {
	public enum DialogType {
		OPEN_FILE, SAVE_FILE, DIRECTORY
	}

	private File cd;
	private File selected;
	private TextButton button;

	private final static String FILE_TEXT = "Select file";
	private final static String DIR_TEXT = "Select folder";
	private FileChooser fileChooser;

	public FileInputPanel(Skin skin, String title, String desc, DialogType dialogType) {
		this(skin, title, desc, Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."),
				dialogType, true);
	}

	public FileInputPanel(Skin skin, String title, String desc, DialogType dialogType, boolean mandatory) {
		this(skin, title, desc, Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."),
				dialogType, mandatory);
	}

	public FileInputPanel(Skin skin, String title, String desc, File current, final DialogType dialogType,
			boolean mandatory) {
		Table t = new Table();
		button = new TextButton(dialogType == DialogType.DIRECTORY ? DIR_TEXT : FILE_TEXT, skin, "no-toggled");
		
		t.add(button);
		
		init(skin, title, desc, t,
				mandatory, null);

		switch (dialogType) {
		case DIRECTORY: 
			fileChooser = new FileChooser(Mode.OPEN);
			fileChooser.setSelectionMode(SelectionMode.DIRECTORIES);
			break;
		case OPEN_FILE:
			fileChooser = new FileChooser(Mode.OPEN);
			fileChooser.setSelectionMode(SelectionMode.FILES);
			break;
		case SAVE_FILE:
			fileChooser = new FileChooser(Mode.SAVE);
			fileChooser.setSelectionMode(SelectionMode.FILES);
			break;
		default:
			break;
		}
		
		fileChooser.setSize(Gdx.graphics.getWidth() * 0.7f, Gdx.graphics.getHeight() * 0.7f);
		fileChooser.setViewMode(ViewMode.LIST);

		button.addListener(new ClickListener() {
			public void clicked(InputEvent event, float x, float y) {
				if(cd != null)
					fileChooser.setDirectory(cd);
					
				getStage().addActor(fileChooser);

				fileChooser.setListener(new FileChooserListener() {

					@Override
					public void selected(Array<FileHandle> files) {
						selected = files.get(0).file();

						button.setText(selected.getAbsolutePath());
					}

					@Override
					public void canceled() {
					}
				});
			}
		});
		
		// Adds clear button if not mandatory
		if(!mandatory) {
			TextButton clearButton = new TextButton("Clear", skin, "no-toggled");
			
			clearButton.addListener(new ClickListener() {
				public void clicked(InputEvent event, float x, float y) {
					button.setText(dialogType == DialogType.DIRECTORY ? DIR_TEXT : FILE_TEXT);
					selected = null;
				}
			});
			
			t.add(clearButton);
		}
	}

	public File getFile() {
		return selected;
	}

	@Override
	public String getText() {
		if (selected != null)
			return selected.getAbsolutePath();
		else
			return "";
	}

	@Override
	public void setText(String text) {
		button.setText(text);
		selected = new File(text);
		cd = new File(text);
	}
	
	public void setFileTypeFilter(FileTypeFilter ftf) {
		fileChooser.setFileTypeFilter(ftf);
	}

}

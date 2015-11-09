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
package com.bladecoder.engineeditor.ui.components;

import java.io.File;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.bladecoder.engineeditor.Ctx;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class FileInputPanel extends InputPanel {
	public enum DialogType {
		OPEN_FILE, SAVE_FILE, DIRECTORY
	}

	private File cd;
	private File selected;

	private final static String FILE_TEXT = "Select file";
	private final static String DIR_TEXT = "Select folder";
	
	public FileInputPanel(Skin skin, String title, String desc, DialogType dialogType) {
		this(skin, title, desc, Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."), dialogType);
	}

	public FileInputPanel(Skin skin, String title, String desc, File current, final DialogType dialogType) {
		init(skin, title, desc, new TextButton(dialogType == DialogType.DIRECTORY ? DIR_TEXT : FILE_TEXT, skin), true, null);
		
		this.cd = current;

//		((TextField) getField()).setEditable(false);

		((TextButton) getField()).addListener(new ClickListener() {
			public void clicked (InputEvent event, float x, float y) {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						final File result;
						switch (dialogType) {
							case DIRECTORY: {
								DirectoryChooser chooser = new DirectoryChooser();
//								if(cd != null)
//									chooser.setInitialDirectory(cd);
								chooser.setTitle(DIR_TEXT);
								result = chooser.showDialog(null);
								break;
							}
							case OPEN_FILE:
							case SAVE_FILE: {
								FileChooser chooser = new FileChooser();
								chooser.setInitialDirectory(cd);
								chooser.setTitle(FILE_TEXT);
								result = dialogType == DialogType.OPEN_FILE
										? chooser.showOpenDialog(null)
										: chooser.showSaveDialog(null);
								break;
							}
							default:
								throw new RuntimeException("Unknown dialog type");
						}

						if (result != null) {
							((TextButton) getField()).setText(result.getAbsolutePath());
							selected = cd = result;
						}
					}
				});
			}
		});
	}
	
	public File getFile() {
		return selected;
	}
	
	@Override
	public String getText() {
		if(selected != null)
			return selected.getAbsolutePath();
		else 
			return "";
	}
	
	@Override
	public void setText(String text) {
		((TextButton)field).setText(text);
		selected=cd= new File(text);
	}
	
}

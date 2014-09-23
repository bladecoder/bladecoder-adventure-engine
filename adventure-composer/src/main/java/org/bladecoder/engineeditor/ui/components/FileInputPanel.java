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
package org.bladecoder.engineeditor.ui.components;

import java.io.File;

import javax.swing.JFileChooser;

import org.bladecoder.engineeditor.Ctx;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class FileInputPanel extends InputPanel {
	
	private File cd;
	private File selected;
	
	private boolean dirOnly = false;
	private final static String FILE_TEXT = "Select file";
	private final static String DIR_TEXT = "Select folder";
	
	public FileInputPanel(Skin skin, String title, String desc, boolean dirOnly) {
		this(skin, title, desc, Ctx.project.getProjectDir() != null ? Ctx.project.getProjectDir() : new File("."), dirOnly);
	}

	public FileInputPanel(Skin skin, String title, String desc, File current, boolean dOnly) {
		super(skin, title, desc, new TextButton(dOnly?DIR_TEXT:FILE_TEXT, skin), null);
		
		this.cd = current;
		this.dirOnly = dOnly;

//		((TextField) getField()).setEditable(false);

		((TextButton) getField()).addListener(new ClickListener() {
			public void clicked (InputEvent event, float x, float y) {
				JFileChooser chooser = new JFileChooser(cd);			
				
				if(dirOnly) {
					chooser.setDialogTitle("Select folder");
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				} else {
					chooser.setDialogTitle("Select file");
				}
				
				chooser.setMultiSelectionEnabled(false);

				if (chooser.showDialog(null, "Ok") == JFileChooser.APPROVE_OPTION) {
					((TextButton) getField()).setText(chooser.getSelectedFile().getAbsolutePath());
					selected = cd = chooser.getSelectedFile();
				}				
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
		super.setText(text);
		selected=cd= new File(text);
	}
	
}

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

import java.io.IOException;
import java.text.MessageFormat;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.common.Message;
import com.bladecoder.engineeditor.model.Chapter;
import com.bladecoder.engineeditor.ui.panels.EditDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class EditChapterDialog extends EditDialog {

	private Chapter doc;
	private InputPanel inputId;

	private String previousId = null;
	private String newId = null;
	
    private ChangeListener listener;

	public EditChapterDialog(Skin skin, Chapter doc, String chapter) {
		super("", skin);
		
		this.doc = doc;

		inputId = InputPanelFactory.createInputPanel(skin, "Chapter ID", "The id of the chapter",
				true);

		setInfo("An adventure game is composed of chapters. Chapters contains scenes.");
		
		addInputPanel(inputId);
		
		if (chapter == null) {
			setTitle("CREATE CHAPTER");
		} else {
			setTitle(MessageFormat.format("EDIT CHAPTER ''{0}''", chapter));
			
			inputId.setText(chapter);
		}

		previousId = chapter;
		
	}
	

	private void create() {
		try {	
			newId = doc.createChapter(inputId.getText());
		} catch (TransformerException
				| ParserConfigurationException | IOException e) {
			String msg = "Something went wrong while creating the chapter.\n\n"
					+ e.getClass().getSimpleName() + " - " + e.getMessage();
			Message.showMsg(getStage(), msg, 2);

			EditorLogger.printStackTrace(e);
		}
	}

	private void renameChapter() {
		
		if (previousId != null && !previousId.equals(newId)) {

			try {
				// save selected chapter if renamed chapter is the selected chapter
				if(previousId.equals(Ctx.project.getChapter())) {
					Ctx.project.getChapter().save();
				}
				
				doc.renameChapter(previousId, newId);
				
				// Reload chapter if renamed chapter is the selected chapter
				if(previousId.equals(Ctx.project.getChapter())) {
					Ctx.project.loadChapter(newId);
				}
				
				// sets the init chapter
				if(previousId.equals(doc.getInitChapter())) {
					Ctx.project.getWorld().setInitChapter(newId);
				}
				
				Ctx.project.saveProject();
			} catch (TransformerException | ParserConfigurationException
					| SAXException | IOException e1) {
				String msg = "Something went wrong while renaming the chapter.\n\n"
						+ e1.getClass().getSimpleName()
						+ " - "
						+ e1.getMessage();
				Message.showMsg(getStage(), msg, 3);

				EditorLogger.printStackTrace(e1);
			}
		}
	}


	@Override
	protected boolean validateFields() {
		return inputId.validateField();
	}


	@Override
	protected void ok() {
		if (previousId == null) {
			create();
		} else {
			newId = inputId.getText();
			renameChapter();
		}
		
		if(listener != null)
			listener.changed(new ChangeEvent(), this);
	}
	
	public void setListener(ChangeListener l) {
		listener = l;
	}

	String getNewId() {
		return newId;
	}
}

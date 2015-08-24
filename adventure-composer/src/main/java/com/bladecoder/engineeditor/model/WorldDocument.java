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
package com.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.XML2Bean;
import org.xml.sax.SAXException;

import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.utils.EditorLogger;

public class WorldDocument extends  BaseDocument {	
	public static final int DEFAULT_WIDTH = 1920;
	public static final int DEFAULT_HEIGHT = 1080;
	
	public static final String NOTIFY_DOCUMENT_MODIFIED = "DOCUMENT_MODIFIED";
	
    private PropertyChangeListener documentModifiedListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
//			if(!evt.getPropertyName().equals(NOTIFY_DOCUMENT_MODIFIED))
			firePropertyChange(evt);
			EditorLogger.debug("WorldDocument Listener: " +  evt.getPropertyName());
		}
	};
	private World world;

	public WorldDocument() {
		setFilename(XMLConstants.WORLD_FILENAME);
	}
	
	@Override
	public String getRootTag() {
		return XMLConstants.WORLD_TAG;
	}

	@Override
	public void create() throws ParserConfigurationException {
		super.create();
		
		setDimensions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}
	
	public void setDimensions(int width, int height) {
		world.setWidth(width);
		world.setHeight(height);
		modified = true;
		firePropertyChange();
	}
	
	public String[] getChapters() {
		String dir = Ctx.project.getProjectPath() + Project.MODEL_PATH;
		
		String[] chapters = new File(dir).list((arg0, arg1) -> arg1.endsWith(XMLConstants.CHAPTER_EXT));
		
		for(int i = 0; i < chapters.length; i++)
			chapters[i] = chapters[i].substring(0, chapters[i].lastIndexOf('.'));
		
		return chapters;
	}

	public int getWidth() {
		return world.getWidth();
	}
	
	public int getHeight() {		
		return world.getHeight();
	}

	public void setWidth(String value) {
		world.setWidth(Integer.parseInt(value));

		modified = true;
		firePropertyChange();
	}
	
	public void setHeight(String value) {
		world.setHeight(Integer.parseInt(value));

		modified = true;
		firePropertyChange();
	}
	
	public ChapterDocument loadChapter(String id) throws ParserConfigurationException, SAXException, IOException {
			ChapterDocument chapter = new ChapterDocument(modelPath);
			chapter.setFilename(id + XMLConstants.CHAPTER_EXT);
			chapter.load();
			chapter.addPropertyChangeListener(documentModifiedListener);
			
			return chapter;
	}
	
	public String getInitChapter() {
		String init = world.getInitChapter();
		
		if(init == null || init.isEmpty()) {
			init = getChapters()[0];
			
			world.setInitChapter(init);
		}
		
		return init;
	}
	
	public void setInitChapter(String value) {
		world.setInitChapter(value);

		modified = true;
		firePropertyChange();
	}
	
	public ChapterDocument createChapter(String id) throws IOException, TransformerException, ParserConfigurationException {
		ChapterDocument chapter = new ChapterDocument(modelPath);	
		String checkedId = getChapterCheckedId(id);
		
		chapter.create(checkedId);
		firePropertyChange(XMLConstants.CHAPTER_TAG);
		
		return chapter;
	}
	
	public String getChapterCheckedId(String id) {
		boolean checked = false;
		int i = 1;
		
		String idChecked = id;
		
		String [] nl = getChapters();

		while (!checked) {
			checked = true;

			for (int j = 0; j < nl.length; j++) {
				String id2 = nl[j];
						
				if (id2.equals(idChecked)) {
					i++;
					idChecked = id + i;
					checked = false;
					break;
				}
			}
		}
		
		return idChecked;
	}
		
	public void renameChapter(String oldId, String newId) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		
		ChapterDocument chapter = new ChapterDocument(modelPath);
		chapter.setFilename(oldId + XMLConstants.CHAPTER_EXT);
		chapter.load();
		chapter.rename(newId);
		firePropertyChange(XMLConstants.CHAPTER_TAG);
	}
	
	public void removeChapter(String id) throws FileNotFoundException, TransformerException {
		
		ChapterDocument chapter = new ChapterDocument(modelPath);
		chapter.setFilename(id + XMLConstants.CHAPTER_EXT);
		chapter.deleteFiles();
		firePropertyChange(XMLConstants.CHAPTER_TAG);
	}
	
	@Override
	public void load() throws ParserConfigurationException, SAXException, IOException {
		File file = new File(getAbsoluteName());
		this.world = XML2Bean.loadJson(file, World.class);
		loadI18N();
	}

	@Override
	public void save() throws TransformerException, IOException {
		if (!modified)
			return;

		File file = new File(getAbsoluteName());
		XML2Bean.saveJson(file, world);
		saveI18N();

		modified = false;
		firePropertyChange(NOTIFY_DOCUMENT_SAVED);
	}

	@Override
	public void setModified() {
		modified = true;
		firePropertyChange(DOCUMENT_CHANGED, null, world);
	}
}

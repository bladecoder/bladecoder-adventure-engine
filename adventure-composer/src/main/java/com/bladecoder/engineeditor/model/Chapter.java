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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.World;

public class Chapter {
	private String modelPath;
	private String id;
	private String filename;

	public Chapter(String modelPath) {
		this.modelPath = modelPath;
		
		if(!modelPath.endsWith("/"))
			this.modelPath = modelPath + "/";
	}

	public void create(String id) throws IOException {
		filename = modelPath + id + XMLConstants.CHAPTER_EXT;
		this.id = id;
		save();
	}

	public void rename(String newId) throws IOException {

		deleteFiles();

		filename = modelPath + id + XMLConstants.CHAPTER_EXT;
		id = newId;
		save();
	}

	public void deleteFiles() {
		File f = new File(filename);
		f.delete();

		String i18nFilename = modelPath + id + ".properties";
		f = new File(i18nFilename);
		f.delete();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		this.filename = modelPath + id + XMLConstants.CHAPTER_EXT;
	}

	public String toString() {
		return getId();
	}
	
	public void load(String id) throws IOException {
		setId(id);
		World.getInstance().loadChapter(id);
	}
	
	public void save() throws IOException {
		World.getInstance().saveModel();
	}
	
	public String[] getChapters() {
		
		String[] chapters = new File(modelPath).list(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				if (!arg1.endsWith(XMLConstants.CHAPTER_EXT))
					return false;

				return true;
			}
		});
		
		for(int i = 0; i < chapters.length; i++)
			chapters[i] = chapters[i].substring(0, chapters[i].lastIndexOf('.'));
		
		return chapters;
	}
	
	public String getInitChapter() {
		String init = World.getInstance().getInitChapter();
		
		if(init == null || init.isEmpty()) {
			init = getChapters()[0];
			
			World.getInstance().setInitChapter(init);
		}
		
		return init;
	}
	
	public Chapter createChapter(String id) throws TransformerException, ParserConfigurationException, IOException {
		Chapter chapter = new Chapter(modelPath);	
		String checkedId = getChapterCheckedId(id);
		
		chapter.create(checkedId);
		
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
		Chapter chapter = new Chapter(modelPath);
		
		chapter.setId(oldId);
		chapter.rename(newId);
	}
	
	public void deleteChapter(String id) throws TransformerException, ParserConfigurationException, SAXException, IOException {		
		Chapter chapter = new Chapter(modelPath);
		
		chapter.setId(id);
		chapter.deleteFiles();
	}
}

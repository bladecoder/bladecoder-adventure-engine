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
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.common.ElementUtils;

public class Chapter {
	private String modelPath;
	private String id;

	public Chapter(String modelPath) {
		this.modelPath = modelPath;
		
		if(!modelPath.endsWith("/"))
			this.modelPath = modelPath + "/";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String toString() {
		return getId();
	}
	
	public void load(String id) throws IOException {
		setId(id);
		World.getInstance().loadChapter(id);
	}
	
	public void save() throws IOException {
		World.getInstance().saveModel(id);
	}
	
	public String[] getChapters() {
		
		String[] chapters = new File(modelPath).list(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				if (!arg1.endsWith(EngineAssetManager.CHAPTER_EXT) &&
						!arg1.endsWith(".chapter"))
					return false;

				return true;
			}
		});
		
		for(int i = 0; i < chapters.length; i++) {
			if(chapters[i].endsWith(EngineAssetManager.CHAPTER_EXT))
				chapters[i] = chapters[i].substring(0, chapters[i].lastIndexOf(EngineAssetManager.CHAPTER_EXT));
			else
				chapters[i] = chapters[i].substring(0, chapters[i].lastIndexOf(".chapter"));
		}
		
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
	
	public String createChapter(String id) throws TransformerException, ParserConfigurationException, IOException {
		String checkedId = ElementUtils.getCheckedId(id, getChapters());
		
		URL inputUrl = getClass().getResource("/projectTmpl/android/assets/model/00.chapter.json");
		File dest = new File(modelPath + checkedId + EngineAssetManager.CHAPTER_EXT);
		FileUtils.copyURLToFile(inputUrl, dest);
		
		return checkedId;
	}
		
	public void renameChapter(String oldId, String newId) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		File f = new File(modelPath + id + EngineAssetManager.CHAPTER_EXT);
		f.renameTo(new File(modelPath + newId + EngineAssetManager.CHAPTER_EXT));
		
		String i18nFilename = modelPath + id + ".properties";
		f = new File(i18nFilename);
		f.renameTo(new File(modelPath + newId + ".properties"));
	}
	
	public void deleteChapter(String id) throws TransformerException, ParserConfigurationException, SAXException, IOException {		
		File f = new File(modelPath + id + EngineAssetManager.CHAPTER_EXT);
		f.delete();

		String i18nFilename = modelPath + id + ".properties";
		f = new File(i18nFilename);
		f.delete();
	}
}

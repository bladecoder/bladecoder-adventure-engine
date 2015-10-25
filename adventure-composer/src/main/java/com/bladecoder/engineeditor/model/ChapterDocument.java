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
import java.io.FileNotFoundException;

import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.World;

public class ChapterDocument extends BaseDocument {
	
	private String id;

	public ChapterDocument(String modelPath, String id) {
		super();
		setModelPath(modelPath);
		this.id = id;
		setFilename(id + XMLConstants.CHAPTER_EXT);
	}

	public void setFilenameFromId() {
		setFilename(getId() + XMLConstants.CHAPTER_EXT);
	}

	public void create(String id) throws FileNotFoundException {
		create();
		setId(id);
		setFilenameFromId();
		save();
	}

	public void rename(String newId) throws FileNotFoundException {

		deleteFiles();

		setId(newId);
		setFilenameFromId();

		save();
	}

	public void deleteFiles() {
		File f = new File(getAbsoluteName());

		f.delete();

		// TODO delete all .properties
		f = new File(getI18NFilename());
		f.delete();
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
	
	@Override
	public void load() {
		super.load();	
		World.getInstance().loadChapter(id);
	}
}

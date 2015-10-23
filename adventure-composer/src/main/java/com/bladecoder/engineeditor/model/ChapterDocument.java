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
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engine.model.World;

public class ChapterDocument extends BaseDocument {

	public static final String ACTOR_TYPES[] = { XMLConstants.BACKGROUND_VALUE, XMLConstants.SPRITE_VALUE,
			XMLConstants.CHARACTER_VALUE, XMLConstants.OBSTACLE_VALUE, XMLConstants.ANCHOR_VALUE };

	public static final String ACTOR_RENDERERS[] = { XMLConstants.ATLAS_VALUE, XMLConstants.SPINE_VALUE,
			XMLConstants.S3D_VALUE, XMLConstants.IMAGE_VALUE };

	public static final String ANIMATION_TYPES[] = { XMLConstants.NO_REPEAT_VALUE, XMLConstants.REPEAT_VALUE,
			XMLConstants.YOYO_VALUE, XMLConstants.REVERSE_VALUE };
	
	String id;

	public ChapterDocument(String modelPath, String id) {
		super();
		setModelPath(modelPath);
		this.id = id;
		setFilename(id + XMLConstants.CHAPTER_EXT);
	}

	public void setFilenameFromId() {
		setFilename(getId() + XMLConstants.CHAPTER_EXT);
	}

	public void create(String id) throws ParserConfigurationException, FileNotFoundException, TransformerException {
		create();
		setId(id);
		setFilenameFromId();
		save();
	}

	public void rename(String newId) throws FileNotFoundException, TransformerException {

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
	public void load() throws ParserConfigurationException, SAXException, IOException {
		super.load();	
		World.getInstance().loadChapter(id);
	}
}

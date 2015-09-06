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
package com.bladecoder.engine.loader;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.bladecoder.engine.model.XML2Bean;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;

public class WorldXMLLoader {
	public static void loadWorld(World world) throws IOException {
		XML2Bean.loadJson(EngineAssetManager.getInstance()
				.getModelFile(XMLConstants.WORLD_FILENAME).file(), world);

		// When we know the world width, we can put the scale
		final int width = world.getWidth();
		final int height = world.getHeight();
		if (width < 1 || height < 1) {
			throw new RuntimeException("World 'width' or 'height' missing or incorrect in JSON.");
		}
		EngineAssetManager.getInstance().setScale(width, height);
		float scale = EngineAssetManager.getInstance().getScale();
		world.setWidth((int) (width * scale));
		world.setHeight((int) (height * scale));

		I18N.loadWorld(EngineAssetManager.MODEL_DIR + "world");
	}

	public static void loadChapter(String chapter, World world)
			throws ParserConfigurationException, SAXException, IOException {
		if (chapter == null) {
			chapter = world.getInitChapter();
		}

		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser saxParser = spf.newSAXParser();

		ChapterXMLLoader parser = new ChapterXMLLoader();
		XMLReader xmlReader = saxParser.getXMLReader();
		xmlReader.setContentHandler(parser);
		xmlReader.parse(new InputSource(EngineAssetManager.getInstance()
				.getModelFile(chapter + XMLConstants.CHAPTER_EXT).read()));

		I18N.loadChapter(EngineAssetManager.MODEL_DIR + chapter);

		world.setChapter(chapter);

		for (Scene s : parser.getScenes()) {
			s.resetCamera(world.getWidth(), world.getHeight());

			world.addScene(s);
		}

		if (parser.getInitScene() != null)
			world.setCurrentScene(parser.getInitScene());
		else if (parser.getScenes().size() > 0)
			world.setCurrentScene(parser.getScenes().get(0).getId());
	}
}
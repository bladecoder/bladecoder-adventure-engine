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
import java.text.MessageFormat;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.bladecoder.engine.actions.AbstractAction;
import com.bladecoder.engine.model.XML2Bean;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.bladecoder.engine.actions.ActionFactory;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class WorldXMLLoader extends DefaultHandler {
	
	private World world;

	private Verb currentVerb;

	private float scale;

	private Locator locator;

	public static void loadWorld(World world)
			throws ParserConfigurationException, SAXException, IOException {
		XML2Bean.loadJson(EngineAssetManager.getInstance()
				.getModelFile(XMLConstants.WORLD_FILENAME).file(), world);

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

	public WorldXMLLoader(World world) {
		this.world = world;
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (currentVerb != null) {
			parseAction(localName, atts);
		} else if (localName.equals(XMLConstants.WORLD_TAG)) {
			int width, height;

			try {
				width = Integer.parseInt(atts.getValue(XMLConstants.WIDTH_ATTR));
				height = Integer.parseInt(atts.getValue(XMLConstants.HEIGHT_ATTR));

				// When we know the world width, we can put the scale
				EngineAssetManager.getInstance().setScale(width, height);
				scale = EngineAssetManager.getInstance().getScale();

				width = (int) (width * scale);
				height = (int) (height * scale);

			} catch (NumberFormatException e) {
				SAXParseException e2 = new SAXParseException(
						"World 'width' or 'height' missing or incorrect in XML.",
						locator);
				error(e2);
				throw e2;
			}

			world.setWidth(width);
			world.setHeight(height);
			world.setInitChapter(atts.getValue(XMLConstants.INIT_CHAPTER_ATTR));
		} else if (localName.equals(XMLConstants.VERB_TAG)) {
			String id = atts.getValue(XMLConstants.ID_ATTR);

			currentVerb = new Verb();
			currentVerb.setId(id);

			world.getVerbManager().addVerb(currentVerb);
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {

		if (localName.equals(XMLConstants.VERB_TAG)) {
			currentVerb = null;
		}
	}

	private void parseAction(String localName, Attributes atts) {

		if (localName.equals(XMLConstants.ACTION_TAG)) {
			String actionName = null;
			AbstractAction action = null;
			HashMap<String, String> params = new HashMap<String, String>();
			String actionClass = null;

			for (int i = 0; i < atts.getLength(); i++) {
				String attName = atts.getLocalName(i);

				if (attName.equals(XMLConstants.CLASS_ATTR)) {
					actionClass = atts.getValue(attName);
				} else if (attName.equals(XMLConstants.ACTION_NAME_ATTR)) {
					actionName = atts.getValue(attName);
				} else {
					String value = atts.getValue(attName);

					params.put(attName, value);
				}
			}

			if (actionClass != null) {
				action = ActionFactory.createByClass(actionClass, params);
			} else if (actionName != null) {
				action = ActionFactory.create(actionName, params);
			}

			if (action != null) {
				currentVerb.addAction(action);
			}
		} else {
			EngineLogger.error("TAG not supported inside VERB: " + localName
					+ " LINE: " + locator.getLineNumber());
		}
	}

	@Override
	public void setDocumentLocator(Locator l) {
		locator = l;
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		EngineLogger.error(MessageFormat.format(
				"{0} in 'world.xml' Line: {1} Column: {2}", e.getMessage(),
				e.getLineNumber(), e.getColumnNumber()));
	}

}
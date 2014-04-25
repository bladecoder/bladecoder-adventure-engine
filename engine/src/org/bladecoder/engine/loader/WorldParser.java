package org.bladecoder.engine.loader;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionFactory;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.Verb;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;
import org.bladecoder.engine.util.I18NControl;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class WorldParser extends DefaultHandler {
	World world;
	
	String initScene;
	Verb currentVerb;
	
	ResourceBundle i18n;

	float scale;
	
	Locator locator;
	
	String chapter;
	String currentChapter;

	public WorldParser(World world, String i18nFilename, String chapter) {
		this.world = world;
		this.chapter = chapter;
		
		Locale locale = Locale.getDefault();
		
		try{
			i18n = ResourceBundle.getBundle(i18nFilename, locale, new I18NControl("ISO-8859-1"));
		} catch (Exception e) {
			EngineLogger.error(e.getMessage());
		}
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		if (currentVerb != null) {
			String actionName = localName;
			Action action = null;
			HashMap<String, String> params = new HashMap<String, String>();
			String actionClass = null;

			for (int i = 0; i < atts.getLength(); i++) {
				String attName = atts.getLocalName(i);

				if (attName.equals("class")) {
					actionClass = atts.getValue(attName);
				} else {
					String value = atts.getValue(attName);
					if(i18n!=null && value!=null && value.length() > 0 && value.charAt(0) == '@') value = i18n.getString(value.substring(1));							
					
					params.put(attName, value);
				}
			}
			

			if (actionClass != null) {
				action = ActionFactory.createByClass(actionClass, params);
			} else {
				action = ActionFactory.create(actionName, params);
			}
			
			if (action != null) {
				currentVerb.add(action);			
			} else {
				EngineLogger.error("Action '" + actionName + "' not found.");
			}

		} else if (localName.equals("world")) {
			int width, height;

			try {
				width = Integer.parseInt(atts.getValue("width"));
				height = Integer.parseInt(atts.getValue("height"));
				
				// When we know the world width, we can put the scale
				EngineAssetManager.getInstance().setScale(width);
				scale = EngineAssetManager.getInstance().getScale();
				
				width =  (int) (width * scale);
				height = (int) (height * scale);
				
			} catch (NumberFormatException e) {
				SAXParseException e2 = new SAXParseException("World 'width' or 'height' missing or incorrect in XML.", locator);
				error(e2);
				throw e2;
			}
			
			world.setWidth(width);
			world.setHeight(height);
			
			if(chapter == null)
				chapter = atts.getValue("init_chapter");
			
		} else if (localName.equals("chapter")) {
			currentChapter = atts.getValue("id");
			
			if(chapter == null) chapter = currentChapter;
			
			if(chapter.equals(currentChapter))
				initScene = atts.getValue("init_scene");
		} else if (localName.equals("scene") && currentChapter.equals(chapter)) {
			String filename = atts.getValue("filename");
			Scene scene;

			if (filename == null || filename.isEmpty()) {
				SAXParseException e2 = new SAXParseException("Scene filename not found or empty.", locator);
				error(e2);
				throw e2;				
			}
			
			try {
						
				scene = XMLLoader.loadScene(filename);
				
				if(scene!=null && initScene==null) initScene = scene.getId();
				
				scene.resetCamera(world.getWidth(), world.getHeight());
				
				world.addScene(scene);
			} catch (Exception e) {
				SAXParseException e2 = new SAXParseException("Error loading scene '" + filename + "'", locator, e);
				error(e2);
				throw e2;
			}				
						
//		} else if (localName.equals("actor")) { //TODO Check inside 'inventory'
//			String filename = atts.getValue("filename");
//
//			SpriteActor actor = null;
//
//			try {
//				actor = (SpriteActor) XMLLoader.loadActor(filename, i18n);
//			} catch (Exception e) {
//				SAXParseException e2 = new SAXParseException("Error loading XML '" + filename + "' for inventory actor.", locator, e);
//				error(e2);
//				throw e2;
//			}
//
//			if (actor!=null)
//				world.getInventory().addItem(actor);
		} else if (localName.equals("verb")) {
			String id = atts.getValue("id");

			currentVerb = new Verb(id);

			Actor.addDefaultVerb(id, currentVerb);			
		}
	}
	
	@Override
	public void endElement(String namespaceURI, String localName,
			String qName) throws SAXException  {
		
		if (localName.equals("world")) {
			world.setCurrentScene(initScene);
		} else if (localName.equals("verb")) {
			currentVerb = null;
		}	
	}
	
	@Override
	public void setDocumentLocator(Locator l) {
		locator = l;
	}
	
	@Override
	public void error(SAXParseException e) throws SAXException {
        EngineLogger.error(MessageFormat.format("{0} in 'world.xml' Line: {1} Column: {2}", e.getMessage(),
				e.getLineNumber(), e.getColumnNumber()));
     }	
}
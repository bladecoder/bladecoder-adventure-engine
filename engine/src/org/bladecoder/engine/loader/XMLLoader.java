package org.bladecoder.engine.loader;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Scene;
import org.bladecoder.engine.model.World;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XMLLoader {

	public static Scene loadScene(String filename) throws ParserConfigurationException, SAXException, IOException  {	
		SAXParserFactory spf = SAXParserFactory.newInstance();
	    spf.setNamespaceAware(true);
	    SAXParser saxParser = spf.newSAXParser();
	    
	    String propFilename = "model/" + filename.substring(0, filename.lastIndexOf('.'));
	    SceneParser parser = new SceneParser(propFilename);
	    XMLReader xmlReader = saxParser.getXMLReader();
	    xmlReader.setContentHandler(parser);
	    xmlReader.parse(new InputSource(EngineAssetManager.getInstance().getModelFile(filename).read()));
	    
	    return parser.getScene();
	}

	public static void loadWorld(String filename, World world, String chapter) throws ParserConfigurationException, SAXException, IOException  {
		SAXParserFactory spf = SAXParserFactory.newInstance();
	    spf.setNamespaceAware(true);
	    SAXParser saxParser = spf.newSAXParser();
	    
	    String propFilename = "model/" + filename.substring(0, filename.lastIndexOf('.'));
	    WorldParser parser = new WorldParser(world, propFilename, chapter);
	    XMLReader xmlReader = saxParser.getXMLReader();
	    xmlReader.setContentHandler(parser);
	    xmlReader.parse(new InputSource(EngineAssetManager.getInstance().getModelFile(filename).read()));
	}	

}

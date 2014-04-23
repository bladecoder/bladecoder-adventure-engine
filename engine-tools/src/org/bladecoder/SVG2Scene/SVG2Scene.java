package org.bladecoder.SVG2Scene;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class SVG2Scene {

	/**
	 * @param args
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws ParserConfigurationException, SAXException, FileNotFoundException, IOException {
		
		if(args.length < 1) {
			usage();
			System.exit(0);
		}
		
		String svgFilename = args[0]; 
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
	    spf.setNamespaceAware(true);
	    SAXParser saxParser = spf.newSAXParser();
	    
	    SVGParser parser = new SVGParser();
	    XMLReader xmlReader = saxParser.getXMLReader();
	    xmlReader.setContentHandler(parser);
	    xmlReader.parse(new InputSource(new FileInputStream(svgFilename)));
	    
	    System.out.println("SCENE GENERATED SUCCESSFULLY!\n");
	}

	private static void usage() {
		System.out.println("SVG filename argument missing.\n");
	}

}

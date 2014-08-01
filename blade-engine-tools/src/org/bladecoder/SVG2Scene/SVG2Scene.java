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

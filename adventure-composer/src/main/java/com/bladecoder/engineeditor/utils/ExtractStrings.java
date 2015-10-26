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
package com.bladecoder.engineeditor.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.bladecoder.engine.i18n.I18N;

/**
 * Extract all strings from chapter to the I18N properties file.
 * 
 * @author rgarcia
 */
public class ExtractStrings {
	private final static String[] i18nAttributes = {"text", "desc", "response_text"};
	
	// other locale message properties to generate
	private final static String[] locales = {"es"};
	
	Properties prop = new Properties();

	String xmlFilename;
	String propFilename;
	String propFilenameLocales[] = new String[locales.length];
	Properties propLocales[] = new Properties[locales.length];

	Document doc;

	public void parseXML(String xmlFilename, String propFilename) throws ParserConfigurationException, SAXException,
			IOException {
		this.xmlFilename = xmlFilename;
		this.propFilename = propFilename;
		
		// If the .properties name is not given, it is used the same that the xml 
		if(propFilename == null) {
			this.propFilename = xmlFilename.substring(0, xmlFilename.length() - 4) + ".properties";
			
			for(int i = 0; i < locales.length; i++) {
				propFilenameLocales[i] = xmlFilename.substring(0, xmlFilename.length() - 4) + "_" + locales[i] + ".properties";
			}
		} else {
			for(int i = 0; i < locales.length; i++) {
				propFilenameLocales[i] = this.propFilename.substring(0, this.propFilename.length() - 11) + "_" + locales[i] + ".properties";
			}
		}

		File fXmlFile = new File(xmlFilename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(fXmlFile);
		
		File propFile = new File(this.propFilename);
		
		if(propFile.exists()) {
			prop.load(new FileInputStream(propFile));
		}
		
		for(int i = 0; i < propFilenameLocales.length; i++) {
			File propFileLocale = new File(propFilenameLocales[i]);
			propLocales[i] = new Properties();
		
			if(propFileLocale.exists()) {
				propLocales[i].load(new FileInputStream(propFileLocale));
			}
		}
	}

	public void extract() {
		extract(doc.getDocumentElement(), doc.getDocumentElement().getAttribute("id"));
	}

	private void extract(Element e, String key) {
		for(String attr:i18nAttributes) {
			if(e.getAttribute(attr) != null && !e.getAttribute(attr).isEmpty()) {
				String key2 = key + "." + attr;
				String value = e.getAttribute(attr);
				
				if(e.getAttribute(attr).charAt(0) != I18N.PREFIX) {
					prop.put(key2, value);
					
					for(int i = 0; i < locales.length; i++) {
						propLocales[i].put(key2, value);
					}

					// change XML value
					e.getAttributeNode(attr).setNodeValue("@" + key2);
				}
			}
		}

		NodeList nodes = e.getChildNodes();

		for (int i = 0, j=0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);

			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e2 = (Element) n;
				
				String state = e2.getAttribute("state");
				String target = e2.getAttribute("target");
				String id = e2.getAttribute("id");
				
				if (id.isEmpty())
					id = Integer.toString(j);
				
				if(!state.isEmpty()) id  = id + "." + state;
				if(!target.isEmpty()) id  = id + "." + target;				

				String key2 = key + "." + id;
				
				if(key.isEmpty()) key2 = id;
				
				extract(e2, key2);
				
				j++;
			}
		}

	}

	public void writeProp() throws IOException {
		FileOutputStream os = new FileOutputStream(propFilename);				
		Writer out = new OutputStreamWriter(os, "ISO-8859-1");
		prop.store(out, null);
		
		for(int i = 0; i < locales.length; i++) {
			FileOutputStream osES = new FileOutputStream(propFilenameLocales[i]);		
			Writer outES = new OutputStreamWriter(osES, "ISO-8859-1");
			propLocales[i].store(outES, null);
		}
	}

	public void writeXML() throws TransformerException, FileNotFoundException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new FileOutputStream(xmlFilename));
		transformer.transform(source, result);
	}

	/**
	 * Main method
	 * 
	 * @param args
	 *            XML filename
	 *            Props filename (optional)
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public static void main(String[] args) throws ParserConfigurationException, SAXException,
			IOException, TransformerException {

		if (args.length < 1) {
			usage();
			System.exit(0);
		}
		
		String xmlFilename = args[0];
		String propFilename = null;
		
		if(args.length == 2) {
			propFilename = args[1];
		}

		ExtractStrings e = new ExtractStrings();
		e.parseXML(xmlFilename, propFilename);
		e.extract();
		e.writeProp();
		e.writeXML();
		System.out.println("Strings extracted successfully for " + args[0]);
	}

	private static void usage() {
		System.out.println("XML filename argument missing.\n");
	}
}

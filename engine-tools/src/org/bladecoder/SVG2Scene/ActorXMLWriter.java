package org.bladecoder.SVG2Scene;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

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

public class ActorXMLWriter {
	
	Document document;
	String id;

	public ActorXMLWriter() throws ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		
		document = documentBuilder.newDocument();		
	}


	public void write(String filename) throws TransformerException, FileNotFoundException {
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(new FileOutputStream(filename));
		transformer.transform(source, result);
	}


	public void createRootElement(String id, float x, float y, float width,
			float height, String desc, String lookat, String pickup, String filename) {
		Element rootElement = null;
		
		if(filename ==null ) rootElement = document.createElement("actor");
		else  rootElement = document.createElement("sprite_actor");

		this.id = id;


		rootElement.setAttribute("id", id);
		rootElement.setAttribute("desc", desc);
		rootElement.setAttribute("x", ""+ (filename==null?x:x - width/2));
		rootElement.setAttribute("y", ""+y);
		
		if(filename!=null) {
			rootElement.setAttribute("width", ""+width);
			rootElement.setAttribute("height", ""+height);
		}
				
		document.appendChild(rootElement);
		
		Element verbLookat = document.createElement("verb");
		verbLookat.setAttribute("id", "lookat");
		verbLookat.setAttribute("text", lookat);
		rootElement.appendChild(verbLookat);
		
		if(pickup!=null && !pickup.isEmpty()) {
		Element verbPickup = document.createElement("verb");
		verbPickup.setAttribute("id", "pickup");
		verbPickup.setAttribute("text", pickup);
		rootElement.appendChild(verbPickup);
		}
		
		if(filename!=null) {
			// <frame_animation id="default" start_frame="0" num_frames = "1"/>
			Element fa = document.createElement("frame_animation");
			fa.setAttribute("id", "default");
			fa.setAttribute("atlas", "scene"); // TODO SCENE NAME

			rootElement.appendChild(fa);			
		}
	}
}

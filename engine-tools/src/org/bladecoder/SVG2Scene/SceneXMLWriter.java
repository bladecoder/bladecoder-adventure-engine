package org.bladecoder.SVG2Scene;

import java.io.FileNotFoundException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;

public class SceneXMLWriter extends ActorXMLWriter {

	private final static String ROOT_TAG = "scene";
	private final static String BACKGROUND_TAG = "background";
	private final static String ACTOR_TAG = "actor";

	public SceneXMLWriter() throws ParserConfigurationException {
		super();
	}

	public void createRootElement(String id) {
		Element rootElement = document.createElement(ROOT_TAG);

		this.id = id;

		document.appendChild(rootElement);
		rootElement.setAttribute("id", id);
	}

	public void createBackgroundElement(String filename) {
		String mapFilename = filename.substring(0,filename.length() - 4) + "_map.png";
		
		Element bg = document.createElement(BACKGROUND_TAG);
		document.getDocumentElement().appendChild(bg);
		bg.setAttribute("filename", this.id + "/" + filename);
		bg.setAttribute("map", this.id + "/" + mapFilename);
	}

	public void createActorElement(String id, float x, float y, float width,
			float height, String desc, String lookat, String pickup, String filename) throws ParserConfigurationException,
			FileNotFoundException, TransformerException {
		Element a = document.createElement(ACTOR_TAG);
		document.getDocumentElement().appendChild(a);
		a.setAttribute("filename", this.id + "/" + id + ".xml");

		ActorXMLWriter actor = new ActorXMLWriter();

		actor.createRootElement(id, x, y, width, height, desc, lookat, pickup, 
				filename==null?null:this.id + "/" + filename);
		
		actor.write(id + ".xml");
	}

	public void createPlayerElement() {

	}

}
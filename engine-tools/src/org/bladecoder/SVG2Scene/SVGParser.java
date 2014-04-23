package org.bladecoder.SVG2Scene;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SVGParser extends DefaultHandler {
	SceneXMLWriter scene;

	float width, height;

	String scene_name;

	Point2D.Float groupTranslation;

	Actor currentActor;
	String currentTag;

	ArrayList<ArrayList<Point2D.Float>> paths = new ArrayList<ArrayList<Point2D.Float>>();

	public SVGParser() throws ParserConfigurationException {
		scene = new SceneXMLWriter();
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {

		currentTag = localName;

		// for(int i = 0; i< atts.getLength(); i++) {
		// System.out.println(atts.getLocalName(i) + " " + atts.getQName(i) +
		// " " + atts.getURI(i) + " " + atts.getValue(i));
		// }

		if (localName.equals("svg")) {
			String widthstr = atts.getValue("width");
			String heightstr = atts.getValue("height");
			scene_name = atts.getValue("sodipodi:docname");

			if (scene_name == null)
				scene_name = "scene";
			else
				scene_name = scene_name.substring(0, scene_name.indexOf('.'));

			width = Float.parseFloat(widthstr);
			height = Float.parseFloat(heightstr);

			scene.createRootElement(scene_name);

		} else if (localName.equals("g")) {

			groupTranslation = SVGParseUtils.parseTransform(atts
					.getValue("transform"));

		} else if (localName.equals("rect") || localName.equals("image")) {
			currentActor = new Actor();

			currentActor.id = atts.getValue("id");
			currentActor.desc = atts.getValue("inkscape:label");

			String widthstr = atts.getValue("width");
			String heightstr = atts.getValue("height");
			String xstr = atts.getValue("x");
			String ystr = atts.getValue("y");

			currentActor.width = Float.parseFloat(widthstr);
			currentActor.height = Float.parseFloat(heightstr);
			currentActor.x = Float.parseFloat(xstr);
			currentActor.y = Float.parseFloat(ystr);

			// apply group translation
			if (groupTranslation != null) {
				currentActor.x += groupTranslation.x;
				currentActor.y += groupTranslation.y;
			}

			// apply own translation
			Point2D.Float translation = SVGParseUtils.parseTransform(atts
					.getValue("transform"));
			if (translation != null) {
				currentActor.x += translation.x;
				currentActor.y += translation.y;
			}

			// invert y coordinate
			currentActor.y = height - currentActor.y - currentActor.height - 1;
			currentActor.filename = atts.getValue("xlink:href");

		} else if (localName.equals("path")) {
			String d = atts.getValue("d");
//			String transform = atts.getValue("transform");

			if (d == null || d.isEmpty())
				throw new SAXException("vertices (d) not found or empty");

			ArrayList<Point2D.Float> p = SVGParseUtils.parsePathD(d);
			paths.add(p);

			// PathParser.parseTransform(transform, p.getVertices());

			// Corregimos el eje Y que está invertido
			for (Point2D.Float v : p) {
				v.y = height - v.y - 1;
			}

		}
	}

	@Override
	public void characters(char[] cbuf, int start, int len) {
		if (currentTag == null)
			return;

		String text = new String(cbuf, start, len);

		if (currentTag.equals("desc"))
			currentActor.pickup = text;
		else if (currentTag.equals("title"))
			currentActor.lookat = text;
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (localName.equals("svg")) {
			try {
				scene.write(scene_name + ".xml");
			} catch (FileNotFoundException | TransformerException e) {
				throw new SAXException(e);
			}
		} else if (localName.equals("rect") || localName.equals("image")) {

			if (localName.equals("image")
					&& (currentActor.width >= width && currentActor.height >= height)) {
				scene.createBackgroundElement(currentActor.filename);
			} else {

				try {
					scene.createActorElement(currentActor.id, currentActor.x,
							currentActor.y, currentActor.width,
							currentActor.height, currentActor.desc,
							currentActor.lookat, currentActor.pickup,
							currentActor.filename);
				} catch (FileNotFoundException | ParserConfigurationException
						| TransformerException e) {
					throw new SAXException(e);
				}
			}

			currentActor = null;
		}

		currentTag = null;
	}

	class Actor {
		String id;
		float x, y, width, height;
		String desc, lookat, pickup;
		String filename;
	}
}
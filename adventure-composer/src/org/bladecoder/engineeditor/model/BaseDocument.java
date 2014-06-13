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
package org.bladecoder.engineeditor.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.bladecoder.engine.util.EngineLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class BaseDocument extends PropertyChange {
	public static final String NOTIFY_ELEMENT_DELETED = "ELEMENT_DELETED";
	public static final String NOTIFY_ELEMENT_CREATED = "ELEMENT_CREATED";
	
	public static final String NOTIFY_DOCUMENT_SAVED = "DOCUMENT_SAVED";

	Document doc;
	private String filename;
	protected String modelPath;

	protected Properties i18n;

	protected boolean modified = false;

	public abstract String getRootTag();

	public void create() throws ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		doc = dBuilder.newDocument();

		Element rootElement = doc.createElement(getRootTag());

		doc.appendChild(rootElement);

		i18n = new Properties();

		modified = true;
		firePropertyChange();
	}

	protected String getI18NFilename() {
		String name = getAbsoluteName();

		return name.substring(0, name.lastIndexOf('.')) + ".properties";
	}

	private void loadI18N() {
		String i18nFilename = getI18NFilename();

		i18n = new Properties();
		try {
			i18n.load(new FileInputStream(i18nFilename));
			;
		} catch (IOException e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nFilename);
		}
	}

	public String getTranslation(String key) {
		if (key.isEmpty() || key.charAt(0) != '@' || i18n == null)
			return key;

		return i18n.getProperty(key.substring(1), key);
	}

	public void setTranslation(String key, String value) {
		i18n.setProperty(key, value);
	}

	private void saveI18N() {
		String i18nFilename = getI18NFilename();

		try {
			i18n.store(new FileOutputStream(i18nFilename), filename);
		} catch (IOException e) {
			EngineLogger.error("ERROR WRITING BUNDLE: " + i18nFilename);
		}
	}

	public void load() throws ParserConfigurationException, SAXException, IOException {
		File fXmlFile = new File(getAbsoluteName());
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(fXmlFile);

		loadI18N();

		modified = false;
	}

	public void save() throws TransformerException, FileNotFoundException {

		if (!modified)
			return;

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new FileOutputStream(getAbsoluteName()));
		transformer.transform(source, result);

		saveI18N();

		modified = false;
		firePropertyChange(NOTIFY_DOCUMENT_SAVED);
	}

	public boolean isModified() {
		return modified;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getAbsoluteName() {
		return modelPath + "/" + filename;
	}
	
	public void setModelPath(String p) {
		modelPath = p;
	}

	public String getRootAttr(String attr) {
		return doc.getDocumentElement().getAttribute(attr);
	}

	public String getElementAttr(String tag, String attr) {
		NodeList nl = doc.getDocumentElement().getElementsByTagName(tag);

		if (nl.getLength() == 0)
			return "";
		else
			return ((Element) nl.item(0)).getAttribute(attr);
	}

	public void setElementAttr(String tag, String attr, String value) {
		NodeList nl = doc.getDocumentElement().getElementsByTagName(tag);

		Element e = null;

		if (nl.getLength() == 0) {
			e = doc.createElement(tag);

			doc.getDocumentElement().appendChild(e);
		} else {
			e = (Element) nl.item(0);
		}

		e.setAttribute(attr, value);
		modified = true;
	}

	public void setModified(boolean b) {
		modified = b;
		firePropertyChange(DOCUMENT_CHANGED, null, doc.getDocumentElement());
	}

	public Element getRootElement() {
		return doc.getDocumentElement();
	}

	public Document getDocument() {
		return doc;
	}

	public Element cloneNode(Element e) {
		Element cloned;

		if (e.getOwnerDocument() != doc) {
			cloned = (Element) doc.importNode(e, true);
		} else {
			cloned = (Element) e.cloneNode(true);
		}

		return cloned;
	}

	public void setModified(Element e) {
		setModified(e.getTagName(), e);
	}

	public void setModified(String property, Element e) {
		modified = true;
		firePropertyChange(property, null, e);
	}

	public String getRootAttr(Element e, String a) {
		return e.getAttribute(a);
	}

	public void setRootAttr(Element e, String attr, String value) {
		String old = e.getAttribute(attr);

		if (value != null && !value.isEmpty())
			e.setAttribute(attr, value);
		else
			e.removeAttribute(attr);

		modified = true;
		firePropertyChange(attr, old, e);
	}
	
	public void setRootAttr(String attr, String value) {
		String old = getRootAttr(getRootElement(), attr);

		if (value != null && !value.isEmpty())
			getRootElement().setAttribute(attr, value);
		else
			getRootElement().removeAttribute(attr);

		modified = true;
		firePropertyChange(attr, old, getRootElement());
	}

	public void deleteElement(Element e) {
		e.getParentNode().removeChild(e);

		modified = true;
		firePropertyChange(NOTIFY_ELEMENT_DELETED, e);
	}

	public Element createVerb(Element e, String id, String state, String target) {
		Element ev = doc.createElement("verb");
		ev.setAttribute("id", id);
		if (state != null && !state.isEmpty())
			ev.setAttribute("state", state);
		if (target != null && !target.isEmpty())
			ev.setAttribute("target", target);

		e.appendChild(ev);

		modified = true;
		firePropertyChange("verb", null, e);

		return ev;
	}

	public Element createAction(Element verb, String action, String actor,
			HashMap<String, String> params) {
		Element e = doc.createElement(action);
		if (actor != null && !actor.isEmpty())
			e.setAttribute("actor", actor);

		if (params != null) {
			for (String k : params.keySet()) {
				String v = params.get(k);
				e.setAttribute(k, v);
			}
		}

		verb.appendChild(e);

		modified = true;
		firePropertyChange("action", null, e);

		return e;
	}

	public String getType(Element e) {
		return e.getAttribute("type");
	}

	public NodeList getVerbs(Element e) {
		// return e.getElementsByTagName("verb");

		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		try {
			return (NodeList) xpath.evaluate("./verb", e, XPathConstants.NODESET);
		} catch (XPathExpressionException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}

		return null;
	}

	public NodeList getChildrenByTag(Element e, String tag) {

		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();

		try {
			return (NodeList) xpath.evaluate("./" + tag, e, XPathConstants.NODESET);
		} catch (XPathExpressionException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}

		return null;
	}

	public Element getElement() {
		return doc.getDocumentElement();
	}

	/**
	 * Sets the element id avoiding duplicated ids
	 * 
	 * @param e
	 * @param id
	 */
	public void setId(Element e, String id) {
		String idChecked = id;

		if (e.getParentNode() instanceof Element) {

			NodeList nl = ((Element) e.getParentNode()).getElementsByTagName(e.getTagName());
			boolean checked = false;

			int i = 1;

			while (!checked) {
				checked = true;

				for (int j = 0; j < nl.getLength(); j++) {
					Element e2 = (Element) nl.item(j);
					if (e2.getAttribute("id").equals(idChecked) && e != e2) {
						i++;
						idChecked = id + i;
						checked = false;
						break;
					}
				}
			}
		}

		setRootAttr(e, "id", idChecked);
	}

	public String getId(Element e) {
		return e.getAttribute("id");
	}

	public Element createElement(Element parent, String type) {
		Element es = doc.createElement(type);
		parent.appendChild(es);

		return es;
	}
}

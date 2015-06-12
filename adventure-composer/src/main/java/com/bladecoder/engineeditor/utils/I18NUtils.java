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

import java.util.ArrayList;
import java.util.Enumeration;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.bladecoder.engineeditor.model.BaseDocument;


public class I18NUtils {
	
	public static boolean mustTraslateAttr(String attrName) {
		if (attrName.equals("desc") || attrName.toLowerCase().endsWith("text")) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Extract the texts for translation to the in the I18N file.
	 * 
	 * Texts are extracted for the next elements:
	 * 
	 * - actor: 'desc' attribute - action: 'text' attribute - option: 'text' and
	 * 'response_text' attributes
	 * 
	 * @param e
	 *            The element to extract texts
	 */
	public static void setI18NAttr(BaseDocument doc, Element e, String attr, String value) {
		if (mustTraslateAttr(attr)) {
			
			// Gen the key only when no key exists
			String key = e.getAttribute(attr);

			if (key == null || key.length() == 0 || key.charAt(0) != BaseDocument.I18NPREFIX
					|| doc.getI18N().getProperty(key.substring(1), null) == null) {
				key = BaseDocument.I18NPREFIX + genI18NKey(doc, e, attr);
			}
			
			// ALWAYS gen the key
//			String key = BaseDocument.I18NPREFIX + genI18NKey(doc, e, attr);

			doc.setTranslation(key, value);
			e.setAttribute(attr, key);
		} else {
			e.setAttribute(attr, value);
		}
	}

	/**
	 * Generates the translation key based in the element type. Does not add the prefix.
	 * 
	 * @param doc
	 * @param e
	 * @param attr
	 */
	public static String genI18NKey(BaseDocument doc, Element e, String attr) {
		String key = null;
		
		if (e.getTagName().equals("actor")) {
			String actorId = e.getAttribute("id");
			String sceneId = ((Element) e.getParentNode()).getAttribute("id");
			key = sceneId + "." + actorId + "." + attr;
		} else if (e.getTagName().equals("option")) {
			String optionId = getDialogOptionId(e);
			
			Element dialog = (Element)e.getParentNode();
			
			while(!dialog.getTagName().equals("dialog")) {
				dialog = (Element)dialog.getParentNode();
			}
			
			String dialogId = dialog.getAttribute("id");
			String actorId = ((Element) dialog.getParentNode()).getAttribute("id");
			String sceneId = ((Element)((Element) dialog.getParentNode()).getParentNode()).getAttribute("id");
			
			key = sceneId + "." + actorId + "." + dialogId + "." + optionId + "." + attr;
		} else 	if (e.getTagName().equals("action")) {
			Element verb = (Element) e.getParentNode();
			String verbId = verb.getAttribute("id");
			String target = verb.getAttribute("target");
			String state = verb.getAttribute("state");

			if (!state.isEmpty())
				verbId = verbId + "." + state;
			if (!target.isEmpty())
				verbId = verbId + "." + target;

			key = verbId;

			NodeList nl = verb.getChildNodes();

			// Find action position inside the verb
			for (int i = 0, c = 0; i < nl.getLength(); i++) {
				if (e == nl.item(i)) {
					key = key + "." + c;
					break;
				}

				if (nl.item(i) instanceof Element)
					c++;
			}

			Element p = (Element) verb.getParentNode();

			if (p.getTagName().equals("actor")) { // ACTOR VERB
				String actorId = p.getAttribute("id");
				String sceneId = ((Element) p.getParentNode()).getAttribute("id");
				key = sceneId + "." + actorId + "." + key + "." + attr;
			} else if (p.getTagName().equals("scene")) { // SCENE VERB
				String sceneId = p.getAttribute("id");
				key = sceneId + "." + key + "." + attr;
			} else { // WORLD VERB
				key = "default." + key + "." + attr;
			}

		}
		
		// Assure that the key is not duplicated
		if(key != null) {
			while(doc.getI18N().getProperty(key, null) != null) {
				key = key + '_';
			}
		}
		
		
		return key;
	}
	

	public static String getDialogOptionId(Element e) {
		String id = null;

		Element currentOption = e;
		Element parent;
		NodeList nl;

		do {
			parent = (Element) currentOption.getParentNode();
			nl = parent.getChildNodes();	
			
			for (int i = 0, c = 0; i < nl.getLength(); i++) {
				if (currentOption == nl.item(i)) {
					id = (id != null ? i + "." + id: Integer.toString(c));
					break;
				}
				
				if(nl.item(i) instanceof Element)
					c++;
				
			}

			currentOption = parent;
		} while (!parent.getTagName().equals("dialog"));

		return id;
	}
	
	public static void putTranslationsInElement(BaseDocument doc, Element e) {
		NamedNodeMap attrs = e.getAttributes();
		
		for(int i = 0; i < attrs.getLength(); i++) {
			if(mustTraslateAttr(attrs.item(i).getNodeName()) && attrs.item(i).getNodeValue().charAt(0) == BaseDocument.I18NPREFIX) {
				String value = doc.getTranslation(attrs.item(i).getNodeValue());
				e.setAttribute(attrs.item(i).getNodeName(), value);
			}
		}
		
		NodeList childs = e.getChildNodes();
		
		for(int i = 0; i < childs.getLength(); i++) {
			if(childs.item(i) instanceof Element)
				putTranslationsInElement(doc, (Element)childs.item(i));
		}		
	}
	
	public static void extractStrings(BaseDocument doc, Element e) {
		NamedNodeMap attrs = e.getAttributes();
		
		for(int i = 0; i < attrs.getLength(); i++) {
			if(mustTraslateAttr(attrs.item(i).getNodeName()) && attrs.item(i).getNodeValue().charAt(0) != BaseDocument.I18NPREFIX) {
				setI18NAttr(doc, e, attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
			}
		}
		
		NodeList childs = e.getChildNodes();
		
		for(int i = 0; i < childs.getLength(); i++) {
			if(childs.item(i) instanceof Element)
				extractStrings(doc, (Element)childs.item(i));
		}
	}
	
	public static void deleteUnusedKeys(BaseDocument doc) {
		ArrayList<String> usedKeys = new ArrayList<String>();
		getUsedKeys(doc.getRootElement(), usedKeys);
		
		Enumeration<Object> keys = doc.getI18N().keys();
		
		while(keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			
			if(!usedKeys.contains(key) && 
					!key.startsWith("ui.")) { // Doesn't remove ui keys
				doc.getI18N().remove(key);
			}
		}
	}
	
	private static void getUsedKeys(Element e, ArrayList<String> usedKeys) {
		NamedNodeMap attrs = e.getAttributes();
		
		for(int i = 0; i < attrs.getLength(); i++) {
			if(attrs.item(i).getNodeValue().length() > 0 && attrs.item(i).getNodeValue().charAt(0) == BaseDocument.I18NPREFIX) {
				usedKeys.add(attrs.item(i).getNodeValue().substring(1));
			}
		}
		
		NodeList childs = e.getChildNodes();
		
		for(int i = 0; i < childs.getLength(); i++) {
			if(childs.item(i) instanceof Element)
				getUsedKeys((Element)childs.item(i), usedKeys);
		}		
	}
}

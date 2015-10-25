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
package com.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engineeditor.utils.I18NUtils;

public abstract class BaseDocument extends PropertyChange {
	public static final String NOTIFY_ELEMENT_DELETED = "ELEMENT_DELETED";
	public static final String NOTIFY_ELEMENT_CREATED = "ELEMENT_CREATED";

	public static final String NOTIFY_DOCUMENT_SAVED = "DOCUMENT_SAVED";

	public static final char I18NPREFIX = '@';

	private String filename;
	protected String modelPath;

	protected Properties i18n;

	protected boolean modified = false;

	@SuppressWarnings("serial")
	public void create() {

		// To save in alphabetical order we override the keys method
		i18n = new Properties() {
			@Override
			public synchronized Enumeration<Object> keys() {
				return Collections.enumeration(new TreeSet<Object>(keySet()));
			}
		};

		modified = true;
		firePropertyChange();
	}

	protected String getI18NFilename() {
		String name = getAbsoluteName();

		return name.substring(0, name.lastIndexOf('.')) + ".properties";
	}

	@SuppressWarnings("serial")
	private void loadI18N() {
		String i18nFilename = getI18NFilename();

		// To save in alphabetical order we override the keys method
		i18n = new Properties() {
			@Override
			public synchronized Enumeration<Object> keys() {
				return Collections.enumeration(new TreeSet<Object>(keySet()));
			}
		};
		
		try {
			i18n.load(new FileInputStream(i18nFilename));
		} catch (IOException e) {
			EngineLogger.error("ERROR LOADING BUNDLE: " + i18nFilename);
		}
	}

	public String getTranslation(String key) {
		if (key == null || key.isEmpty() || key.charAt(0) != I18NPREFIX || i18n == null)
			return key;

		return i18n.getProperty(key.substring(1), key);
	}

	public void setTranslation(String key, String value) {
		if (key.charAt(0) != I18NPREFIX)
			i18n.setProperty(key, value);
		else
			i18n.setProperty(key.substring(1), value);
	}

	public Properties getI18N() {
		return i18n;
	}

	private void saveI18N() {
		String i18nFilename = getI18NFilename();
		
		I18NUtils.deleteUnusedKeys(this);

		try {
			FileOutputStream os = new FileOutputStream(i18nFilename);
			Writer out = new OutputStreamWriter(os, "ISO-8859-1");
			i18n.store(out, filename);
		} catch (IOException e) {
			EngineLogger.error("ERROR WRITING BUNDLE: " + i18nFilename);
		}
	}

	public void load() {

		loadI18N();

		modified = false;
	}

	public void save() throws FileNotFoundException {

		if (!modified)
			return;

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

	public void setModified() {
		modified = true;
		firePropertyChange(DOCUMENT_CHANGED, null, null);
	}

//	public Element cloneNode(Element parent, Element e) {
//		Element cloned;
//
//		if (e.getOwnerDocument() != doc) {
//			cloned = (Element) doc.importNode(e, true);
//		} else {
//			cloned = (Element) e.cloneNode(true);
//		}
//
//		parent.appendChild(cloned);
//
//		if (cloned.getAttribute("id") != null && !cloned.getAttribute("id").isEmpty()) {
//			cloned.setAttribute("id", getCheckedId(cloned, cloned.getAttribute("id")));
//		}
//
//		setModified(cloned);
//
//		return cloned;
//	}

	public void setModified(Object e) {
		setModified(e, e);
	}
	
	public void setModified(Object e, Object source) {
		modified = true;
		PropertyChangeEvent evt = new PropertyChangeEvent(source, e.toString(), null, e);
		firePropertyChange(evt);
	}

	public void setModified(String property, Object e) {
		modified = true;
		firePropertyChange(property, null, e);
	}



	/**
	 * Sets the element id avoiding duplicated ids
	 * 
	 * @param e
	 * @param id
	 */
//	public void setId(Element e, String id) {
//		String idChecked = getCheckedId(e, id);
//
//		setRootAttr(e, "id", idChecked);
//	}
//
//	public String getCheckedId(Element e, String id) {
//		String idChecked = id;
//
//		if (e.getParentNode() instanceof Element) {
//
//			NodeList nl = ((Element) e.getParentNode()).getElementsByTagName(e.getTagName());
//			boolean checked = false;
//
//			int i = 1;
//
//			while (!checked) {
//				checked = true;
//
//				for (int j = 0; j < nl.getLength(); j++) {
//					Element e2 = (Element) nl.item(j);
//					if (e2.getAttribute("id").equals(idChecked) && e != e2) {
//						i++;
//						idChecked = id + i;
//						checked = false;
//						break;
//					}
//				}
//			}
//		}
//
//		return idChecked;
//	}
}

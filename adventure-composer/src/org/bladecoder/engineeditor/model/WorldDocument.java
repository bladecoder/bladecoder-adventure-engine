package org.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WorldDocument extends  BaseDocument {
	public static final int DEFAULT_WIDTH = 1920;
	public static final int DEFAULT_HEIGHT = 1080;
	
	public static final String NOTIFY_DOCUMENT_MODIFIED = "DOCUMENT_MODIFIED";
	
    private PropertyChangeListener documentModifiedListener = new PropertyChangeListener() {	
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
//			if(!evt.getPropertyName().equals(NOTIFY_DOCUMENT_MODIFIED))
			firePropertyChange(evt);
			EditorLogger.debug("WorldDocument Listener: " +  evt.getPropertyName());
		}
	};
	
	public WorldDocument() {
		setFilename("world.xml");
	}
	
	@Override
	public String getRootTag() {
		return "world";
	}

	@Override
	public void create() throws ParserConfigurationException {
		super.create();
		
		setDimensions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}
	
	public void setDimensions(int width, int height) {
		doc.getDocumentElement().setAttribute("width", Integer.toString(width));
		doc.getDocumentElement().setAttribute("height", Integer.toString(height));
		modified = true;
		firePropertyChange();
	}
	
	public NodeList getChapters() {
		 return doc.getDocumentElement().getElementsByTagName("chapter");
	}

	public int getWidth() {
		return Integer.parseInt(doc.getDocumentElement().getAttribute("width"));
	}
	
	public int getHeight() {
		return  Integer.parseInt(doc.getDocumentElement().getAttribute("height"));		
	}

	public void setWidth(String value) {
		doc.getDocumentElement().setAttribute("width", value);
		modified = true;
		firePropertyChange();
	}
	
	public void setHeight(String value) {
		doc.getDocumentElement().setAttribute("height", value);
		modified = true;
		firePropertyChange();
	}
	
	public ChapterDocument loadChapter(String id) throws ParserConfigurationException, SAXException, IOException {
			ChapterDocument chapter = new ChapterDocument(modelPath);
			chapter.setFilename(id + ".chapter");
			chapter.load();
			chapter.addPropertyChangeListener(documentModifiedListener);
			
			return chapter;
	}
	
	public String getInitChapter() {
		String init = getRootAttr("init_chapter");
		
		if(init == null || init.isEmpty()) {
			init = ((Element)getChapters().item(0)).getAttribute("id");			
		}
		
		return init;
	}
	
	public void addChapterNode(String id) {
		Element e = doc.createElement("chapter");
		
		doc.getDocumentElement().appendChild(e);
		e.setAttribute("id", id);
		modified = true;
		firePropertyChange();
	}

	public void removeChapterNode(String id) {
		
		NodeList chaptersNL = getChapters();
		for (int j = 0; j < chaptersNL.getLength(); j++) {
			Element e = (Element) chaptersNL.item(j);
			if (id.equals(e.getAttribute("id"))) {
				doc.getDocumentElement().removeChild(e);
			}
		}
	}
	
	public ChapterDocument createChapter(String id) throws FileNotFoundException, TransformerException, ParserConfigurationException {
		ChapterDocument chapter = new ChapterDocument(modelPath);	
		String checkedId = getChapterCheckedId(id);
		
		chapter.create(checkedId);

		addChapterNode(id);
		save();
		
		chapter.addPropertyChangeListener(documentModifiedListener);
		
		firePropertyChange();

		return chapter;
	}
	
	public String getChapterCheckedId(String id) {
		boolean checked = false;
		int i = 1;
		
		String idChecked = id;
		
		NodeList nl = getRootElement().getElementsByTagName("chapter");

		while (!checked) {
			checked = true;

			for (int j = 0; j < nl.getLength(); j++) {
				String id2 = ((Element)nl.item(j)).getAttribute("id");
						
				if (id2.equals(idChecked)) {
					i++;
					idChecked = id + i;
					checked = false;
					break;
				}
			}
		}
		
		return idChecked;
	}
		
	// TODO: Reload current chapter if the renamed chapter is the current chapter
	public void renameChapter(String id) throws FileNotFoundException, TransformerException, ParserConfigurationException {
		
		removeChapterNode(id);
		String checkedId = getChapterCheckedId(id);
		
		// TODO chapter.rename(checkedId);

		addChapterNode(id);
		save();
		
		firePropertyChange();
	}
	
	public void removeChapter(String id) throws FileNotFoundException, TransformerException {
		removeChapterNode(id);
		// TODO chapter.deleteFiles();
		save();
		
		firePropertyChange();
	}
}

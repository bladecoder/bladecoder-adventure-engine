package org.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

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
	
	private HashMap<String, SceneDocument> scenes = new HashMap<String, SceneDocument>();
	
	private Element currentChapter;
	
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
	
	public void setCurrentChapter(String chapter) throws ParserConfigurationException, SAXException, IOException {
		NodeList nl = doc.getDocumentElement().getElementsByTagName("chapter");
		
		for(int i = 0; i < nl.getLength(); i++) {
			Element e =  (Element) nl.item(i);
			String id = e.getAttribute("id");
			
			if(id.equals(chapter)) {
				setCurrentChapter(e);
				return;
			}
		}
		
		if(nl.getLength() > 0) {
			setCurrentChapter((Element) nl.item(0));		
		}
	}
	
	public void setCurrentChapter(Element e) throws ParserConfigurationException, SAXException, IOException {
		currentChapter = e;
		
		loadCurrentChapterScenes();
	}
	
	public Element getCurrentChapter() {
		return currentChapter;
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

	public NodeList getSceneNodes() {
		return currentChapter.getElementsByTagName("scene");
	}
	
	public SceneDocument getScene(String id) {
		return scenes.get(id);
	}
	
	public HashMap<String, SceneDocument> getSceneMap() {
		return scenes;
	}
	
	@Override
	public void load() throws ParserConfigurationException, SAXException, IOException {
		super.load();
		
		String chapterId = getRootAttr("init_chapter");
		
		setCurrentChapter(chapterId);		
	}
	
	public void loadCurrentChapterScenes() throws ParserConfigurationException, SAXException, IOException {
		scenes.clear();
		
		NodeList nl = getSceneNodes();

		for (int i = 0; i < nl.getLength(); i++) {
			String filename = ((Element) nl.item(i)).getAttribute("filename");
			SceneDocument scn = new SceneDocument(modelPath);
			scn.setFilename(filename);
			scn.load();
			scenes.put(scn.getId(), scn);
			scn.addPropertyChangeListener(documentModifiedListener);
		}		
	}
	
	public String getInitScene() {
		String init = getRootAttr(currentChapter, "init_scene");
		
		if(init == null || init.isEmpty()) {
			String filename = ((Element)getSceneNodes().item(0)).getAttribute("filename");
					
			init = 	getSceneByFilename(filename).getId();				
		}
		
		return init;
	}
	
	public void setInitScene(String id) {
		setRootAttr(currentChapter, "init_scene", id);
	}
	
	public String getInitChapter() {
		String init = getRootAttr("init_chapter");
		
		if(init == null || init.isEmpty()) {
			((Element)getChapters().item(0)).getAttribute("id");			
		}
		
		return init;
	}
	
	public SceneDocument getSceneByFilename(String filename) {
		for(SceneDocument scn:scenes.values()) {
			if(scn.getFilename().equals(filename))
				return scn;
		}
			
		return null;
	}

	public void addSceneNode(SceneDocument scn) {
		Element e = doc.createElement("scene");
		
		currentChapter.appendChild(e);
		e.setAttribute("filename", scn.getFilename());
		modified = true;
		firePropertyChange();
	}

	public void removeSceneNode(SceneDocument scn) {
		NodeList scenes = getSceneNodes();
		String filename = scn.getFilename();
		
		for(int i = 0; i < scenes.getLength(); i++) {
			
			if(scenes.item(i) instanceof Element) {
				Element e = (Element) scenes.item(i);
				
				if(e.getAttribute("filename").equals(filename)) {
					currentChapter.removeChild(e);
					modified = true;
					firePropertyChange();	
					return;
				}
			}
		}
	}
	
	public SceneDocument createScene(String id) throws FileNotFoundException, TransformerException, ParserConfigurationException {
		SceneDocument scn = new SceneDocument(modelPath);	
		String checkedId = getSceneCheckedId(id);
		
		scn.create(checkedId);

		getSceneMap().put(checkedId, scn);
		addSceneNode(scn);
		save();
		
		scn.addPropertyChangeListener(documentModifiedListener);
		
		firePropertyChange();

		return scn;
	}
	
	public String getSceneCheckedId(String id) {
		boolean checked = false;
		int i = 1;
		
		String idChecked = id;
		
		NodeList nl = getRootElement().getElementsByTagName("scene");

		while (!checked) {
			checked = true;

			for (int j = 0; j < nl.getLength(); j++) {
				String filename = ((Element)nl.item(j)).getAttribute("filename");
				
				String id2 = filename.substring(0, filename.lastIndexOf('.'));
				
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
	
	public String getSceneCheckedIdOLD(String id) {
		boolean checked = false;
		int i = 1;
		
		String idChecked = id;

		while (!checked) {
			checked = true;

			for (SceneDocument s:scenes.values()) {
				String id2 = s.getId();
				
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
	
	public void renameScene(SceneDocument scn, String id) throws FileNotFoundException, TransformerException, ParserConfigurationException {
		
		getSceneMap().remove(scn.getId());
		removeSceneNode(scn);
		String checkedId = getSceneCheckedId(id);
		
		scn.rename(checkedId);

		getSceneMap().put(checkedId, scn);
		addSceneNode(scn);
		save();
		
		firePropertyChange();
	}
	
	public void removeScene(SceneDocument scn) throws FileNotFoundException, TransformerException {
		getSceneMap().remove(scn.getId());
		removeSceneNode(scn);
		scn.deleteFiles();
		save();
		
		firePropertyChange();
	}
	
	public void saveAll() throws FileNotFoundException, TransformerException {
		save();
		
		for (SceneDocument s:scenes.values()) {
			s.save();
		}
	}
}

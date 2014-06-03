package org.bladecoder.engineeditor.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.bladecoder.engineeditor.Ctx;
import org.bladecoder.engineeditor.utils.EditorLogger;
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
	
	public String[] getChapters() {
		String dir = Ctx.project.getProjectPath() + Project.MODEL_PATH;
		
		String[] chapters = new File(dir).list(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				if (!arg1.endsWith(".chapter"))
					return false;

				return true;
			}
		});
		
		for(int i = 0; i < chapters.length; i++)
			chapters[i] = chapters[i].substring(0, chapters[i].lastIndexOf('.'));
		
		return chapters;
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
			init = getChapters()[0];
			
			setRootAttr("init_chapter", init);
		}
		
		return init;
	}
	
	public void setInitChapter(String value) {
		doc.getDocumentElement().setAttribute("init_chapter", value);
		modified = true;
		firePropertyChange();
	}
	
	public ChapterDocument createChapter(String id) throws FileNotFoundException, TransformerException, ParserConfigurationException {
		ChapterDocument chapter = new ChapterDocument(modelPath);	
		String checkedId = getChapterCheckedId(id);
		
		chapter.create(checkedId);
		firePropertyChange("chapter");
		
		return chapter;
	}
	
	public String getChapterCheckedId(String id) {
		boolean checked = false;
		int i = 1;
		
		String idChecked = id;
		
		String [] nl = getChapters();

		while (!checked) {
			checked = true;

			for (int j = 0; j < nl.length; j++) {
				String id2 = nl[j];
						
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
		
	public void renameChapter(String oldId, String newId) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		
		ChapterDocument chapter = new ChapterDocument(modelPath);
		chapter.setFilename(oldId + ".chapter");
		chapter.load();
		chapter.rename(newId);
		firePropertyChange("chapter");
	}
	
	public void removeChapter(String id) throws FileNotFoundException, TransformerException {
		
		ChapterDocument chapter = new ChapterDocument(modelPath);
		chapter.setFilename(id + ".chapter");
		chapter.deleteFiles();
		firePropertyChange("chapter");
	}
}

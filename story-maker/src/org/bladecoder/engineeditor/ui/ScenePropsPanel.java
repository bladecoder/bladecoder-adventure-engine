package org.bladecoder.engineeditor.ui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.components.PropertyTable;
import org.bladecoder.engineeditor.ui.components.PropertyTable.PropertyTableModel;
import org.bladecoder.engineeditor.ui.components.PropertyTable.Types;
import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class ScenePropsPanel extends JPanel {
	public static final String MUSIC_PROP = "Music filename";
	public static final String LOOP_MUSIC_PROP = "Loop Music";
	public static final String BACKGROUND_PROP = "Background";
	public static final String LIGHTMAP_PROP = "Light Map";
	public static final String INITIAL_MUSIC_DELAY_PROP = "Initial Music Delay";
	public static final String REPEAT_MUSIC_DELAY_PROP = "Repeat Music Delay";
	
	private PropertyTable propertyTable;
	
	ChapterDocument doc;
	Element scn;
	
	TableModelListener tableModelListener = new TableModelListener() {
		@Override
		public void tableChanged(TableModelEvent e) {
			if(e.getType() == TableModelEvent.UPDATE) {
				int row = e.getFirstRow();
				updateModel((String) propertyTable.getModel()
					.getValueAt(row, 0), (String) propertyTable
					.getModel().getValueAt(row, 1));
			}
		}
	};
	
	PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {			
			setSceneDocument(doc, scn);			
		}
	};

	public ScenePropsPanel() {

		propertyTable = new PropertyTable();
		setLayout(new BorderLayout(0, 0));
		add(propertyTable, java.awt.BorderLayout.CENTER);
		
		PropertyTableModel model = (PropertyTableModel) propertyTable.getModel();
		model.addTableModelListener(tableModelListener);
	}

	public void setSceneDocument(ChapterDocument doc, Element scn) {

		this.scn = scn;
		this.doc = doc;
		
		PropertyTableModel model = (PropertyTableModel) propertyTable
				.getModel();
		
		model.clear();

		if (scn != null) {
			model.addProperty(BACKGROUND_PROP, doc.getBackground(scn));
			model.addProperty(LIGHTMAP_PROP, doc.getLightmap(scn));
			model.addProperty(MUSIC_PROP, doc.getMusic(scn));
			model.addProperty(LOOP_MUSIC_PROP, doc.getRootAttr(scn,"loop_music"), Types.BOOLEAN);
			model.addProperty(INITIAL_MUSIC_DELAY_PROP, doc.getRootAttr(scn,"initial_music_delay"), Types.FLOAT);
			model.addProperty(REPEAT_MUSIC_DELAY_PROP, doc.getRootAttr(scn,"repeat_music_delay"), Types.FLOAT);
			
			this.doc.addPropertyChangeListener("scene", propertyChangeListener);
		}	
	}

	private void updateModel(String property, String value) {
		if (property.equals(MUSIC_PROP)) {
			doc.setRootAttr(scn,"music", value);
		} else if (property.equals(LOOP_MUSIC_PROP)) {
			doc.setRootAttr(scn,"loop_music", value);
		} else if (property.equals(BACKGROUND_PROP)) {
			doc.setBackground(scn,value);
		} else if (property.equals(LIGHTMAP_PROP)) {
			doc.setLightmap(scn,value);			
		} else if (property.equals(INITIAL_MUSIC_DELAY_PROP)) {
			doc.setRootAttr(scn,"initial_music_delay", value);
		} else if (property.equals(REPEAT_MUSIC_DELAY_PROP)) {
			doc.setRootAttr(scn,"repeat_music_delay", value);
		}
	}
}

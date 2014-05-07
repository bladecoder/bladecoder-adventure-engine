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
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

@SuppressWarnings("serial")
public class ActorPropsPanel extends JPanel {

	public static final String DESC_PROP = "Description";
	public static final String BBOX_X_PROP = "bbox x";
	public static final String BBOX_Y_PROP = "bbox y";
	public static final String BBOX_WIDTH_PROP = "bbox width";
	public static final String BBOX_HEIGHT_PROP = "bbox height";
	public static final String POS_X_PROP = "pos X";
	public static final String POS_Y_PROP = "pos Y";
	public static final String INTERACTION_PROP = "interaction";
	public static final String VISIBLE_PROP = "visible";
	public static final String ACTIVE_PROP = "active";
	public static final String STATE_PROP = "state";
	public static final String WALKING_SPEED_PROP = "Walking Speed";

	private PropertyTable propertyTable;
	private ChapterDocument doc;
	private Element actor;

	TableModelListener tableModelListener = new TableModelListener() {
		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getType() == TableModelEvent.UPDATE) {
				int row = e.getFirstRow();
				updateModel((String) propertyTable.getModel().getValueAt(row, 0),
						(String) propertyTable.getModel().getValueAt(row, 1));
			}
		}
	};
	
	PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			EditorLogger.debug("Property Listener: " + evt.getPropertyName());
			setActorDocument(doc, actor);			
		}
	};

	public ActorPropsPanel() {
		propertyTable = new PropertyTable();
		setLayout(new BorderLayout(0, 0));
		add(propertyTable, java.awt.BorderLayout.CENTER);
		
		PropertyTableModel model = (PropertyTableModel) propertyTable.getModel();
		model.addTableModelListener(tableModelListener);
	}

	public void setActorDocument(ChapterDocument doc, Element a) {		
		this.doc = doc;
		this.actor = a;
		PropertyTableModel model = (PropertyTableModel) propertyTable.getModel();
		model.clear();

		if (a != null) {
			model.addProperty(BBOX_X_PROP, doc.getRootAttr(a, "x"), Types.FLOAT);
			model.addProperty(BBOX_Y_PROP, doc.getRootAttr(a, "y"), Types.FLOAT);
			model.addProperty(BBOX_WIDTH_PROP, doc.getRootAttr(a, "width"), Types.FLOAT);
			model.addProperty(BBOX_HEIGHT_PROP, doc.getRootAttr(a, "height"), Types.FLOAT);

			if (!a.getAttribute("type").equals("background")) {
				Vector2 pos = doc.getPos(a);
				model.addProperty(POS_X_PROP, Float.toString(pos.x), Types.FLOAT);
				model.addProperty(POS_Y_PROP, Float.toString(pos.y), Types.FLOAT);
			}

			model.addProperty(DESC_PROP, doc.getRootAttr(a, "desc"));

			model.addProperty(INTERACTION_PROP, doc.getRootAttr(a, "interaction"), Types.BOOLEAN);
			model.addProperty(VISIBLE_PROP, doc.getRootAttr(a, "visible"), Types.BOOLEAN);

			model.addProperty(ACTIVE_PROP, doc.getRootAttr(a, "active"), Types.BOOLEAN);
			model.addProperty(STATE_PROP, doc.getRootAttr(a, "state"));
			
			
			if (a.getAttribute("type").equals("player") || 
				a.getAttribute("type").equals("character")) {
				model.addProperty(WALKING_SPEED_PROP, doc.getRootAttr(a, "walking_speed"));
			}
			
			doc.addPropertyChangeListener(propertyChangeListener);
			propertyTable.repaint();
		}
	}

	private void updateModel(String property, String value) {
		if (property.equals(BBOX_X_PROP)) {
			Rectangle bbox = doc.getBBox(actor);
			bbox.x = Float.parseFloat(value);
			doc.setBbox(actor, bbox);
		} else if (property.equals(BBOX_Y_PROP)) {
			Rectangle bbox = doc.getBBox(actor);
			bbox.y = Float.parseFloat(value);
			doc.setBbox(actor, bbox);
		} else if (property.equals(BBOX_WIDTH_PROP)) {
			Rectangle bbox = doc.getBBox(actor);
			bbox.width = Float.parseFloat(value);
			doc.setBbox(actor, bbox);
		} else if (property.equals(BBOX_HEIGHT_PROP)) {
			Rectangle bbox = doc.getBBox(actor);
			bbox.height = Float.parseFloat(value);
			doc.setBbox(actor, bbox);
		} else if (property.equals(DESC_PROP)) {
			doc.setRootAttr(actor, "desc", value);
		} else if (property.equals(POS_X_PROP)) {
			Vector2 pos = doc.getPos(actor);
			pos.x = Float.parseFloat(value);
			doc.setPos(actor, pos);
		} else if (property.equals(POS_Y_PROP)) {
			Vector2 pos = doc.getPos(actor);
			pos.y = Float.parseFloat(value);
			doc.setPos(actor, pos);
		} else if (property.equals(INTERACTION_PROP)) {
			doc.setRootAttr(actor, "interaction", value);
		} else if (property.equals(VISIBLE_PROP)) {
			doc.setRootAttr(actor, "visible", value);
		} else if (property.equals(ACTIVE_PROP)) {
			doc.setRootAttr(actor, "active", value);
		} else if (property.equals(STATE_PROP)) {
			doc.setRootAttr(actor, "state", value);
		} else if (property.equals(WALKING_SPEED_PROP)) {
			doc.setRootAttr(actor, "walking_speed", value);			
		}

	}
}

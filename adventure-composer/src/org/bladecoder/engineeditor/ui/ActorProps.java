package org.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.bladecoder.engineeditor.model.ChapterDocument;
import org.bladecoder.engineeditor.ui.components.PropertyTable;
import org.bladecoder.engineeditor.utils.EditorLogger;
import org.w3c.dom.Element;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class ActorProps extends PropertyTable {

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

	private ChapterDocument doc;
	private Element actor;

//	TableModelListener tableModelListener = new TableModelListener() {
//		@Override
//		public void tableChanged(TableModelEvent e) {
//			if (e.getType() == TableModelEvent.UPDATE) {
//				int row = e.getFirstRow();
//				updateModel((String) propertyTable.getModel().getValueAt(row, 0),
//						(String) propertyTable.getModel().getValueAt(row, 1));
//			}
//		}
//	};
	
	PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			EditorLogger.debug("Property Listener: " + evt.getPropertyName());
			setActorDocument(doc, actor);			
		}
	};

	public ActorProps(Skin skin) {
		super(skin);
	}

	public void setActorDocument(ChapterDocument doc, Element a) {		
		this.doc = doc;
		this.actor = a;
		clear();

		if (a != null) {
			addProperty(BBOX_X_PROP, doc.getRootAttr(a, "x"), Types.FLOAT);
			addProperty(BBOX_Y_PROP, doc.getRootAttr(a, "y"), Types.FLOAT);
			addProperty(BBOX_WIDTH_PROP, doc.getRootAttr(a, "width"), Types.FLOAT);
			addProperty(BBOX_HEIGHT_PROP, doc.getRootAttr(a, "height"), Types.FLOAT);

			if (!a.getAttribute("type").equals("background")) {
				Vector2 pos = doc.getPos(a);
				addProperty(POS_X_PROP, Float.toString(pos.x), Types.FLOAT);
				addProperty(POS_Y_PROP, Float.toString(pos.y), Types.FLOAT);
			}

			addProperty(DESC_PROP, doc.getRootAttr(a, "desc"));

			addProperty(INTERACTION_PROP, doc.getRootAttr(a, "interaction"), Types.BOOLEAN);
			addProperty(VISIBLE_PROP, doc.getRootAttr(a, "visible"), Types.BOOLEAN);

			addProperty(ACTIVE_PROP, doc.getRootAttr(a, "active"), Types.BOOLEAN);
			addProperty(STATE_PROP, doc.getRootAttr(a, "state"));
			
			
			if (a.getAttribute("type").equals("player") || 
				a.getAttribute("type").equals("character")) {
				addProperty(WALKING_SPEED_PROP, doc.getRootAttr(a, "walking_speed"));
			}
			
			doc.addPropertyChangeListener(propertyChangeListener);
			
			invalidateHierarchy();
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

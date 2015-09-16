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
package com.bladecoder.engineeditor.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.w3c.dom.Element;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.loader.XMLConstants;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.ChapterDocument;
import com.bladecoder.engineeditor.ui.components.PropertyTable;
import com.bladecoder.engineeditor.undo.UndoOp;
import com.bladecoder.engineeditor.undo.UndoSetAttr;
import com.bladecoder.engineeditor.utils.EditorLogger;
import com.eclipsesource.json.ParseException;

public class ActorProps extends PropertyTable {

	public static final String DESC_PROP = "Description";
	public static final String POS_X_PROP = "pos X";
	public static final String POS_Y_PROP = "pos Y";
	public static final String VISIBLE_PROP = XMLConstants.VISIBLE_ATTR;
	public static final String INTERACTION_PROP = XMLConstants.INTERACTION_ATTR;
	public static final String STATE_PROP = XMLConstants.STATE_ATTR;
	public static final String BBOX_FROM_RENDERER_PROP = "Set BBOX from renderer";

	private ChapterDocument doc;
	private Element actor;

	// TableModelListener tableModelListener = new TableModelListener() {
	// @Override
	// public void tableChanged(TableModelEvent e) {
	// if (e.getType() == TableModelEvent.UPDATE) {
	// int row = e.getFirstRow();
	// updateModel((String) propertyTable.getModel().getValueAt(row, 0),
	// (String) propertyTable.getModel().getValueAt(row, 1));
	// }
	// }
	// };

	PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			EditorLogger.debug("Actor Props Listener: " + evt.getPropertyName());
			
			updateField(evt.getPropertyName());
		}
	};

	public ActorProps(Skin skin) {
		super(skin);
	}
	
	private void updateField(String modelProperty) {
		
		String value = doc.getRootAttr(actor, modelProperty);
		
		if (modelProperty.equals(XMLConstants.DESC_ATTR)) {
			setProperty(DESC_PROP, value);
		} else if (modelProperty.equals(XMLConstants.POS_ATTR)) {
			Vector2 pos = doc.getPos(actor);
			
			setProperty(POS_X_PROP, Float.toString(pos.x));			
			setProperty(POS_Y_PROP, Float.toString(pos.y));
		} else if (modelProperty.equals(XMLConstants.VISIBLE_ATTR)) {
			setProperty(VISIBLE_PROP, value);
		} else if (modelProperty.equals(XMLConstants.INTERACTION_ATTR)) {
			setProperty(INTERACTION_PROP, value);
		} else if (modelProperty.equals(XMLConstants.STATE_ATTR)) {
			setProperty(STATE_PROP, value);
		} else if (modelProperty.equals(XMLConstants.BBOX_ATTR)) {
			boolean v = value.isEmpty();
			
//			setProperty(BBOX_FROM_RENDERER_PROP, Boolean.toString(v));
		}
	}

	public void setActorDocument(ChapterDocument doc, Element a) {
		this.doc = doc;
		this.actor = a;
		clearProps();

		if (a != null) {

			Vector2 pos = doc.getPos(a);
			addProperty(POS_X_PROP, Float.toString(pos.x), Types.FLOAT);
			addProperty(POS_Y_PROP, Float.toString(pos.y), Types.FLOAT);
			addProperty(VISIBLE_PROP, doc.getRootAttr(a, XMLConstants.VISIBLE_ATTR), Types.BOOLEAN);

			if (!a.getAttribute(XMLConstants.TYPE_ATTR).equals(XMLConstants.OBSTACLE_VALUE)) {
				addProperty(DESC_PROP, doc.getRootAttr(a, XMLConstants.DESC_ATTR));

				addProperty(INTERACTION_PROP, doc.getRootAttr(a, XMLConstants.INTERACTION_ATTR), Types.BOOLEAN);
				addProperty(STATE_PROP, doc.getRootAttr(a, XMLConstants.STATE_ATTR));
			}
			
			if (a.getAttribute(XMLConstants.TYPE_ATTR).equals(XMLConstants.SPRITE_VALUE)) {
				boolean v = doc.getRootAttr(a, XMLConstants.BBOX_ATTR).isEmpty();
				
				addProperty(BBOX_FROM_RENDERER_PROP, Boolean.toString(v), Types.BOOLEAN);
			}

			doc.addPropertyChangeListener(propertyChangeListener);

			invalidateHierarchy();
		}
	}

	@Override
	protected void updateModel(String property, String value) {
		if (property.equals(DESC_PROP)) {
			doc.setRootAttr(actor, XMLConstants.DESC_ATTR, value);
		} else if (property.equals(POS_X_PROP)) {
			Vector2 pos = doc.getPos(actor);
			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), XMLConstants.POS_ATTR,
					Param.toStringParam(pos));
			Ctx.project.getUndoStack().add(undoOp);

			try {
				pos.x = Float.parseFloat(value);
			} catch (NumberFormatException e) {

			}
			doc.setPos(actor, pos);
		} else if (property.equals(POS_Y_PROP)) {
			Vector2 pos = doc.getPos(actor);
			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(),XMLConstants.POS_ATTR,
					Param.toStringParam(pos));
			Ctx.project.getUndoStack().add(undoOp);
			try {
				pos.y = Float.parseFloat(value);
			} catch (NumberFormatException e) {

			}

			doc.setPos(actor, pos);
		} else if (property.equals(VISIBLE_PROP)) {
			doc.setRootAttr(actor, XMLConstants.VISIBLE_ATTR, value);
		} else if (property.equals(INTERACTION_PROP)) {
			doc.setRootAttr(actor, XMLConstants.INTERACTION_ATTR, value);
		} else if (property.equals(STATE_PROP)) {
			doc.setRootAttr(actor,  XMLConstants.STATE_ATTR, value);
		} else if (property.equals(BBOX_FROM_RENDERER_PROP)) {
			boolean v = true;
			
			try {
				v = Boolean.parseBoolean(value);
			} catch(ParseException e) {
				
			}
			
			if(!v) {
				doc.setBbox(actor, null); // TODO get image size
			} else { 
				doc.setRootAttr(actor, XMLConstants.BBOX_ATTR, null);
			}
		}

	}
}

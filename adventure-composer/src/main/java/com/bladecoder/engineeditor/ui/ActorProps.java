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
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.ChapterDocument;
import com.bladecoder.engineeditor.ui.components.PropertyTable;
import com.bladecoder.engineeditor.undo.UndoOp;
import com.bladecoder.engineeditor.undo.UndoSetAttr;
import com.bladecoder.engineeditor.utils.EditorLogger;

public class ActorProps extends PropertyTable {

	public static final String DESC_PROP = "Description";
	public static final String POS_X_PROP = "pos X";
	public static final String POS_Y_PROP = "pos Y";
	public static final String VISIBLE_PROP = "visible";
	public static final String ACTIVE_PROP = "active";
	public static final String STATE_PROP = "state";

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
		clearProps();

		if (a != null) {

			Vector2 pos = doc.getPos(a);
			addProperty(POS_X_PROP, Float.toString(pos.x), Types.FLOAT);
			addProperty(POS_Y_PROP, Float.toString(pos.y), Types.FLOAT);

			addProperty(DESC_PROP, doc.getRootAttr(a, "desc"));

			addProperty(VISIBLE_PROP, doc.getRootAttr(a, "visible"), Types.BOOLEAN);

			addProperty(ACTIVE_PROP, doc.getRootAttr(a, "active"), Types.BOOLEAN);
			addProperty(STATE_PROP, doc.getRootAttr(a, "state"));
			
			doc.addPropertyChangeListener(propertyChangeListener);
			
			invalidateHierarchy();
		}
	}

	@Override
	protected void updateModel(String property, String value) {
		if (property.equals(DESC_PROP)) {
			doc.setRootAttr(actor, "desc", value);
		} else if (property.equals(POS_X_PROP)) {
			Vector2 pos = doc.getPos(actor);
			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), "pos",
					Param.toStringParam(pos));
			Ctx.project.getUndoStack().add(undoOp);				
			
			pos.x = Float.parseFloat(value);
			doc.setPos(actor, pos);		
		} else if (property.equals(POS_Y_PROP)) {
			Vector2 pos = doc.getPos(actor);
			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), "pos",
					Param.toStringParam(pos));
			Ctx.project.getUndoStack().add(undoOp);					
			
			pos.y = Float.parseFloat(value);
			doc.setPos(actor, pos);	
		} else if (property.equals(VISIBLE_PROP)) {
			doc.setRootAttr(actor, "visible", value);
		} else if (property.equals(ACTIVE_PROP)) {
			doc.setRootAttr(actor, "active", value);
		} else if (property.equals(STATE_PROP)) {
			doc.setRootAttr(actor, "state", value);
		}

	}
}

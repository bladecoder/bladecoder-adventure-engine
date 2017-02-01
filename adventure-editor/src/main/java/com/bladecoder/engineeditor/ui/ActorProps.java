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

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.model.Project;
import com.bladecoder.engineeditor.ui.panels.PropertyTable;
import com.eclipsesource.json.ParseException;

public class ActorProps extends PropertyTable {

	public static final String DESC_PROP = "Description";
	public static final String POS_X_PROP = "pos X";
	public static final String POS_Y_PROP = "pos Y";
	public static final String VISIBLE_PROP = "visible";
	public static final String INTERACTION_PROP = "interaction";
	public static final String STATE_PROP = "state";
	public static final String BBOX_FROM_RENDERER_PROP = "Set BBOX from renderer";

	private BaseActor actor;

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
		
		if(actor==null)
			return;
		
		if (modelProperty.equals("desc")) {
			setProperty(DESC_PROP, ((InteractiveActor)actor).getDesc());
		} else if (modelProperty.equals(Project.POSITION_PROPERTY)) {
			setProperty(POS_X_PROP, Float.toString(actor.getX()));			
			setProperty(POS_Y_PROP, Float.toString(actor.getY()));
		} else if (modelProperty.equals("visible")) {
			setProperty(VISIBLE_PROP, Boolean.toString(actor.isVisible()));
		} else if (modelProperty.equals("interaction")) {
			setProperty(INTERACTION_PROP, Boolean.toString(((InteractiveActor)actor).getInteraction()));
		} else if (modelProperty.equals("state")) {
			setProperty(STATE_PROP, ((InteractiveActor)actor).getState());
		} else if (modelProperty.equals("bbox")) {
			
			// TODO Conflict with scnwidget
			
//			boolean v = value.isEmpty();
			
//			setProperty(BBOX_FROM_RENDERER_PROP, Boolean.toString(v));
		}
	}

	public void setActorDocument(BaseActor a) {
		this.actor = a;
		clearProps();

		if (a != null) {
			addProperty(POS_X_PROP,  Float.toString(actor.getX()), Types.FLOAT);
			addProperty(POS_Y_PROP, Float.toString(actor.getY()), Types.FLOAT);
			addProperty(VISIBLE_PROP, Boolean.toString(actor.isVisible()), Types.BOOLEAN);

			if (a instanceof InteractiveActor) {
				addProperty(DESC_PROP, ((InteractiveActor)actor).getDesc());

				addProperty(INTERACTION_PROP, Boolean.toString(((InteractiveActor)actor).getInteraction()), Types.BOOLEAN);
				addProperty(STATE_PROP, ((InteractiveActor)actor).getState());
			}
			
			if (a instanceof SpriteActor) {
				boolean v = ((SpriteActor) a).isBboxFromRenderer();
				
				addProperty(BBOX_FROM_RENDERER_PROP, Boolean.toString(v), Types.BOOLEAN);
			}

			Ctx.project.addPropertyChangeListener(propertyChangeListener);

			invalidateHierarchy();
		}
	}

	@Override
	protected void updateModel(String property, String value) {
		if (property.equals(DESC_PROP)) {
			((InteractiveActor)actor).setDesc(value);
		} else if (property.equals(POS_X_PROP)) {
			
			// TODO UNDO
//			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(), XMLConstants.POS_ATTR,
//					Param.toStringParam(pos));
//			Ctx.project.getUndoStack().add(undoOp);

			try {
				actor.setPosition(Float.parseFloat(value), actor.getY());
			} catch (NumberFormatException e) {

			}
		} else if (property.equals(POS_Y_PROP)) {
//			UndoOp undoOp = new UndoSetAttr(Ctx.project.getSelectedChapter(), Ctx.project.getSelectedActor(),XMLConstants.POS_ATTR,
//					Param.toStringParam(pos));
//			Ctx.project.getUndoStack().add(undoOp);
			try {
				actor.setPosition(actor.getX(), Float.parseFloat(value));
			} catch (NumberFormatException e) {

			}

		} else if (property.equals(VISIBLE_PROP)) {
			actor.setVisible(Boolean.parseBoolean(value));
		} else if (property.equals(INTERACTION_PROP)) {
			((InteractiveActor)actor).setInteraction(Boolean.parseBoolean(value));
		} else if (property.equals(STATE_PROP)) {
			((InteractiveActor)actor).setState(value);
		} else if (property.equals(BBOX_FROM_RENDERER_PROP)) {
			boolean v = true;
			
			try {
				v = Boolean.parseBoolean(value);
			} catch(ParseException e) {
				
			}
			
			((SpriteActor)actor).setBboxFromRenderer(v);
		}

	}
}

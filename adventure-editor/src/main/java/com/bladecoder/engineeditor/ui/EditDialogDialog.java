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

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.ui.panels.EditModelDialog;
import com.bladecoder.engineeditor.ui.panels.InputPanel;
import com.bladecoder.engineeditor.ui.panels.InputPanelFactory;

public class EditDialogDialog extends EditModelDialog<CharacterActor, Dialog> {
	public static final String INFO = "Actors can have several dialogs defined. Dialogs have a tree of options to choose";

	private InputPanel id;
	
	public EditDialogDialog(Skin skin,  CharacterActor parent, Dialog e) {
		super(skin);
		
		id = InputPanelFactory.createInputPanel(skin, "Dialog ID",
				"Select the dialog id to create.", true);

		setInfo(INFO);
		
		init(parent, e, new InputPanel[] { id });
	}
	
	@Override
	protected void inputsToModel(boolean create) {
		
		if(create) {
			e = new Dialog();
		} else {
			parent.getDialogs().remove(e.getId());
		}
		
		if(parent.getDialogs() != null)
			e.setId(ElementUtils.getCheckedId(id.getText(), parent.getDialogs().keySet().toArray(new String[0])));
		else
			e.setId(id.getText());
		
		parent.addDialog(e);

		// TODO UNDO OP
//		UndoOp undoOp = new UndoAddElement(doc, e);
//		Ctx.project.getUndoStack().add(undoOp);
		
		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {
		id.setText(e.getId());
	}	
}

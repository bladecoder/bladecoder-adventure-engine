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
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.InputPanel;
import com.bladecoder.engineeditor.ui.components.InputPanelFactory;

public class EditOptionDialog extends EditModelDialog<Dialog, DialogOption> {

	private InputPanel text;
	private InputPanel responseText;
	private InputPanel verb;
	private InputPanel next;
	private InputPanel visible;
	private InputPanel once;
	private int pos;
	

	public EditOptionDialog(Skin skin, Dialog parent, DialogOption e, int pos) {
		super(skin);
		
		this.pos = e == null ? pos + 1 : pos;

		text = InputPanelFactory.createInputPanel(skin, "Text", "The sentence of the dialog to say by the player",
				Type.SMALL_TEXT, true);
		responseText = InputPanelFactory.createInputPanel(skin, "Response Text", "The response by the character",
				Type.TEXT, false);
		verb = InputPanelFactory.createInputPanel(skin, "Verb", "The verb to execute when choosing this option");
		next = InputPanelFactory.createInputPanel(skin, "Next Dialog",
				"The next dialog to show when this option is selected",
				getActorDialogs((CharacterActor) Ctx.project.getSelectedActor()), false);
		visible = InputPanelFactory.createInputPanel(skin, "Visible", "The visibility", Param.Type.BOOLEAN, false);
		once = InputPanelFactory.createInputPanel(skin, "Once", "When true, the option is hidden after selection",
				Param.Type.BOOLEAN, false);

		setInfo("A dialog is composed of an option tree. Each option is a dialog sentence that the user can choose to say");

		text.getCell(text.getField()).fillX();
		responseText.getCell(responseText.getField()).fillX();

		init(parent, e, new InputPanel[] { text, responseText, verb, next, visible, once });
	}

	@Override
	protected void inputsToModel(boolean create) {

		if (create) {
			e = new DialogOption();
//			parent.addOption(e);
			
			parent.getOptions().add(pos, e);
		}
		
		String key = e.getText();

		if (key == null || key.isEmpty() || key.charAt(0) != I18N.PREFIX)
			key = Ctx.project.getI18N().genKey(Ctx.project.getSelectedScene().getId(), Ctx.project.getSelectedActor().getId(), parent.getId(), pos, "text");

		Ctx.project.getI18N().setTranslation(key, text.getText());

		if (text.getText() != null && !text.getText().isEmpty())
			e.setText(key);
		else
			e.setText(null);

		String responseKey = e.getResponseText();
		
		if (responseKey == null || responseKey.isEmpty() || responseKey.charAt(0) != I18N.PREFIX)
			responseKey = Ctx.project.getI18N().genKey(Ctx.project.getSelectedScene().getId(), Ctx.project.getSelectedActor().getId(), parent.getId(), pos, "responseText");
		
		Ctx.project.getI18N().setTranslation(responseKey, responseText.getText());

		if (responseText.getText() != null && !responseText.getText().isEmpty())
			e.setResponseText(responseKey);
		else
			e.setResponseText(null);

		e.setVerbId(verb.getText());
		e.setNext(next.getText());
		e.setVisible(Boolean.parseBoolean(visible.getText()));
		e.setOnce(Boolean.parseBoolean(once.getText()));

		// TODO UNDO OP
		// UndoOp undoOp = new UndoAddElement(doc, e);
		// Ctx.project.getUndoStack().add(undoOp);

		Ctx.project.setModified();
	}

	@Override
	protected void modelToInputs() {
		text.setText(Ctx.project.translate(e.getText()));
		
		
		responseText.setText(Ctx.project.translate(e.getResponseText()));
		
		verb.setText(e.getVerbId());
		next.setText(e.getNext());

		visible.setText(Boolean.toString(e.isVisible()));
		once.setText(Boolean.toString(e.isOnce()));
	}

	private String[] getActorDialogs(CharacterActor actor) {
		Dialog[] array = actor.getDialogs().values().toArray(new Dialog[0]);

		String[] result = new String[array.length + 1];
		result[0] = "this";

		for (int i = 0; i < array.length; i++) {
			result[i + 1] = array[i].getId();
		}

		return result;
	}
}

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
	public static final String INFO = "[RED]DEPRECATED:[]\nActors can have several dialogs defined. Dialogs have a list of options to choose. This is the legacy option to create simple dialogs, for more complex dialogs use the *Ink* language.";

	private InputPanel id;

	public EditDialogDialog(Skin skin, CharacterActor parent, Dialog e) {
		super(skin);

		id = InputPanelFactory.createInputPanel(skin, "Dialog ID", "Select the dialog id to create.", true);

		setInfo(INFO);

		init(parent, e, new InputPanel[] { id });
	}

	@Override
	protected void inputsToModel(boolean create) {

		if (create) {
			e = new Dialog();
		} else {
			parent.getDialogs().remove(e.getId());
		}

		if (parent.getDialogs() != null)
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

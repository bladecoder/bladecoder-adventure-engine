package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoDeleteDialog implements UndoOp {
	private CharacterActor a;
	private Dialog d;
	
	public UndoDeleteDialog(CharacterActor a, Dialog d) {
		this.d = d;
		this.a = a;
	}
	
	@Override
	public void undo() {
		a.addDialog(d);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, d);
	}
}

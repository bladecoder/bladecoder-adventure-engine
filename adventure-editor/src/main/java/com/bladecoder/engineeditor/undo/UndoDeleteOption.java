package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoDeleteOption implements UndoOp {
	private Dialog d;
	private DialogOption o;
	private int idx;
	
	public UndoDeleteOption(Dialog d, DialogOption o, int idx) {
		this.d = d;
		this.o = o;
		this.idx = idx;
	}
	
	@Override
	public void undo() {
		d.getOptions().add(idx, o);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, o);
	}
}

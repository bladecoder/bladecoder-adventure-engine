package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoDeleteAction implements UndoOp {
	private Verb v;
	private Action a;
	private int idx;
	
	public UndoDeleteAction(Verb v, Action a, int idx) {
		this.v = v;
		this.a = a;
		this.idx = idx;
	}
	
	@Override
	public void undo() {
		v.getActions().add(idx, a);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, a);
	}
}

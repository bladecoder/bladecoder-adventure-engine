package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.model.VerbManager;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;

public class UndoDeleteVerb implements UndoOp {
	private VerbManager vm;
	private Verb v;

	public UndoDeleteVerb(VerbManager vm, Verb v) {
		this.vm = vm;
		this.v = v;
	}

	@Override
	public void undo() {
		vm.addVerb(v);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, v);
	}
}

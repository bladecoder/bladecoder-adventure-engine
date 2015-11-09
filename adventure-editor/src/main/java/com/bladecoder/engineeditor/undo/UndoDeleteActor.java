package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoDeleteActor implements UndoOp {
	private BaseActor a;
	private Scene s;
	
	public UndoDeleteActor(Scene s, BaseActor a) {
		this.s = s;
		this.a = a;
	}
	
	@Override
	public void undo() {
		s.addActor(a);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, a);
	}
}

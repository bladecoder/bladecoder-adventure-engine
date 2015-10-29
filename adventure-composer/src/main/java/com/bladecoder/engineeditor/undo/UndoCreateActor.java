package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoCreateActor implements UndoOp {
	private Scene scn;
	private BaseActor a;
	
	public UndoCreateActor(Scene s, BaseActor a) {
		scn = s;
		this.a = a;
	}
	
	@Override
	public void undo() {
		scn.getActors().remove(a.getId());
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_DELETED, null, a);
	}
}

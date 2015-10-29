package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.Scene;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoEditActor implements UndoOp {
	private Scene scn;
	
	public UndoEditActor(Scene s) {
		scn = s;
	}
	
	@Override
	public void undo() {
		// TODO restore attributes
//		World.getInstance().getScenes().remove(scn);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, scn);
	}
}

package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.Scene;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoCreateScene implements UndoOp {
	private Scene scn;
	
	public UndoCreateScene(Scene s) {
		scn = s;
	}
	
	@Override
	public void undo() {
		Ctx.project.getWorld().getScenes().remove(scn.getId());
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_DELETED, null, scn);
	}
}

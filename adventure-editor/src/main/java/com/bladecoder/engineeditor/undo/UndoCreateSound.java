package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoCreateSound implements UndoOp {
	private SoundDesc s;
	
	public UndoCreateSound(SoundDesc s) {
		this.s = s;
	}
	
	@Override
	public void undo() {
		Ctx.project.getWorld().getSounds().remove(s.getFilename());
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_DELETED, null, s);
	}
}

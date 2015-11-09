package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.SoundFX;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoCreateSound implements UndoOp {
	private SoundFX s;
	private InteractiveActor a;
	
	public UndoCreateSound(InteractiveActor a, SoundFX s) {
		this.s = s;
		this.a = a;
	}
	
	@Override
	public void undo() {
		a.getSounds().remove(s.getFilename());
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_DELETED, null, s);
	}
}

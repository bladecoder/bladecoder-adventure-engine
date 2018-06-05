package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.common.ElementUtils;
import com.bladecoder.engineeditor.model.Project;


public class UndoDeleteSound implements UndoOp {
	private SoundDesc s;
	
	public UndoDeleteSound(SoundDesc s) {
		this.s = s;
	}
	
	@Override
	public void undo() {
		s.setId(ElementUtils.getCheckedId(s.getFilename(), Ctx.project.getWorld().getSounds().keySet().toArray(new String[ Ctx.project.getWorld().getSounds().size()]))); 
		Ctx.project.getWorld().getSounds().put(s.getId(), s);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, s);
	}
}

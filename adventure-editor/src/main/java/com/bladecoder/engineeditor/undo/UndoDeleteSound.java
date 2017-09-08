package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.SoundDesc;
import com.bladecoder.engine.model.World;
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
		s.setId(ElementUtils.getCheckedId(s.getFilename(), World.getInstance().getSounds().keySet().toArray(new String[ World.getInstance().getSounds().size()]))); 
		World.getInstance().getSounds().put(s.getId(), s);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, s);
	}
}

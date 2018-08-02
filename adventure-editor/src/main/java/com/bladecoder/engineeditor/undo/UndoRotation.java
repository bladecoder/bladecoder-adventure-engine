package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;


public class UndoRotation implements UndoOp {
	private SpriteActor a;
	private float rot;
	
	public UndoRotation(SpriteActor a, float rot) {
		this.rot = rot;
		this.a = a;
	}
	
	@Override
	public void undo() {
		a.setRot(rot);
		Ctx.project.setModified();
	}
}

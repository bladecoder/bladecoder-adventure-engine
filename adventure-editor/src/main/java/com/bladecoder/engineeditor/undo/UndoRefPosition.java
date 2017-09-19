package com.bladecoder.engineeditor.undo;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engineeditor.Ctx;


public class UndoRefPosition implements UndoOp {
	private InteractiveActor a;
	private Vector2 pos;
	
	public UndoRefPosition(InteractiveActor a, Vector2 pos) {
		this.pos = pos;
		this.a = a;
	}
	
	@Override
	public void undo() {
		a.setRefPoint(pos.x, pos.y);
		Ctx.project.setModified();
	}
}

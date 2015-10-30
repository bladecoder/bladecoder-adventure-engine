package com.bladecoder.engineeditor.undo;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoPositionAction implements UndoOp {
	private BaseActor a;
	private Vector2 pos;
	
	public UndoPositionAction(BaseActor a, Vector2 pos) {
		this.pos = pos;
		this.a = a;
	}
	
	@Override
	public void undo() {
		a.setPosition(pos.x, pos.y);
		Ctx.project.setModified(this, Project.POSITION_PROPERTY, null, a);
	}
}

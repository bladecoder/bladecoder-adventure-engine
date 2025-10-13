package com.bladecoder.engineeditor.undo;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engineeditor.Ctx;


public class UndoBboxPointPos implements UndoOp {
	private BaseActor a;
	private Vector2 pos;
	private int i;
	
	public UndoBboxPointPos(BaseActor a, int i, Vector2 pos) {
		this.pos = pos;
		this.a = a;
		this.i = i;
	}
	
	@Override
	public void undo() {
		
		float[] verts = a.getBBox().getVertices();
		verts[i] = pos.x;
		verts[i + 1] = pos.y;
		a.getBBox().dirty();
		
		Ctx.project.setModified();
	}
}

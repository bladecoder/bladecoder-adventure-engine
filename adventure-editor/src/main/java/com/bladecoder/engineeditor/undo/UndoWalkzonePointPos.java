package com.bladecoder.engineeditor.undo;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engineeditor.Ctx;


public class UndoWalkzonePointPos implements UndoOp {
	private Polygon wz;
	private Vector2 pos;
	private int i;
	
	public UndoWalkzonePointPos(Polygon wz, int i, Vector2 pos) {
		this.pos = pos;
		this.wz = wz;
		this.i = i;
	}
	
	@Override
	public void undo() {
		
		float verts[] = wz.getVertices();
		verts[i] = pos.x;
		verts[i + 1] = pos.y;
		wz.dirty();
		
		Ctx.project.setModified();
	}
}

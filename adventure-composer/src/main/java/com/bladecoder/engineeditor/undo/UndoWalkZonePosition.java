package com.bladecoder.engineeditor.undo;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;


public class UndoWalkZonePosition implements UndoOp {
	private Polygon wz;
	private Vector2 oldpos;
	
	public UndoWalkZonePosition(Polygon walkzone, Vector2 pos) {
		this.oldpos = pos;
		this.wz = walkzone;
	}
	
	@Override
	public void undo() {
		wz.setPosition(oldpos.x, oldpos.y);
	}
}

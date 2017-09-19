package com.bladecoder.engineeditor.undo;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;


public class UndoDepthVector implements UndoOp {
	private Vector2 pos;
	private Scene s;
	
	public UndoDepthVector(Scene s, Vector2 pos) {
		this.pos = pos;
		this.s = s;
	}
	
	@Override
	public void undo() {
		s.getDepthVector().set(pos.x, pos.y);
		Ctx.project.setModified();
		updateFakeDepth();
	}
	
	private void updateFakeDepth() {
		for (BaseActor a : s.getActors().values()) {
			if (a instanceof SpriteActor) {
				a.setPosition(a.getX(), a.getY());
			}
		}
	}
}

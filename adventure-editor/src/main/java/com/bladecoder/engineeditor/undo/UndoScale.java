package com.bladecoder.engineeditor.undo;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;


public class UndoScale implements UndoOp {
	private SpriteActor a;
	private Vector2 scale;
	
	public UndoScale(SpriteActor a, Vector2 scale) {
		this.scale = scale;
		this.a = a;
	}
	
	@Override
	public void undo() {
		a.setScale(scale.x, scale.y);
		Ctx.project.setModified();
	}
}

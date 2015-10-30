package com.bladecoder.engineeditor.undo;

import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.model.Project;


public class UndoDeleteAnimation implements UndoOp {
	private SpriteActor a;
	private AnimationDesc anim;
	
	public UndoDeleteAnimation(SpriteActor a, AnimationDesc anim) {
		this.anim = anim;
		this.a = a;
	}
	
	@Override
	public void undo() {
		a.getRenderer().addAnimation(anim);
		Ctx.project.setModified(this, Project.NOTIFY_ELEMENT_CREATED, null, anim);
	}
}

package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.BaseActor;

public interface ActorVisitor extends Visitor {
	public void visit(BaseActor a);
}

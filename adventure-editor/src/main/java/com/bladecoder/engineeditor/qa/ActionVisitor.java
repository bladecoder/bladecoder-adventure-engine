package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;

public interface ActionVisitor extends Visitor {
	public void visit(Scene scn, InteractiveActor a, Verb v, Action act);
}

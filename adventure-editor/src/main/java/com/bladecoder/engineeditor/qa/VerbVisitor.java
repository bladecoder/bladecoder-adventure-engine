package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;

public interface VerbVisitor extends Visitor {
	public void visit(Scene s, InteractiveActor a, Verb v);
}

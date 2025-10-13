package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.Scene;

public interface SceneVisitor extends Visitor {
	void visit(Scene s);
}

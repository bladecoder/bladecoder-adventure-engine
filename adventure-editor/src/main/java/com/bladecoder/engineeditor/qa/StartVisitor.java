package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.World;

public interface StartVisitor extends Visitor {
	void start(World w);
}

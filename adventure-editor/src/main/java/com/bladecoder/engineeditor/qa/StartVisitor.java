package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.World;

public interface StartVisitor extends Visitor {
	public void start(World w);
}

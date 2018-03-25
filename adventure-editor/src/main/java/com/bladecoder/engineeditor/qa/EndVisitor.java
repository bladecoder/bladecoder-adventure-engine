package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.World;

public interface EndVisitor extends Visitor {
	public void end(World w);
}

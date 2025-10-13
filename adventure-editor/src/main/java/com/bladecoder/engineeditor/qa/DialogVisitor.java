package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;

public interface DialogVisitor extends Visitor {
	void visit(CharacterActor a, Dialog d);
}

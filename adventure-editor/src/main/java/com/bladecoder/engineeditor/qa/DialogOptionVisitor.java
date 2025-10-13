package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;

public interface DialogOptionVisitor extends Visitor {
	void visit(CharacterActor a, Dialog d, DialogOption o);
}

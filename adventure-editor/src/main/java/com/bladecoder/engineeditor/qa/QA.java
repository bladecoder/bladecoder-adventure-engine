package com.bladecoder.engineeditor.qa;

import com.bladecoder.engine.model.World;
import com.bladecoder.engineeditor.qa.rules.CheckDesc;
import com.bladecoder.engineeditor.qa.rules.CheckInteractionVerbs;
import com.bladecoder.engineeditor.qa.rules.Stats;

public class QA {
	public void run(World w) {
		ModelWalker mw = new ModelWalker();
		
		mw.addVisitor(new Stats());
		mw.addVisitor(new CheckInteractionVerbs());
		mw.addVisitor(new CheckDesc());
		
		mw.walk(w);
	}
}

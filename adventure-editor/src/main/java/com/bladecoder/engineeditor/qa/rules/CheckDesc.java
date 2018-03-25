package com.bladecoder.engineeditor.qa.rules;

import java.util.HashMap;

import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.qa.ActorVisitor;

public class CheckDesc implements ActorVisitor {

	private InteractiveActor ia;

	private boolean hasLeave;
	private boolean hasEnterExit;

	@Override
	public void visit(BaseActor a) {
		if (a instanceof InteractiveActor) {
			ia = (InteractiveActor) a;

			hasLeave = false;
			hasEnterExit = false;

			if (ia.getInteraction()) {
				
				HashMap<String, Verb> verbs = ia.getVerbManager().getVerbs();
				
				for(Verb v:verbs.values())
					checkVerb(v);
				
				// TODO: check states and verbs for states.

				if (hasLeave || hasEnterExit)
					return;
				
				if(ia.getDesc() == null || ia.getDesc().trim().isEmpty()) {
					EditorLogger.error("CheckDesc: " + a.getScene().getId() + "." + a.getId());
				}
			}
		}
	}

	public void checkVerb(Verb v) {
		if (v.getId().equals("leave"))
			hasLeave = true;
		else if (v.getId().equals("enter") || v.getId().equals("exit"))
			hasEnterExit = true;
	}

}

package com.bladecoder.engineeditor.qa.rules;

import java.util.ArrayList;

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.SetCutmodeAction;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.Verb;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engineeditor.common.EditorLogger;
import com.bladecoder.engineeditor.qa.VerbVisitor;

/**
 * Checks that cutmode is set to false before ending the verb if it was previously set to true.
 * @author rgarcia
 */
public class CheckCutmodeEnd implements VerbVisitor {


	@Override
	public void visit(Scene s, InteractiveActor a, Verb v) {
		ArrayList<Action> actions = v.getActions();
		
		if(actions.size() > 0) {
			Action action = actions.get(actions.size() - 1);
			
			if (action instanceof SetCutmodeAction) {
				try {
					String val = ActionUtils.getStringValue(action, "value");
					
					if("true".equals(val)) {	
						StringBuilder sb = new StringBuilder("CheckCutmodeEnd: Cutmode ends with value=true! - ");
						
						if(s != null) {
							sb.append(s.getId());
							sb.append(".");
						}
						
						if(a != null) {
							sb.append(a.getId());
							sb.append(".");
						}
						
						sb.append(v.getId());						
						
						EditorLogger.error(sb.toString());
					}
				} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
					
				}
			}
		}
	}

}

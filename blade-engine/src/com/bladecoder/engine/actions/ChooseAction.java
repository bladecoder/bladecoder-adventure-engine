/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.actions;

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.math.MathUtils;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.VerbRunner;

@ActionDescription("Execute only one action inside the Choose/EndChoose block.")
public class ChooseAction implements Action {
	private static final String ITERATE = "iterate";
	private static final String RANDOM = "random";
	private static final String CYCLE = "cycle";

	public static final Param[] PARAMS = {
			new Param("chooseCriteria", "The action to execute will be selected following this criteria.",
				Type.OPTION, true, CYCLE, new String[]{ITERATE, RANDOM, CYCLE}),
				new Param("endType", "The type for the end action. All control actions must have this attr.", Type.STRING, false, "choose")
			};

	/** 
	 * When the verb is a comma separated verb list, we use chooseCriteria as criteria to choose the verb to execute.
	 */
	String chooseCriteria;
	/** Used when choose_criteria is 'iterate' or 'cycle' */
	int chooseCount = -1;

	@Override
	public void setParams(HashMap<String, String> params) {
		chooseCriteria = params.get("chooseCriteria");
	}

	@Override
	public boolean run(ActionCallback cb) {
		VerbRunner v = (VerbRunner) cb;

		int ip = v.getIP();
		int ip0 = ip + 1;
		int numActions = -1;
		ArrayList<Action> actions = v.getActions();

		while (!(actions.get(ip) instanceof EndAction)
				|| !((EndAction) actions.get(ip)).getType().equals("choose")) {
			ip++;
			numActions++;
		}
		
		if(numActions <= 0)
			return false;
		
		if(chooseCriteria.equals(ITERATE)) {			
			chooseCount++;
		} else if(chooseCriteria.equals(RANDOM)) {
			chooseCount = MathUtils.random(0, numActions - 1);
		} else if(chooseCriteria.equals(CYCLE)) {
			chooseCount = (chooseCount + 1) % numActions;
		}
		
		v.setIP(ip);
		
		if(chooseCount < numActions) {
			return v.getActions().get(ip0 + chooseCount).run(v);
		}

		return false;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

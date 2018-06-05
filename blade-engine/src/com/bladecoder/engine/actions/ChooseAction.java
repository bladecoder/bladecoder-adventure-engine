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

import java.util.List;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Execute only one action inside the Choose/EndChoose block.")
public class ChooseAction extends AbstractControlAction implements Serializable {
	public enum ChooseCriteria {
		ITERATE, RANDOM, CYCLE
	}

	/**
	 * When the verb is a comma separated verb list, we use chooseCriteria as criteria to choose the verb to execute.
	 */
	@ActionProperty(required = true, defaultValue = "CYCLE")
	@ActionPropertyDescription("The action to execute will be selected following this criteria.")
	private ChooseCriteria chooseCriteria = ChooseCriteria.CYCLE;

	/** Used when choose_criteria is 'iterate' or 'cycle' */
	int chooseCount = -1;
	
	@Override
	public void init(World w) {
	}

	@Override
	public boolean run(VerbRunner cb) {

		int startIp = cb.getIP();
		int ip0 = startIp + 1;
		final List<Action> actions = cb.getActions();

		int ip = skipControlIdBlock(actions, startIp);
		int numActions = ip - startIp - 1;

		if(numActions <= 0)
			return false;

		switch (chooseCriteria) {
			case ITERATE:
				chooseCount++;
				break;
			case RANDOM:
				chooseCount = MathUtils.random(0, numActions - 1);
				break;
			case CYCLE:
				chooseCount = (chooseCount + 1) % numActions;
				break;
		}

		cb.setIP(ip);

		if(chooseCount < numActions) {
			return actions.get(ip0 + chooseCount).run(cb);
		}

		return false;
	}

	@Override
	public String getControlActionID() {
		return caID;
	}
	
	
	@Override
	public void write(Json json) {
		json.writeValue("chooseCount", chooseCount);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		chooseCount = json.readValue("chooseCount", int.class, 0, jsonData);
	}
}

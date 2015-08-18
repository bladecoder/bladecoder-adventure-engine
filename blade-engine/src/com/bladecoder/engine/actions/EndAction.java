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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.VerbRunner;

@ActionDescription("Marks the end of a block for a control action")
public class EndAction implements Action {
	public static final Param[] PARAMS = { new Param("endType", "The block type", Type.STRING) };

	String type;

	@Override
	public void setParams(HashMap<String, String> params) {
		type = params.get("endType");
	}

	@Override
	public boolean run(ActionCallback cb) {
		if (type.equals("repeat")) {

			VerbRunner v = (VerbRunner) cb;
			int ip = v.getIP();
			ArrayList<Action> actions = v.getActions();

			// Find the previous repeat action for the next loop
			while (!(actions.get(ip) instanceof RepeatAction))
				ip--;
			ip--;
			v.setIP(ip);
		} else if (type.equals("else")) {
			VerbRunner v = (VerbRunner) cb;
			int ip = v.getIP();
			ArrayList<Action> actions = v.getActions();
			
			while(!((actions.get(ip) instanceof EndAction) &&
					((EndAction)actions.get(ip)).getType().equals("if"))) ip++; 
			
			v.setIP(ip);
		}

		return false;
	}

	public String getType() {
		return type;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

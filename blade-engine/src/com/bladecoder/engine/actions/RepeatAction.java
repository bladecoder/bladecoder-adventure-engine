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

public class RepeatAction implements Action {

	public static final String INFO = "Repeats the actions inside the Repeat/EndRepeat actions.";
	public static final Param[] PARAMS = {
			new Param("repeat", "Repeat the actions the specified times. -1 to infinity",
					Type.INTEGER, true, "1"),
			new Param("endType", "The type for the end action. All control actions must have this attr.", Type.STRING, false, "repeat")};

	int repeat = 1;
	int currentRepeat = 0;

	@Override
	public void setParams(HashMap<String, String> params) {
		repeat = Integer.parseInt(params.get("repeat"));
	}

	@Override
	public boolean run(ActionCallback cb) {
		VerbRunner v = (VerbRunner)cb;
		
		currentRepeat++;
		
		if(currentRepeat > repeat && repeat >= 0) {
			int ip = v.getIP();
			ArrayList<Action> actions = v.getActions();
			
			// TODO: Handle RepeatAction to allow nested Repeats
			while(!(actions.get(ip) instanceof EndAction) || !((EndAction)actions.get(ip)).getType().equals("repeat")) ip++; 
			
			v.setIP(ip);
			currentRepeat = 0;
		}
		
		return false;
	}
	
	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

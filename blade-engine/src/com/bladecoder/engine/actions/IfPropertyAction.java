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
import com.bladecoder.engine.model.World;

public class IfPropertyAction implements Action {

	public static final String INFO = "Execute the actions inside the If/EndIf if the game propert has the specified value.";
	public static final Param[] PARAMS = {
			new Param("name", "The property name", Type.STRING, true),
			new Param("value", "The property value", Type.STRING),
			new Param("endType", "The type for the end action. All control actions must have this attr.", Type.STRING,
					false, "else") };

	String name;
	String value;

	@Override
	public void setParams(HashMap<String, String> params) {
		name = params.get("name");
		value = params.get("value");
	}

	@Override
	public boolean run(ActionCallback cb) {
		String valDest = World.getInstance().getCustomProperty(name); 
		
		if ( (value == null && valDest != null) ||
			 (value != null && !value.equals(valDest))
				) {
			gotoElse((VerbRunner) cb);
		}

		return false;
	}

	private void gotoElse(VerbRunner v) {
		int ip = v.getIP();
		ArrayList<Action> actions = v.getActions();

		// TODO: Handle If to allow nested Ifs
		while (!(actions.get(ip) instanceof EndAction) || !((EndAction) actions.get(ip)).getType().equals("else"))
			ip++;

		v.setIP(ip);
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

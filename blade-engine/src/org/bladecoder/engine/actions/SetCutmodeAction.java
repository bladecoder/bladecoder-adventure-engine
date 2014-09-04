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
package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;

public class SetCutmodeAction implements Action {
	public static final String INFO = "Set/Unset the cutmode. Also shows/hide the inventory";
	public static final Param[] PARAMS = {
		new Param("value", "when 'true' sets the scene in 'cutmode'", Type.BOOLEAN, true), 
		new Param("inventory", "show/hide the inventory", Type.BOOLEAN)
		};	
	
	boolean value = true;
	boolean inventory = true;
	boolean setInventory = false;
	
	@Override
	public void setParams(HashMap<String, String> params) {	
		if(params.get("value") != null)
			value = Boolean.parseBoolean(params.get("value"));
		
		if(params.get("inventory") != null) {
			setInventory = true;
			inventory = Boolean.parseBoolean(params.get("inventory"));
		}
	}

	@Override
	public void run() {
		World.getInstance().setCutMode(value);
		
		if(setInventory)
			World.getInstance().showInventory(inventory);
	}

	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}

	@Override
	public boolean waitForFinish(ActionCallback cb) {
		return false;
	}
}

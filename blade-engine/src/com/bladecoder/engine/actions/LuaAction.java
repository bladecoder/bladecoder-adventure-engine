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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.lua.ScriptManager;
import com.bladecoder.engine.model.VerbRunner;

@ActionDescription(name = "Lua Script", value="Execs the Lua script.")
public class LuaAction implements Action {
	@ActionProperty(required = true, type = Type.SMALL_TEXT)
	@ActionPropertyDescription("The Lua script to execute.")
	private String script = null;

	@Override
	public boolean run(VerbRunner cb) {
		if(script != null) {
			ScriptManager.getInstance().eval(script.replace("\\n\\n", "\n"));
		}
		
		return false;
	}

}

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

@ActionDescription(name = "IfExpr",value="Execute the actions inside the If/EndIf evaluating the Lua expresion.")
public class IfLuaExprAction extends AbstractIfAction {
	@ActionProperty(required = true, type = Type.SMALL_TEXT)
	@ActionPropertyDescription("The Lua expresion.")
	private String expr = null;

	@Override
	public boolean run(VerbRunner cb) {
		if(expr != null) {
			boolean ret = ScriptManager.getInstance().evalIf(expr.replace("\\n\\n", "\n"));
			
			if (!ret) {
				gotoElse((VerbRunner) cb);
			}
		}

		return false;
	}

}

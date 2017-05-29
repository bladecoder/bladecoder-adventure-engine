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

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;

@ActionDescription("Execute the actions inside the If/EndIf if the game property has the specified value. Properties are created by the user but the next always exists: SAVED_GAME_VERSION, PREVIOUS_SCENE, CURRENT_CHAPTER")
public class IfPropertyAction extends AbstractIfAction {
	@ActionProperty(required = true)
	@ActionPropertyDescription("The property name")
	private String name;

	@ActionProperty
	@ActionPropertyDescription("The property value")
	private String value;

	@Override
	public boolean run(VerbRunner cb) {
		String valDest = World.getInstance().getCustomProperty(name); 
		
		if (!ActionUtils.compareNullStr(value, valDest)) {
			gotoElse((VerbRunner) cb);
		}

		return false;
	}

}

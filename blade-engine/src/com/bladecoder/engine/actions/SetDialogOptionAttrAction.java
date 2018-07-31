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
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Change the selected dialog option properties")
public class SetDialogOptionAttrAction implements Action {
	@ActionProperty(type = Type.SCENE_CHARACTER_ACTOR, required = true)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The dialog")
	private String dialog;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The option")
	private int option;

	@ActionProperty
	@ActionPropertyDescription("Show/Hide the dialog option")
	private Boolean visible;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		final Scene s = actor.getScene(w);

		CharacterActor a = (CharacterActor) s.getActor(actor.getActorId(), true);

		Dialog d = a.getDialog(dialog);

		if (d == null) {
			EngineLogger.error("SetDialogOptionAttrAction: Dialog '" + dialog + "' not found");
			return false;
		}

		DialogOption o = d.getOptions().get(option);

		if (o == null) {
			EngineLogger.error("SetDialogOptionAttrAction: Option '" + option + "' not found");
			return false;
		}

		if (visible != null)
			o.setVisible(visible);

		return false;
	}

}

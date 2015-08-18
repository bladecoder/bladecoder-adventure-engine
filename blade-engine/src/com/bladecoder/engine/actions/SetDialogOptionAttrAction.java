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

import java.util.HashMap;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Change the selected dialog option properties")
public class SetDialogOptionAttrAction implements Action {
	public static final Param[] PARAMS = {
		new Param("actor", "The target actor", Type.SCENE_ACTOR, false),
		new Param("dialog", "The dialog", Type.STRING, true),	
		new Param("option", "The option", Type.STRING, true),
		new Param("visible", "Shows/Hide the dialog option", Type.BOOLEAN),
		new Param("set_current", "Sets the selected option as the current dialog option", Type.BOOLEAN)		
		};	
	
	
	String actorId;
	String sceneId;
	String dialog;
	String option;
	boolean setVisibility;
	boolean setCurrent = false;
	boolean visibility;

	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));
		
		sceneId = a[0];
		actorId = a[1];

		dialog = params.get("dialog");
		option = params.get("option");

		if (params.get("visible") != null) {
			setVisibility = true;
			visibility = Boolean.parseBoolean(params.get("visible"));
		}

		if (params.get("set_current") != null) {
			setCurrent = Boolean.parseBoolean(params.get("set_current"));
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		Scene s;
		
		if(sceneId != null && !sceneId.isEmpty()) {
			s = World.getInstance().getScene(sceneId);
		} else {
			s = World.getInstance().getCurrentScene();
		}
		
		CharacterActor actor = (CharacterActor)s.getActor(actorId, true);
		
		Dialog d = actor.getDialog(dialog);

		if (d == null) {
			EngineLogger.error("SetDialogOptionAttrAction: Dialog '" + dialog + "' not found");
			return false;
		}

		DialogOption o = null;
		
		if (option != null) {
			o = d.findSerOption(option);

			if (o == null) {
				EngineLogger.error("SetDialogOptionAttrAction: Option '" + option + "' not found");
				return false;
			}
		}

		if (setVisibility && o != null)
			o.setVisible(visibility);

		if (setCurrent && World.getInstance().getCurrentScene() == s) {
			World.getInstance().setCurrentDialog(actor.getDialog(dialog));
			d.setCurrentOption(o);
		}
		
		return false;
	}


	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

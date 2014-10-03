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

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.Param;

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class DialogOptionAction implements Action {
	public static final String INFO = "Change the selected dialog option properties";
	public static final Param[] PARAMS = {
		new Param("dialog", "The dialog", Type.STRING, true),	
		new Param("option", "The option", Type.STRING, true),
		new Param("visible", "Shows/Hide the dialog option", Type.BOOLEAN),
		new Param("set_current", "Sets the selected option as the current dialog option", Type.BOOLEAN)		
		};	
	
	
	String actorId;
	String dialog;
	String option;
	boolean setVisibility;
	boolean setCurrent = false;
	boolean visibility;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
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

		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene()
				.getActor(actorId, false);
		Dialog d = actor.getDialog(dialog);

		if (d == null) {
			EngineLogger.error("DialogOptionAction: Dialog '" + dialog + "' not found");
			return false;
		}

		DialogOption o = null;
		
		if (option != null) {
			o = d.findSerOption(option);

			if (o == null) {
				EngineLogger.error("DialogOptionAction: Option '" + option + "' not found");
				return false;
			}
		}

		if (setVisibility && o != null)
			o.setVisible(visibility);

		if (setCurrent) {
			World.getInstance().setCurrentDialog(actor.getDialog(dialog));
			d.setCurrentOption(o);
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

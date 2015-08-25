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
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SetDialogOptionAttr")
@ModelDescription("Change the selected dialog option properties")
public class SetDialogOptionAttrAction implements Action {
	@JsonProperty("actor")
	@JsonPropertyDescription("The target actor")
	@ModelPropertyType(Type.SCENE_ACTOR)
	private SceneActorRef sceneActorRef;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The dialog")
	@ModelPropertyType(Type.STRING)
	private String dialog;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The option")
	
	@ModelPropertyType(Type.INTEGER)
	private int option;

	@JsonProperty("visible")
	@JsonPropertyDescription("Show/Hide the dialog option")
	@ModelPropertyType(Type.BOOLEAN)
	private boolean visibility;
	private boolean setVisibility;

	@Override
	public void setParams(HashMap<String, String> params) {
		String[] a = Param.parseString2(params.get("actor"));

		sceneActorRef = new SceneActorRef(a[0], a[1]);

		dialog = params.get("dialog");
		option = Integer.parseInt(params.get("option"));

		if (params.get("visible") != null) {
			setVisibility = true;
			visibility = Boolean.parseBoolean(params.get("visible"));
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		final Scene s = sceneActorRef.getScene();

		CharacterActor actor = (CharacterActor) s.getActor(sceneActorRef.getActorId(), true);

		Dialog d = actor.getDialog(dialog);

		if (d == null) {
			EngineLogger.error("SetDialogOptionAttrAction: Dialog '" + dialog + "' not found");
			return false;
		}

		DialogOption o = d.getOptions().get(option);

		if (o == null) {
			EngineLogger.error("SetDialogOptionAttrAction: Option '" + option + "' not found");
			return false;
		}

		if (setVisibility && o != null)
			o.setVisible(visibility);

		return false;
	}

}

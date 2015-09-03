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
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Talkto")
@ModelDescription("Sets the dialog mode")
public class TalktoAction extends AbstractAction {
	@JsonProperty("actor")
	@JsonPropertyDescription("The target actor")
	@ModelPropertyType(Type.ACTOR)
	private String actorId;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The 'dialogId' to show")
	@ModelPropertyType(Type.STRING)
	private String dialog;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		dialog = params.get("dialog");
	}

	@Override
	public boolean run(ActionCallback cb) {
		
		CharacterActor actor = (CharacterActor)World.getInstance().getCurrentScene().getActor(actorId, false);
		
		World.getInstance().setCurrentDialog(actor.getDialog(dialog));
		
		return false;
	}


}

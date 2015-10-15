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

import com.badlogic.gdx.graphics.Color;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Transition;
import com.bladecoder.engine.model.World;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Sets a transition effect (FADEIN/FADEOUT)")
public class TransitionAction implements Action {
	@JsonProperty(required = true, defaultValue = "1.0")
	@JsonPropertyDescription("Duration of the transition")
	@ActionPropertyType(Type.FLOAT)
	private float time = 1;

	@JsonProperty(required = true, defaultValue = "black")
	@JsonPropertyDescription("The color to fade ('white', 'black' or RRGGBBAA).")
	@ActionPropertyType(Type.COLOR)
	private Color color = new Color(0,0,0,1);

	@JsonProperty(required = true, defaultValue = "FADE_IN")
	@JsonPropertyDescription("The transition type (fadein/fadeout)")
	@ActionPropertyType(Type.STRING)
	private Transition.Type type = Transition.Type.FADE_IN;     // FIXME: This adds NONE as a valid value

	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the transition is showed and the action continues inmediatly")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean wait = true;

	@Override
	public boolean run(ActionCallback cb) {
		Transition t = World.getInstance().getTransition();
		t.create(time, color, type, wait?cb:null);
		
		return wait;
	}

}

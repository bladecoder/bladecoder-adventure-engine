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

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.AnimationDesc;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Text;
import com.bladecoder.engine.model.TextManager;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Shows the text and sets the player to lookat in the selected actor direction")
public class LookAtAction implements Action {
	public enum Direction {
		EMPTY(""),
		FRONT(AnimationDesc.FRONT),
		BACK(AnimationDesc.BACK),
		LEFT(AnimationDesc.LEFT),
		RIGHT(AnimationDesc.RIGHT),
		FRONTLEFT(AnimationDesc.FRONTLEFT),
		FRONTRIGHT(AnimationDesc.FRONTRIGHT),
		BACKLEFT(AnimationDesc.BACKLEFT),
		BACKRIGHT(AnimationDesc.BACKRIGHT);

		private final String direction;

		Direction(String direction) {
			this.direction = direction;
		}

		public String getDirection() {
			return direction;
		}
	}

	@JsonProperty
	@JsonPropertyDescription("The target actor")
	@ActionPropertyType(Type.ACTOR)
	private String actor;

	@JsonProperty("speech")
	@JsonPropertyDescription("The 'soundId' to play if selected")
	@ActionPropertyType(Type.SOUND)
	private String soundId;

	@JsonProperty
	@JsonPropertyDescription("The 'text' to show")
	@ActionPropertyType(Type.SMALL_TEXT)
	private String text;

	@JsonProperty
	@JsonPropertyDescription("The direction to lookat. If empty, the player lookat to the actor")
	@ActionPropertyType(Type.STRING)
	private Direction direction;
	
	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean wait = true;	

	@Override
	public boolean run(ActionCallback cb) {

		EngineLogger.debug("LOOKAT ACTION");
		InteractiveActor a = (InteractiveActor) World.getInstance().getCurrentScene().getActor(actor, true);

		CharacterActor player = World.getInstance().getCurrentScene().getPlayer();
		
		if(direction!=null) player.lookat(direction.getDirection());
		else if(a!=null && player != null) {
			Rectangle bbox = a.getBBox().getBoundingRectangle();
			player.lookat(new Vector2(bbox.x, bbox.y));
		}

		if (soundId != null) {
			if (a == null) {
				EngineLogger.debug("Tried to play a sound (" + soundId + "), but there is no actor defined");
			} else {
				a.playSound(soundId);
			}
		}

		if(text !=null)
			World.getInstance().getTextManager().addText(text, TextManager.POS_SUBTITLE,
					TextManager.POS_SUBTITLE, false, Text.Type.RECTANGLE, player.getTextColor(), null,  wait?cb:null);
		
		return false;
	}


}

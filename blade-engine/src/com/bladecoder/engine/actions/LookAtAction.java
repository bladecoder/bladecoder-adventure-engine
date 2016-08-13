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
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Shows the text and sets the player to lookat in the selected actor direction")
public class LookAtAction implements Action {
	public enum Direction {
		EMPTY(""), FRONT(AnimationDesc.FRONT), BACK(AnimationDesc.BACK), LEFT(AnimationDesc.LEFT), RIGHT(
				AnimationDesc.RIGHT), FRONTLEFT(AnimationDesc.FRONTLEFT), FRONTRIGHT(
						AnimationDesc.FRONTRIGHT), BACKLEFT(AnimationDesc.BACKLEFT), BACKRIGHT(AnimationDesc.BACKRIGHT);

		private final String direction;

		Direction(String direction) {
			this.direction = direction;
		}

		public String getDirection() {
			return direction;
		}
	}

	@ActionPropertyDescription("The target actor")
	@ActionProperty(type = Type.ACTOR)
	private String actor;

	@ActionPropertyDescription("The 'text' to show")
	@ActionProperty(type = Type.SMALL_TEXT)
	private String text;

	@ActionProperty
	@ActionPropertyDescription("The direction to lookat. If empty, the player lookat to the actor")
	private Direction direction;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	@Override
	public boolean run(VerbRunner cb) {

//		EngineLogger.debug("LOOKAT ACTION");
		InteractiveActor a = (InteractiveActor) World.getInstance().getCurrentScene().getActor(actor, true);

		if (World.getInstance().getInventory().getItem(actor) == null) {
			CharacterActor player = World.getInstance().getCurrentScene().getPlayer();

			if (direction != null && player != null)
				player.lookat(direction.getDirection());
			else if (a != null && player != null) {
				Rectangle bbox = a.getBBox().getBoundingRectangle();
				player.lookat(new Vector2(bbox.x, bbox.y));
			}
		}

		if (text != null) {
			String actorId = World.getInstance().getCurrentScene().getPlayer() != null? World.getInstance().getCurrentScene().getPlayer().getId():null;
			
			World.getInstance().getTextManager().addText(text, TextManager.POS_SUBTITLE, TextManager.POS_SUBTITLE,
					false, Text.Type.SUBTITLE, null, null, actorId, wait ? cb : null);
			
			return wait;
		}

		return false;
	}

}

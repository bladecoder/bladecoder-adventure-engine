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

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.CharacterActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Walks to the selected position")
public class GotoAction implements Action {
	public enum Align {
		CENTER, LEFT, RIGHT
	}

	@ActionPropertyDescription("The walking actor")
	@ActionProperty(type = Type.CHARACTER_ACTOR, required = true)
	private String actor;

	@ActionPropertyDescription("Walks to this actor")
	@ActionProperty(type = Type.ACTOR)
	private String target;

	@ActionProperty
	@ActionPropertyDescription("The absolute position to walk to if no target actor is selected. Relative to target if selected.")
	private Vector2 pos;

	@ActionProperty(required = true, defaultValue = "false")
	@ActionPropertyDescription("Ignore the walking zone and walk in a straight line.")
	private boolean ignoreWalkZone = false;

	@ActionProperty(required = true, defaultValue = "true")
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {

		CharacterActor actor = (CharacterActor) w.getCurrentScene().getActor(this.actor, false);

		float x = actor.getX();
		float y = actor.getY();

		if (target != null) {
			BaseActor targetActor = w.getCurrentScene().getActor(target, false);

			if (targetActor == null) {
				EngineLogger.error(target + " not found in the current scene.");
				return false;
			}

			x = targetActor.getX();
			y = targetActor.getY();

			if (targetActor instanceof InteractiveActor && targetActor != actor) {
				Vector2 refPoint = ((InteractiveActor) targetActor).getRefPoint();
				x += refPoint.x;
				y += refPoint.y;
			}

			if (pos != null) {
				float scale = EngineAssetManager.getInstance().getScale();

				x += pos.x * scale;
				y += pos.y * scale;
			}
		} else if (pos != null) {
			float scale = EngineAssetManager.getInstance().getScale();

			x = pos.x * scale;
			y = pos.y * scale;
		}

		actor.goTo(new Vector2(x, y), wait ? cb : null, ignoreWalkZone);

		return wait;
	}

}

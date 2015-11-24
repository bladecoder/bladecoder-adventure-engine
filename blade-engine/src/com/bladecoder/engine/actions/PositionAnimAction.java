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
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.InterpolationMode;

@ActionDescription("Sets an actor Position animation")
public class PositionAnimAction implements Action {
	public enum Mode {
		DURATION, SPEED
	}

	@ActionPropertyDescription("The actor to move")
	@ActionProperty(type = Type.ACTOR)
	private String actor;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The target position")
	private Vector2 pos;

	@ActionPropertyDescription("Sets the actor position as target")
	@ActionProperty(type = Type.ACTOR)
	private String target;

	@ActionProperty(required = true, defaultValue = "1.0")
	@ActionPropertyDescription("Duration or speed in pixels/sec. mode")
	private float speed;

	@ActionProperty(required = true)
	@ActionPropertyDescription("Duration or speed of the animation")
	private Mode mode;

	@ActionProperty(required = true)
	@ActionPropertyDescription("The times to repeat. -1 for infinity")
	private int count = -1;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	private boolean wait = true;

	@ActionProperty(required = true, defaultValue = "NO_REPEAT")
	@ActionPropertyDescription("The repeat mode")

	private Tween.Type repeat = Tween.Type.NO_REPEAT; // FIXME: This adds more
														// types not present
														// here before

	@ActionProperty
	@ActionPropertyDescription("The target actor")
	private InterpolationMode interpolation;

	@Override
	public boolean run(VerbRunner cb) {

		float scale = EngineAssetManager.getInstance().getScale();

		BaseActor a = World.getInstance().getCurrentScene().getActor(actor, false);
		
		float x,y;
		
		if (target == null) {
			x = pos.x * scale; 
			y = pos.y * scale;
		} else {
			BaseActor target = World.getInstance().getCurrentScene().getActor(this.target, false);
			x = target.getX();
			y = target.getY();
		}

		if (speed == 0 || !(a instanceof SpriteActor)) {
			a.setPosition(x, y);

			return false;
		} else {
			// WARNING: only spriteactors support animation
			float s;

			if (mode != null && mode == Mode.SPEED) {
				Vector2 p0 = new Vector2(a.getX(), a.getY());

				s = p0.dst(x, y) / (scale * speed);
			} else {
				s = speed;
			}

			((SpriteActor) a).startPosAnimation(repeat, count, s, x, y, interpolation,
					wait ? cb : null);
		}

		return wait;
	}

}

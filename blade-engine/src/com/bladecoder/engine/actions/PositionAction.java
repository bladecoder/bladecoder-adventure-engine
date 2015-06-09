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

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.InterpolationUtils;

public class PositionAction implements Action {
	public static final String INFO = "Sets an actor Position animation";
	public static final Param[] PARAMS = {
			new Param("actor", "The target actor", Type.ACTOR, false),
			new Param("pos", "The target position", Type.VECTOR2, true),
			new Param("speed", "Duration or speed of the animation", Type.FLOAT, true, "1.0"),
			new Param("mode", "Duration or speed in pixels/sec. mode", Type.OPTION, false, "", new String[] {
					"duration", "speed" }),
			new Param("count", "The times to repeat", Type.INTEGER),
			new Param("wait", "If this param is 'false' the text is showed and the action continues inmediatly",
					Type.BOOLEAN, true),
			new Param("repeat", "The repeat mode", Type.OPTION, true, "no_repeat", new String[] { "repeat", "yoyo",
					"no_repeat" }),
			new Param("interpolation", "The interpolation mode", Type.OPTION, false, "", InterpolationUtils.NAMES) };

	private String actorId;
	private String mode;
	private float speed;
	private Vector2 pos;
	private int repeat = Tween.NO_REPEAT;
	private int count = 1;
	private boolean wait = true;
	private String interpolation;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		// get final position. We need to scale the coordinates to the current
		// resolution
		pos = Param.parseVector2(params.get("pos"));

		speed = Float.parseFloat(params.get("speed"));

		if (params.get("count") != null) {
			count = Integer.parseInt(params.get("count"));
		}

		mode = params.get("mode");

		if (params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}

		if (params.get("repeat") != null) {
			String repeatStr = params.get("repeat");
			if (repeatStr.equalsIgnoreCase("repeat")) {
				repeat = Tween.REPEAT;
			} else if (repeatStr.equalsIgnoreCase("yoyo")) {
				repeat = Tween.PINGPONG;
			} else if (repeatStr.equalsIgnoreCase("no_repeat")) {
				repeat = Tween.NO_REPEAT;
			} else {
				repeat = Tween.FROM_FA;
			}
		}
		
		interpolation = params.get("interpolation");	
	}

	@Override
	public boolean run(ActionCallback cb) {

		float scale = EngineAssetManager.getInstance().getScale();

		BaseActor actor = World.getInstance().getCurrentScene().getActor(actorId, false);

		if (speed == 0 || !(actor instanceof SpriteActor)) {
			actor.setPosition(pos.x * scale, pos.y * scale);

			return false;
		} else {
			// WARNING: only spriteactors support animation
			float s;

			if (mode != null && mode.equals("speed")) {
				Vector2 p0 = new Vector2(actor.getX(), actor.getY());

				s = p0.dst(pos.x * scale, pos.y * scale) / (scale * speed);
			} else {
				s = speed;
			}
			
			Interpolation i = null;
			if(interpolation != null)
				i = InterpolationUtils.getInterpolation(interpolation);

			((SpriteActor) actor)
					.startPosAnimation(repeat, count, s, pos.x * scale, pos.y * scale, i, wait ? cb : null);
		}

		return wait;
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

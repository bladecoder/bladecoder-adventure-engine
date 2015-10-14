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

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.InterpolationMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Sets an actor Position animation")
public class PositionAnimAction implements Action {
	public enum Mode {
		DURATION, SPEED
	}

	@JsonProperty("actor")
	@JsonPropertyDescription("The actor to move")
	@ActionPropertyType(Type.ACTOR)
	private String actorId;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The target position")
	@ActionPropertyType(Type.VECTOR2)
	private Vector2 pos;

	@JsonProperty
	@JsonPropertyDescription("Sets the actor position as target")
	@ActionPropertyType(Type.ACTOR)
	private String target;

	@JsonProperty(required = true, defaultValue = "1.0")
	@JsonPropertyDescription("Duration or speed in pixels/sec. mode")
	@ActionPropertyType(Type.FLOAT)
	private float speed;

	@JsonProperty
	@JsonPropertyDescription("Duration or speed of the animation")
	@ActionPropertyType(Type.OPTION)
	private Mode mode;

	@JsonProperty
	@JsonPropertyDescription("The times to repeat")
	@ActionPropertyType(Type.INTEGER)
	private int count = 1;

	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean wait = true;

	@JsonProperty(required = true, defaultValue = "NO_REPEAT")
	@JsonPropertyDescription("The repeat mode")
	@ActionPropertyType(Type.OPTION)
	private Tween.Type repeat = Tween.Type.NO_REPEAT; // FIXME: This adds more
														// types not present
														// here before

	@JsonProperty
	@JsonPropertyDescription("The target actor")
	@ActionPropertyType(Type.OPTION)
	private InterpolationMode interpolation;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		if (params.get("pos") != null) {
			pos = Param.parseVector2(params.get("pos"));
		} else if (params.get("target") != null) {
			target = params.get("target");
		}

		speed = Float.parseFloat(params.get("speed"));

		if (params.get("count") != null) {
			count = Integer.parseInt(params.get("count"));
		}

		if (params.get("mode") != null) {
			mode = Mode.valueOf(params.get("mode").trim().toUpperCase());
		}

		if (params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}

		if (params.get("repeat") != null) {
			String repeatStr = params.get("repeat");
			repeat = Tween.Type.valueOf(repeatStr.trim().toUpperCase());
		}

		if (params.get("interpolation") != null) {
			interpolation = InterpolationMode.valueOf(params.get("interpolation").trim().toUpperCase());
		}
	}

	@Override
	public boolean run(ActionCallback cb) {

		float scale = EngineAssetManager.getInstance().getScale();

		BaseActor actor = World.getInstance().getCurrentScene().getActor(actorId, false);

		if (speed == 0 || !(actor instanceof SpriteActor)) {
			if (target == null) {
				actor.setPosition(pos.x * scale, pos.y * scale);
			} else {
				BaseActor target = World.getInstance().getCurrentScene().getActor(this.target, false);
				actor.setPosition(target.getX(), target.getY());
			}

			return false;
		} else {
			// WARNING: only spriteactors support animation
			float s;

			if (mode != null && mode == Mode.SPEED) {
				Vector2 p0 = new Vector2(actor.getX(), actor.getY());

				s = p0.dst(pos.x * scale, pos.y * scale) / (scale * speed);
			} else {
				s = speed;
			}

			if (target == null) {
				((SpriteActor) actor).startPosAnimation(repeat, count, s, pos.x * scale, pos.y * scale, interpolation,
						wait ? cb : null);
			} else {
				BaseActor target = World.getInstance().getCurrentScene().getActor(this.target, false);
				
				((SpriteActor) actor).startPosAnimation(repeat, count, s, target.getX(), target.getY(), interpolation,
						wait ? cb : null);
			}
		}

		return wait;
	}

}

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

import java.text.MessageFormat;
import java.util.HashMap;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ActionDescription("Sets the animation for an actor")
public class AnimationAction implements Action {
	private static final int NO_POS = 0;
	private static final int SET_POS_ABSOLUTE = 1;
	private static final int SET_POS_RELATIVE = 2;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The Animation to set")
	@ActionPropertyType(Type.ACTOR_ANIMATION)
	private String animation;

	@JsonProperty
	@JsonPropertyDescription("The times to repeat. -1 to infinity repeat")
	@ActionPropertyType(Type.INTEGER)
	private int count = 1;

	@JsonProperty(required = true)
	@JsonPropertyDescription("If this param is 'false' the text is showed and the action continues inmediatly")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean wait = true;

	@JsonProperty(value = "animation_type", required = true, defaultValue = "SPRITE_DEFINED")
	@JsonPropertyDescription("The repeat mode")
	@ActionPropertyType(Type.STRING)
	private Tween.Type repeat = Tween.Type.SPRITE_DEFINED;       // FIXME: This adds more types not present here before

	@JsonProperty
	@JsonPropertyDescription("Puts actor position after setting the animation")
	@ActionPropertyType(Type.VECTOR2)
	private Vector2 pos;

	@JsonProperty
	@JsonPropertyDescription("Sets the position absolute or relative")
	@ActionPropertyType(Type.BOOLEAN)
	private boolean absolute = false;

	private String actorId;
	private int setPos = NO_POS;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		animation = params.get("animation");

		String a[] = Param.parseString2(animation);

		if(a[0] != null)
			actorId = a[0];

		animation = a[1];

		if (params.get("pos") != null) {
			pos = Param.parseVector2(params.get("pos"));
			setPos = SET_POS_ABSOLUTE;
		}

		if (params.get("absolute") != null) {
			boolean absolute = Boolean.parseBoolean(params.get("absolute"));

			if(absolute)
				setPos = SET_POS_ABSOLUTE;
			else
				setPos = SET_POS_RELATIVE;
		}


		if(params.get("count") != null) {
			count = Integer.parseInt(params.get("count"));
		}

		if(params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}

		if(params.get("animation_type") != null) {
			String repeatStr = params.get("animation_type");
			repeat = Tween.Type.valueOf(repeatStr.trim().toUpperCase());
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		EngineLogger.debug(MessageFormat.format("ANIMATION_ACTION: {0}", animation));

		float scale =  EngineAssetManager.getInstance().getScale();

		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId, true);

		if (setPos == SET_POS_ABSOLUTE)
			actor.setPosition(pos.x * scale, pos.y * scale);
		else if (setPos == SET_POS_RELATIVE) {
			actor.setPosition(actor.getX() + pos.x * scale, actor.getY() + pos.y * scale);
		}

		actor.startAnimation(animation, repeat, count, wait?cb:null);

		return wait;
	}

	@Override
	public Param[] getParams() {
		return null;
	}
}

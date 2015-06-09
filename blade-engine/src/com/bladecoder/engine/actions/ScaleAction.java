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
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;

public class ScaleAction implements Action {
	public static final String INFO = "Sets an actor Scale animation";
	public static final Param[] PARAMS = {
		new Param("actor", "The target actor", Type.ACTOR, false),
		new Param("scale", "The target scale", Type.FLOAT, true),
		new Param("speed", "Duration of the animation in seconds", Type.FLOAT, true, "1.0"),
		new Param("count", "The times to repeat", Type.INTEGER),
		new Param("wait", "If this param is 'false' the text is showed and the action continues inmediatly", Type.BOOLEAN, true),
		new Param("repeat", "The repeat mode", Type.STRING, true, "repeat", new String[]{"repeat", "yoyo", "no_repeat"}),
		};		
	
	private String actorId;
	private float speed;
	private float scale;
	private int repeat = Tween.NO_REPEAT;
	private int count = 1;
	private boolean wait = true;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		// get final position. We need to scale the coordinates to the current resolution
		scale = Float.parseFloat(params.get("scale"));
		
		speed = Float.parseFloat(params.get("speed"));
		
		if(params.get("count") != null) {
			count = Integer.parseInt(params.get("count"));
		}
		
		if(params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}
		
		if(params.get("repeat") != null) {
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
	}

	@Override
	public boolean run(ActionCallback cb) {
		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId, false);
		
		actor.startScaleAnimation(repeat, count, speed, scale, wait?cb:null);
		
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

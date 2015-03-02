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

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.BaseCallbackAction;
import com.bladecoder.engine.actions.Param;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.anim.Tween;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class AnimationAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Sets the animation for an actor";
	public static final Param[] PARAMS = {
		new Param("animation", "The Animation to set", Type.ACTOR_ANIMATION, true),	
		new Param("count", "The times to repeat. -1 to infinity repeat", Type.INTEGER),
		new Param("wait", "If this param is 'false' the text is showed and the action continues inmediatly", Type.BOOLEAN, true),
		new Param("animation_type", "The repeat mode", Type.STRING, true, "sprite defined", new String[]{"repeat", "yoyo", "no_repeat", "reverse", "sprite defined"}),
		new Param("pos", "Puts actor position after setting the animation", Type.VECTOR2, false),
		new Param("absolute", "Sets the position absolute or relative", Type.BOOLEAN, false)		
		};		
	
	private static final int NO_POS = 0;
	private static final int SET_POS_ABSOLUTE = 1;
	private static final int SET_POS_RELATIVE = 2;
	
	private String animation;
	private String actorId;
	private float posx, posy;
	private int setPos = NO_POS;
	private boolean reverse = false;
	private int repeat = Tween.FROM_FA;
	private int count = 1;

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		animation = params.get("animation");
		
		String a[] = Param.parseString2(animation);
		
		if(a[0] != null)
			actorId = a[0];
		
		animation = a[1];

		if (params.get("pos") != null) {
			Vector2 p = Param.parseVector2(params.get("pos"));
			posx = p.x;
			posy = p.y;
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
			setWait(Boolean.parseBoolean(params.get("wait")));
		}
		
		if(params.get("animation_type") != null) {
			String repeatStr = params.get("animation_type");
			if (repeatStr.equalsIgnoreCase("repeat")) {
				repeat = Tween.REPEAT;
			} else if (repeatStr.equalsIgnoreCase("yoyo")) {
				repeat = Tween.PINGPONG;
			} else if (repeatStr.equalsIgnoreCase("no_repeat")) {
				repeat = Tween.NO_REPEAT;
			} else if (repeatStr.equalsIgnoreCase("reverse")) {
				repeat = Tween.REVERSE;				
			} else {
				repeat = Tween.FROM_FA;
			}
		}
	}

	@Override
	public boolean run(ActionCallback cb) {
		setVerbCb(cb);
		EngineLogger.debug(MessageFormat.format("ANIMATION_ACTION: {0}", animation));
		
		float scale =  EngineAssetManager.getInstance().getScale();

		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId, true);
		
		if (setPos == SET_POS_ABSOLUTE)
			actor.setPosition(posx * scale, posy * scale);
		else if (setPos == SET_POS_RELATIVE) {		
			actor.setPosition(actor.getX() + posx * scale, actor.getY() + posy * scale);
		}
		
		actor.startAnimation(animation, repeat, count, getWait()?this:null);
		
		return getWait();
	}

	@Override
	public void write(Json json) {		
		json.writeValue("animation", animation);
		json.writeValue("actorId", actorId);
		json.writeValue("posx", posx);
		json.writeValue("posy", posy);
		json.writeValue("setPos", setPos);
		json.writeValue("reverse", reverse);
		json.writeValue("repeat", repeat);
		json.writeValue("count", count);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {	
		animation = json.readValue("animation", String.class, jsonData);
		actorId = json.readValue("actorId", String.class, jsonData);
		posx = json.readValue("posx", Float.class, jsonData);
		posy = json.readValue("posy", Float.class, jsonData);
		setPos = json.readValue("setPos", Integer.class, jsonData);
		reverse = json.readValue("reverse", Boolean.class, jsonData);
		repeat = json.readValue("repeat", Integer.class, jsonData);
		count = json.readValue("count", Integer.class, jsonData);
		super.read(json, jsonData);
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

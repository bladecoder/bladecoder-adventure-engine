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
package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class GotoAction extends BaseCallbackAction implements Action {
	public static final String INFO = "Walks to the selected position";
	public static final Param[] PARAMS = {
		new Param("pos", "The position to walk to", Type.VECTOR2),
		new Param("target", "Walks to the actor position", Type.ACTOR),
		new Param("wait", "If this param is 'false' the text is showed and the action continues inmediatly", Type.BOOLEAN, true),
		};	
	
	private String actorId;
	private Vector2 pos;
	private String targetId;
	
	private boolean wait = true;

	@Override
	public void run() {
		float scale = EngineAssetManager.getInstance().getScale();
		
		SpriteActor actor = (SpriteActor) World.getInstance().getCurrentScene().getActor(actorId, false);
		
		if(targetId!=null) {
			Actor target =  World.getInstance().getCurrentScene().getActor(targetId, false);
			if(target != null) {
				Rectangle bbox = target.getBBox().getBoundingRectangle();
				actor.goTo(new Vector2(bbox.x, bbox.y), wait?this:null);
			} else {
				EngineLogger.error("GotoAction - Target actor doesn't exists: " + targetId);
			}
		} else 
			actor.goTo(new Vector2(pos.x * scale, pos.y * scale), wait?this:null);
						
		if(!wait)
			onEvent();
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");

		if(params.get("pos") != null) {
			pos = Param.parseVector2(params.get("pos"));
		} else if(params.get("target") != null) {
			targetId = params.get("target") ;	
		}
		
		if(params.get("wait") != null) {
			wait = Boolean.parseBoolean(params.get("wait"));
		}
	}
	
	/**
	 *  If 'player' if far from 'actor', we bring it close. 
	 *  If 'player' is closed from 'actor' do nothing.
	 *  
	 *  TODO: DOESN'T WORK NOW
	 *  
	 * @param player
	 * @param actor
	 */
	@SuppressWarnings("unused")
	private void goNear(SpriteActor player, Actor actor) {
		Rectangle rdest = actor.getBBox().getBoundingRectangle();

		// Vector2 p0 = new Vector2(player.getSprite().getX(),
		// player.getSprite().getY());
		Vector2 p0 = new Vector2(player.getX(), player.getY());

		// calculamos el punto más cercano al objeto
		Vector2 p1 = new Vector2(rdest.x, rdest.y); // izquierda
		Vector2 p2 = new Vector2(rdest.x + rdest.width, rdest.y); // derecha
		Vector2 p3 = new Vector2(rdest.x + rdest.width / 2, rdest.y); // centro
		float d1 = p0.dst(p1);
		float d2 = p0.dst(p2);
		float d3 = p0.dst(p3);
		Vector2 pf;

		if (d1 < d2 && d1 < d3) {
			pf = p1;
		} else if (d2 < d1 && d2 < d3) {
			pf = p2;
		} else {
			pf = p3;
		}

		player.goTo(pf, this);
	}		
	
	@Override
	public void write(Json json) {		
		json.writeValue("pos", pos);
		json.writeValue("targetId", targetId);
		json.writeValue("actorId", actorId);
		super.write(json);	
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		pos = json.readValue("pos", Vector2.class, jsonData);
		targetId = json.readValue("targetId", String.class, jsonData);
		actorId = json.readValue("actorId", String.class, jsonData);
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

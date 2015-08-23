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
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ModelDescription("Drops the inventory actor in the scene.")
public class DropItemAction implements Action {
	@JsonProperty
	@JsonPropertyDescription("An actor in the inventory.")
	@ModelPropertyType(Type.STRING)
	private String actor;

	@JsonProperty
	@JsonPropertyDescription("Position in the scene where de actor is dropped")
	@ModelPropertyType(Type.VECTOR2)
	private Vector2 pos;

	@Override
	public void setParams(HashMap<String, String> params) {
		actor = params.get("actor");
		pos = Param.parseVector2(params.get("pos"));
	}

	@Override
	public boolean run(ActionCallback cb) {
		float scale =  EngineAssetManager.getInstance().getScale();
		
		BaseActor actor = World.getInstance().getInventory().getItem(this.actor);
		
		if(actor==null) {
			EngineLogger.error(MessageFormat.format("DropItemAction -  Item not found: {0}", this.actor));
			return false;
		}
		
		World.getInstance().getInventory().removeItem(this.actor);
		
		World.getInstance().getCurrentScene().addActor(actor);
		
		if(pos != null)
			actor.setPosition(pos.x * scale, pos.y * scale);
		
		return false;
	}

}

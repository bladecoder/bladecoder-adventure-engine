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
import com.bladecoder.engine.actions.ActionCallback;
import com.bladecoder.engine.actions.Param;

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

public class DropItemAction implements Action {
	public static final String INFO = "Drops the inventory actor in the scene.";
	public static final Param[] PARAMS = {
		new Param("actor", "An actor in the inventory.", Type.STRING, false),
		new Param("pos", "Position in the scene where de actor is dropped", Type.VECTOR2, false)
		};	
	
	String itemId;
	Vector2 pos;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		itemId = params.get("actor");
		pos = Param.parseVector2(params.get("pos"));
	}

	@Override
	public boolean run(ActionCallback cb) {
		float scale =  EngineAssetManager.getInstance().getScale();
		
		BaseActor actor = World.getInstance().getInventory().getItem(itemId);
		
		if(actor==null) {
			EngineLogger.error(MessageFormat.format("DropItemAction -  Item not found: {0}", itemId));
			return false;
		}
		
		World.getInstance().getInventory().removeItem(itemId);
		
		World.getInstance().getCurrentScene().addActor(actor);
		
		if(pos != null)
			((SpriteActor)actor).setPosition(pos.x * scale, pos.y * scale);
		
		return false;
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

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

import java.text.MessageFormat;
import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.math.Vector2;

public class DropItemAction implements Action {
	public static final String INFO = "Drops the inventory actor in the scene.";
	public static final Param[] PARAMS = {
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
	public void run() {
		float scale =  EngineAssetManager.getInstance().getScale();
		
		Actor actor = World.getInstance().getCurrentScene().getActor(itemId);
		
		if(actor==null) {
			EngineLogger.error(MessageFormat.format("DropItemAction -  Item not found: {0}", itemId));
			return;
		}
		
		World.getInstance().getInventory().removeItem(itemId);
		
		World.getInstance().getCurrentScene().addActor(actor);
		
		if(actor.isWalkObstacle() && actor.getScene().getPolygonalNavGraph() != null) {
			actor.getScene().getPolygonalNavGraph().addDinamicObstacle(actor.getBBox());
		}
		
		if(pos != null)
			((SpriteActor)actor).setPosition(pos.x * scale, pos.y * scale);
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

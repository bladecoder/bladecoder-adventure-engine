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

import com.badlogic.gdx.math.Vector2;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Drops the inventory actor in the current scene.")
public class DropItemAction implements Action {
	@ActionProperty(required=true)
	@ActionPropertyDescription("An actor in the inventory.")
	private String actor;

	@ActionProperty
	@ActionPropertyDescription("Position in the scene where de actor is dropped")
	private Vector2 pos;

	@Override
	public boolean run(VerbRunner cb) {
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

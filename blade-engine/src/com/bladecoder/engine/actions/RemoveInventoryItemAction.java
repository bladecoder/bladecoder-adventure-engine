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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@Deprecated
@ActionDescription("Remove items from the inventory. Deprecated: Use DropItem action instead.")
public class RemoveInventoryItemAction implements Action {
	@ActionPropertyDescription("The 'actorid' from the inventory item to remove. If empty remove all items.")
	@ActionProperty(type = Type.ACTOR)
	private String id;
	
	@ActionPropertyDescription("The scene where the inventory items will be dropped.")
	@ActionProperty(type = Type.SCENE, required=true)
	private String scene;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		Scene s =  w.getScene(scene);
		
		if(id != null) {
			SpriteActor a = w.getInventory().removeItem(id);
			
			if(a!=null) {
				if(s != w.getCurrentScene())
					a.dispose();
				
				s.addActor(a);
			} else {
				EngineLogger.debug("RemoveInventoryAction - Inventory actor not found: " + id);
			}
		} else {
			int n = w.getInventory().getNumItems();
			
			for(int i = 0; i < n; i++) {
				SpriteActor a = w.getInventory().get(i);			
				s.addActor(a);
			}
			
			w.getInventory().removeAllItems();
		}	
		
		return false;
	}

}

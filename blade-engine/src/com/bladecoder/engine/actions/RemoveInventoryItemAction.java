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
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.World;

@ActionDescription("Remove items from the inventory.")
public class RemoveInventoryItemAction implements Action {
	@ActionPropertyDescription("The 'actorid' from the inventory item to remove. If empty remove all items.")
	@ActionProperty(type = Type.ACTOR)
	private String id;

	@Override
	public boolean run(ActionCallback cb) {
		
		if(id!=null) {
			SpriteActor a = World.getInstance().getInventory().removeItem(id);
			if(a!=null)
				a.dispose();
		} else {
			World.getInstance().getInventory().removeAllItems();
		}
		
		return false;
	}

}

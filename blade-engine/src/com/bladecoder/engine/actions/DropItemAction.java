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
import com.badlogic.gdx.utils.Disposable;
import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription("Drops the selected inventory actor.")
public class DropItemAction implements Action {
	@ActionProperty
	@ActionPropertyDescription("The 'id' from the inventory item to remove. If empty remove all items.")
	private String actor;

	@ActionProperty(type = Type.SCENE, required = false)
	@ActionPropertyDescription("The target scene. If not selected the item is dropped in the current scene.")
	private String scene = null;

	@ActionProperty
	@ActionPropertyDescription("Position in the scene where de actor is dropped")
	private Vector2 pos;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene ts = null;

		if (scene == null)
			ts = w.getCurrentScene();
		else
			ts = w.getScene(scene);

		
		BaseActor a;
		
		if (actor != null) {
			a = w.getInventory().get(actor);

			if (a == null) {
				EngineLogger.error(MessageFormat.format("DropItemAction -  Item not found: {0}", actor));
				return false;
			}

			removeActor(ts, a);
		} else {
			int n = w.getInventory().getNumItems();
			
			for(int i = n - 1; i >= 0; i--) {
				a = w.getInventory().get(i);
				
				removeActor(ts, a);
			}
		}

		return false;
	}

	private void removeActor(Scene ts, BaseActor a) {

		float scale = EngineAssetManager.getInstance().getScale();

		w.getInventory().removeItem(a.getId());

		if (ts != w.getCurrentScene() && w.getCachedScene(ts.getId()) == null && a instanceof Disposable)
			((Disposable) a).dispose();

		ts.addActor(a);

		if (pos != null)
			a.setPosition(pos.x * scale, pos.y * scale);
	}

}

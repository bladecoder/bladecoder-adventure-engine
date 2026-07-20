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

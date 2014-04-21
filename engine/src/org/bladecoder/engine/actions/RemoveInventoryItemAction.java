package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;

public class RemoveInventoryItemAction implements Action {
	public static final String INFO = "Remove items from the inventory. If the removed items are not referenced in other actions.\n" +
			"WARNING: This action does not 'dispose' the item.";
	public static final Param[] PARAMS = {
		new Param("id", "The 'actorid' from the inventory item to remove. If empty remove all items.", Type.STRING)
		};		
	
	String itemId;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		itemId = params.get("id");
	}

	@Override
	public void run() {
		
		if(itemId!=null)
			World.getInstance().getInventory().removeItem(itemId);
		else
			World.getInstance().getInventory().removeAllItems();
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

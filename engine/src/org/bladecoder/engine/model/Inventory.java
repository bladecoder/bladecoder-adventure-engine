package org.bladecoder.engine.model;

import java.util.ArrayList;

import org.bladecoder.engine.assets.AssetConsumer;
import org.bladecoder.engine.util.EngineLogger;

public class Inventory implements AssetConsumer {
	ArrayList<SpriteActor> items;
	
	private boolean visible = true;

	public Inventory() {
		items = new ArrayList<SpriteActor>();
	}

	public int getNumItems() {
		return items.size();
	}

	public SpriteActor getItem(int i) {
		return items.get(i);
	}

	public SpriteActor getItem(String actorId) {
		for (SpriteActor a : items) {
			if (a.getId().equals(actorId))
				return a;
		}

		return null;
	}

	public void addItem(SpriteActor actor) {		
		if(!items.contains(actor))
			items.add(actor);
		else
			EngineLogger.error("Actor already in inventory: " + actor.getId());

		//actor.setFrameAnimation("inventory");
	}

	public void removeItem(SpriteActor item) {
		items.remove(item);
	}
	
	public void removeItem(String item) {
		for (SpriteActor a : items) {
			if (a.getId().equals(item)){
				items.remove(a);
				break;
			}
		}	
	}
	
	public void removeAllItems() {
		items.clear();
	}	

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	@Override
	public void loadAssets() {
		for (SpriteActor a : items)
			a.loadAssets();		
	}
	
	@Override
	public void retrieveAssets() {
		for (SpriteActor a : items) {
			a.retrieveAssets();
		}
	}	
	
	@Override
	public void dispose() {
		for (SpriteActor a : items)
			a.dispose();
	}
	
}

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
package com.bladecoder.engine.model;

import java.util.ArrayList;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.actions.SceneActorRef;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.util.EngineLogger;

public class Inventory implements AssetConsumer, Serializable  {
	private ArrayList<SpriteActor> items;
	
	private boolean visible = true;
	transient private boolean disposed= true;

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
		if(!items.contains(actor)) {
			items.add(actor);
		} else
			EngineLogger.error("BaseActor already in inventory: " + actor.getId());

	}

	public void removeItem(SpriteActor item) {
		items.remove(item);
	}
	
	public SpriteActor removeItem(String item) {
		for (SpriteActor a : items) {
			if (a.getId().equals(item)){
				items.remove(a);
				return a;
			}
		}
		
		return null;
	}
	
	/**
	 * Dispose and remove all items.
	 */
	public void removeAllItems() {
		for (SpriteActor a : items) {
			a.dispose();
		}
		
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
		
		disposed = false;
	}	
	
	@Override
	public void dispose() {
		for (SpriteActor a : items)
			a.dispose();
		
		disposed = true;
	}
	
	public boolean isDisposed() {
		return disposed;
	}

	
	@Override
	public void write(Json json) {
		SceneActorRef actorRef;
		
		json.writeValue("visible", visible);
		
		json.writeObjectStart("items");
		for(SpriteActor a:items) {
			actorRef = new SceneActorRef(a.getInitScene(), a.getId());
			json.writeValue(actorRef.toString(), a);
		}
		json.writeObjectEnd();
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		visible = json.readValue("visible", Boolean.class, jsonData);
		
		items.clear();
		
		JsonValue jsonValueActors = jsonData.get("items");
		SceneActorRef actorRef;
		
		for(int i = 0; i < jsonValueActors.size; i++) {
			JsonValue jsonValueAct = jsonValueActors.get(i);
			actorRef = new SceneActorRef(jsonValueAct.name);
			Scene sourceScn = World.getInstance().getScene(actorRef.getSceneId());
			BaseActor actor = sourceScn.getActor(actorRef.getActorId(), false);
			actor.read(json, jsonValueAct);
			items.add((SpriteActor)actor);
		}
	}
}

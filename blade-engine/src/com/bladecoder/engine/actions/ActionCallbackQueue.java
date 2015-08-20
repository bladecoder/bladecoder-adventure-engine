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

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.model.World.AssetState;
import com.bladecoder.engine.util.ActionCallbackSerialization;

/**
 * This is a queue to group all cb that must be triggered in the next iteration.
 * This class is neccessary to avoid concurrency problems.
 * 
 * All ActionCb must be called at once outside update methods of Actors.
 * 
 * @author rgarcia
 *
 */
public class ActionCallbackQueue {
	private static final List<ActionCallback> queue = new ArrayList<ActionCallback>();
	private static final List<ActionCallback> runQueue = new ArrayList<ActionCallback>();
	
	public static void add(ActionCallback cb) {
		if(World.getInstance().getAssetState() == AssetState.LOADED)
			queue.add(cb);
	}
	
	/**
	 * Resume all cb's in the 'queue'. 
	 * 
	 * To do that, we copy all elements in the 'runQueue' and clean the 'queue' because 
	 * cb.resume() can trigger more cb's
	 */
	public static void run() {
		if(!queue.isEmpty()) {
			runQueue.addAll(queue);				
			queue.clear();
			
			for(ActionCallback cb: runQueue) {
				cb.resume();
								
				// Break when changing scene
				if(World.getInstance().getAssetState() != AssetState.LOADED)
					break;
			}
			
			runQueue.clear();
		}
	}
	
	public static void clear() {
		queue.clear();
	}
	
	public static void write(Json json) {
		ArrayList<String> q = new ArrayList<String>();
		for(ActionCallback cb: queue) {
			q.add(ActionCallbackSerialization.find(cb));
		}
		
		json.writeValue("queue", q);
	}
	
	@SuppressWarnings("unchecked")
	public static void read (Json json, JsonValue jsonData) {
		ArrayList<String> q = json.readValue("queue", ArrayList.class, String.class,
				jsonData);
		
		queue.clear();
		
		for(String s: q) {
			queue.add(ActionCallbackSerialization.find(s));
		}
	}
}

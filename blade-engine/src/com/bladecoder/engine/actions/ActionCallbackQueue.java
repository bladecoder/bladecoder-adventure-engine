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

import com.bladecoder.engine.actions.ActionCallback;

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
	
	public static void add(ActionCallback cb) {
		queue.add(cb);
	}
	
	public static void run() {
		if(!queue.isEmpty()) {
			ActionCallback[] array = queue.toArray(new ActionCallback[queue.size()]);
			
			queue.clear();
			
			for(ActionCallback cb: array)
				cb.resume();
		}
	}
	
	public static void clear() {
		queue.clear();
	}
	
	// TODO: SAVE AND RESUME QUEUE
}

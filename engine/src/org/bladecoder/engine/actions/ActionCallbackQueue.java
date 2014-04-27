package org.bladecoder.engine.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
				cb.onEvent();
		}
	}
}

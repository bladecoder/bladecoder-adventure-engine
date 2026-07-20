package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

public interface Action {
	
	/**
	 * This is to inject the world dependency.
	 * 
	 * @param w The world to inject
	 */
    void init(World w);
	
	
	/**
	 * Execute the action
	 * 
	 * @param cb
	 * @return If 'true', the verb must stops the execution and wait
	 * for the action to call the cb.resume()
	 */
    boolean run(VerbRunner cb);
}

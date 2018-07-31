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

import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

public interface Action {
	
	/**
	 * This is to inject the world dependency.
	 * 
	 * @param w The world to inject
	 */
	public void init(World w);
	
	
	/**
	 * Execute the action
	 * 
	 * @param cb
	 * @return If 'true', the verb must stops the execution and wait
	 * for the action to call the cb.resume()
	 */
	public boolean run(VerbRunner cb);
}

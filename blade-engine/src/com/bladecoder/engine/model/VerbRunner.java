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

import com.bladecoder.engine.actions.Action;
import com.bladecoder.engine.actions.ActionCallback;

/**
 * 
 * Interface to define the methods needed to handle and execute verbs.
 * 
 * @author rgarcia
 *
 */
public interface VerbRunner extends ActionCallback {
	
	/**
	 * Method to retrieve the action list
	 * @return the action list
	 */
	public ArrayList<Action> getActions();
	
	/**
	 * Run the verb
	 */
	public void run();

	/**
	 * Return the current action pointer
	 */
	public int getIP();
	
	/**
	 * Sets the action pointer
	 * @param ip the pointer (the action index to execute)
	 */
	public void setIP(int ip);

	/**
	 * Sets the IP to the end of the queue finishing the verb execution
	 */
	public void cancel();
	
	
	/**
	 * The target actor in 'use' verb.
	 * @return The target actor 'id'
	 */
	public String getTarget();
}

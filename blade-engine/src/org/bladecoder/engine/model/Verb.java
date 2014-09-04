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
package org.bladecoder.engine.model;

import java.util.ArrayList;

import org.bladecoder.engine.actions.Action;
import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.actions.RunVerbAction;
import org.bladecoder.engine.util.EngineLogger;

public class Verb implements ActionCallback {
	private String id;
	
	private ArrayList<Action> actions = new ArrayList <Action>();
	
	private int ip = -1;
	
	public Verb() {
	}
	
	public Verb(String id) {
		this.id=id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public void add(Action a) {
		actions.add(a);
	}
	
	public ArrayList<Action> getActions() {
		return actions;
	}
	
	public void run() {
		ip = 0;
		nextStep();
	}
	
	public void nextStep() {
		
		boolean stop = false;
		
		while( !isFinished() && !stop) {
			Action a = actions.get(ip);
			if(a.waitForFinish(this)) {
				stop = true;
			} else {
				ip++;
			}
			
			try {
				a.run();
			} catch (Exception e) {
				EngineLogger.error("EXCEPTION EXECUTING ACTION: " + a.getClass().getSimpleName(), e);
			}		
		}
	}
	
	private boolean isFinished() {
		return ip >= actions.size();
	}

	@Override
	public void resume() {
		ip++;
		nextStep();	
	}


	public void cancel() {
		for(Action c:actions) {
			if(c instanceof RunVerbAction)
				((RunVerbAction)c).cancel();
		}		
		
		ip = actions.size();
	}	
}

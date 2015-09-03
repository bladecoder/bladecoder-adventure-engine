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

import com.bladecoder.engine.actions.AbstractAction;
import com.bladecoder.engine.actions.ModelDescription;
import com.bladecoder.engine.actions.ModelPropertyType;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.util.EngineLogger;
import com.bladecoder.engine.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;

@ModelDescription("Verbs are used to create the game interaction. Select or write the verb to create")
public class Verb extends AbstractModel implements VerbRunner {
	static final char COMPOSITE_ID_SEPARATOR = '.';

	public static final String LOOKAT_VERB = "lookat";
	public static final String ACTION_VERB = "pickup";
	public static final String LEAVE_VERB = "leave";
	public static final String ENTER_VERB = "enter";
	public static final String EXIT_VERB = "exit";
	public static final String TALKTO_VERB = "talkto";
	public static final String USE_VERB = "use";
	public static final String GOTO_VERB = "goto";
	public static final String TEST_VERB = "test";
	public static final String INIT_VERB = "init";
	public static final String CUSTOM_VERB = "custom";

	@JsonProperty
	@JsonPropertyDescription("The target actor id for the 'use' verb")
	@ModelPropertyType(Param.Type.ACTOR)
	private String target;

	@JsonProperty
	@JsonPropertyDescription("The state")
	@ModelPropertyType(Param.Type.STRING)
	private String state;

	@JsonProperty
	private List<AbstractAction> actions = new ArrayList<>();

	private int ip = -1;

	public Verb() {
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	@TrackPropertyChanges
	public void addAction(AbstractAction action) {
		actions.add(action);
	}

	@TrackPropertyChanges
	public void removeAction(AbstractAction action) {
		actions.remove(action);
	}

	public List<AbstractAction> getActions() {
		return actions;
	}
	
	public void run() {
		if(EngineLogger.debugMode())
			EngineLogger.debug(">>> Running verb: "+ id);
		
		ip = 0;
		nextStep();
	}
	
	public void nextStep() {
		
		boolean stop = false;
		
		while( !isFinished() && !stop) {
			AbstractAction a = actions.get(ip);
			
			if(EngineLogger.debugMode())
				EngineLogger.debug(ip + ". " + a.getClass().getSimpleName());
			
			try {
				if(a.run(this))
					stop = true;
				else
					ip++;
			} catch (Exception e) {
				EngineLogger.error("EXCEPTION EXECUTING ACTION: " + a.getClass().getSimpleName(), e);
				ip++;
			}
		}
		
		if(EngineLogger.debugMode() && isFinished())
			EngineLogger.debug(">>> Verb FINISHED: "+ id);
	}
	
	private boolean isFinished() {
		return ip >= actions.size();
	}

	@Override
	public void resume() {
		ip++;
		nextStep();
	}

	public int getIP() {
		return ip;
	}
	
	public void setIP(int ip) {
		this.ip = ip;
	}

	public void cancel() {
		for (AbstractAction c : actions) {
			if (c instanceof VerbRunner)
				((VerbRunner) c).cancel();
		}

		ip = actions.size();
	}

	public String getCompositeId() {
		final StringBuilder sb = new StringBuilder(50);
		sb.append(id);

		if (!StringUtils.isEmpty(target)) {
			sb.append(COMPOSITE_ID_SEPARATOR).append(target);
		}

		if (!StringUtils.isEmpty(state)) {
			sb.append(COMPOSITE_ID_SEPARATOR).append(state);
		}

		return sb.toString();
	}
}

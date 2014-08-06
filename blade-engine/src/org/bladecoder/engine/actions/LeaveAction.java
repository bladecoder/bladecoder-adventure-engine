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
package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;
import org.bladecoder.engine.util.EngineLogger;


public class LeaveAction implements Action {
	public static final String INFO = "Change the current scene. The target scene must exists in the current chapter.";
	public static final Param[] PARAMS = {
		new Param("scene", "The target scene", Type.STRING, false),
		new Param("chapter", "The target chapter", Type.CHAPTER, false)
		};		
	
	String scene;
	String chapter;

	@Override
	public void run() {
		EngineLogger.debug("LEAVE ACTION");
		
		if(chapter == null || chapter.isEmpty())
			World.getInstance().setCurrentScene(scene);
		else
			World.getInstance().loadXML(chapter, scene);
	}

	@Override
	public void setParams(HashMap<String, String> params) {
		scene = params.get("scene");
		chapter = params.get("chapter");
	}


	@Override
	public String getInfo() {
		return INFO;
	}

	@Override
	public Param[] getParams() {
		return PARAMS;
	}
}

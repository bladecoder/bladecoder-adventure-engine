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
import org.bladecoder.engine.model.Actor;
import org.bladecoder.engine.model.World;

public class SoundAction implements Action {
	public static final String INFO = "Play/Stop a sound";
	public static final Param[] PARAMS = {
		new Param("play", "The 'soundId' to play", Type.STRING),
		new Param("stop", "The 'soundId' to stop", Type.STRING)
		};		
	
	String actorId;
	String play;
	String stop;
	
	@Override
	public void setParams(HashMap<String, String> params) {
		actorId = params.get("actor");
		play = params.get("play");
		stop = params.get("stop");
	}

	@Override
	public void run() {
		
		Actor actor = World.getInstance().getCurrentScene().getActor(actorId, true);
		
		if(play!= null)	actor.playSound(play);
		
		if(stop!= null)	actor.stopSound(stop);
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

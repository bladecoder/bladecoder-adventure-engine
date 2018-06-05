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

import com.bladecoder.engine.model.MusicManager;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Change the volume of the current playing music.")
public class MusicVolumeAction implements Action {
	
	@ActionProperty(required = true, defaultValue = "1.0")
	@ActionPropertyDescription("Volume of the music [0-1].")
	private float volume = 1.0f;
	
	@ActionProperty(required = true, defaultValue = "0.0")
	@ActionPropertyDescription("For volume fade")
	private float duration;

	@ActionProperty(required = true)
	@ActionPropertyDescription("If this param is 'false' the action continues inmediatly")
	private boolean wait = true;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		MusicManager musicEngine = w.getMusicManager();
		
		if(duration==0) {
			musicEngine.setVolume(volume);
			return false;
		} else {
			w.getMusicManager().fade(volume, duration, wait?cb:null);
		}
		
		return wait;
	}

}

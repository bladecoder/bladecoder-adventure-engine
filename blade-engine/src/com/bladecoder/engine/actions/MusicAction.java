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

import com.bladecoder.engine.model.MusicDesc;
import com.bladecoder.engine.model.MusicManager;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Plays/Stops music.")
public class MusicAction implements Action {
	
	@ActionProperty
	@ActionPropertyDescription("The music filename to play. If empty, the current music will be stopped.")
	private String filename;
	
	@ActionProperty(required = true, defaultValue = "false")
	@ActionPropertyDescription("Music Loop")
	private boolean loop = false;
	
	@ActionProperty(required = true, defaultValue = "0")
	@ActionPropertyDescription("The music starts to play after the delay.")
	private float initialDelay = 0;
	
	@ActionProperty(required = true, defaultValue = "-1")
	@ActionPropertyDescription("Time for repeating the music when no looping. -1 for no repeat.")
	private float repeatDelay = -1;
	
	@ActionProperty(required = true, defaultValue = "true")
	@ActionPropertyDescription("Stops the music when leaving the current scene.")
	private boolean stopWhenLeaving = true;
	
	@ActionProperty(required = true, defaultValue = "1.0")
	@ActionPropertyDescription("Volume of the music [0-1].")
	private float volume = 1.0f;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		MusicManager musicEngine = w.getMusicManager();
		
		if(filename == null) {
			musicEngine.setMusic(null);
		} else {
			MusicDesc md = new MusicDesc();
			
			md.setFilename(filename);
			md.setLoop(loop);
			md.setInitialDelay(initialDelay);
			md.setRepeatDelay(repeatDelay);
			md.setStopWhenLeaving(stopWhenLeaving);
			md.setVolume(volume);
			
			musicEngine.setMusic(md);
		}
		
		return false;
	}

}

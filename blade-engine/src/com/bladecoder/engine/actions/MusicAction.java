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
import com.bladecoder.engine.model.MusicEngine;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Plays/Stops music.")
public class MusicAction implements Action {
	
	@ActionProperty
	@ActionPropertyDescription("The music filename to play. If empty, the current music will be stopped.")
	private String filename;
	
	@ActionProperty(required = true)
	@ActionPropertyDescription("Music Loop")
	private boolean loop = false;
	
	@ActionProperty(required = true)
	@ActionPropertyDescription("The music starts to play after the delay.")
	private float initialDelay = 0;
	
	@ActionProperty(required = true)
	@ActionPropertyDescription("Time for repeating the music when no looping. -1 for no repeat.")
	private float repeatDelay = -1;
	
	@ActionProperty(required = true)
	@ActionPropertyDescription("Stops the music when leaving the current scene.")
	private boolean stopWhenLeaving = true;

	@Override
	public boolean run(VerbRunner cb) {
		MusicEngine musicEngine = World.getInstance().getMusicEngine();
		
		if(filename == null) {
			musicEngine.setMusic(null);
		} else {
			MusicDesc md = new MusicDesc();
			
			md.setFilename(filename);
			md.setLoop(loop);
			md.setInitialDelay(initialDelay);
			md.setRepeatDelay(repeatDelay);
			md.setStopWhenLeaving(stopWhenLeaving);
			
			musicEngine.setMusic(md);
		}
		
		return false;
	}

}

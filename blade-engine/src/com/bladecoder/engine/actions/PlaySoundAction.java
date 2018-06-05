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

import com.bladecoder.engine.actions.Param.Type;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;

@ActionDescription("Play/Stop a sound")
public class PlaySoundAction implements Action {
	@ActionPropertyDescription("The 'soundId' to play. ")
	@ActionProperty(required = true, type = Type.SOUND)
	private String sound;
	
	@ActionProperty(required = true, defaultValue = "false")
	@ActionPropertyDescription("When 'true' stops the sound instead of playing it.")
	private boolean stop = false;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		if(!stop)	{
			w.getCurrentScene().getSoundManager().playSound(sound);
		} else {
			w.getCurrentScene().getSoundManager().stopSound(sound);
		}
		
		return false;
	}


}

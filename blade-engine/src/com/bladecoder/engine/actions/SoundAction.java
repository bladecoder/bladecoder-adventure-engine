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

@Deprecated
@ActionDescription("Play/Stop a sound. Deprecated: Use PlaySound action instead.")
public class SoundAction implements Action {
	@ActionPropertyDescription("The target actor")
	@ActionProperty(type = Type.INTERACTIVE_ACTOR, required = true)
	private String actor;

	@ActionPropertyDescription("The actor 'soundId' to play. If empty the current sound will be stopped.")
	@ActionProperty(type = Type.STRING)
	private String play;
	
	private World w;
	
	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		
		if(play!= null)	{
			w.getCurrentScene().getSoundManager().playSound(actor + "_" + play);
		} else {
			w.getCurrentScene().getSoundManager().stopCurrentSound(actor);
		}
		
		return false;
	}


}

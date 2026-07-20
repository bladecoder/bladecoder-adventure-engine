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

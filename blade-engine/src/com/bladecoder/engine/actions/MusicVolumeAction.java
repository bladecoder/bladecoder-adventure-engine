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

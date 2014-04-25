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
		
		Actor actor = World.getInstance().getCurrentScene().getActor(actorId);
		
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

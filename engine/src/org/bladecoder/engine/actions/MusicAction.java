package org.bladecoder.engine.actions;

import java.util.HashMap;

import org.bladecoder.engine.actions.Param.Type;
import org.bladecoder.engine.model.World;

public class MusicAction implements Action {
	public static final String INFO = "Play/Stop the music of the current scene";
	public static final Param[] PARAMS = { new Param("play",
			"Play/Stops the music of the scene", Type.BOOLEAN, true), };

	String play;

	@Override
	public void setParams(HashMap<String, String> params) {
		play = params.get("play");
	}

	@Override
	public void run() {
		boolean p = Boolean.parseBoolean(play);

		if (p)
			World.getInstance().getCurrentScene().playMusic();
		else
			World.getInstance().getCurrentScene().stopMusic();
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

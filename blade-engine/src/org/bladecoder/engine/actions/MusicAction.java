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

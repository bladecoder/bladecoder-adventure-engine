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
package org.bladecoder.engine.model;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;


/**
 * 
 * TODO: Impossible to implement with the current LIBGDX!!!!
 * 
 * @author rgarcia
 *
 */
public class AudioManager  {
	private static AudioManager instance = null;
	
	Music music;

	public static AudioManager getInstance() {
		if (instance == null) {
			instance = new AudioManager();
		}

		return instance;
	}
	
	public void stopAll() {
		
	}
	
	public void pause() {
		
	}
	
	public void resume() {
		
	}

	public static void write(Json json) {
		json.writeValue("music", (String)null);
		json.writeValue("sounds", (String)null);
	}

	public static void read (Json json, JsonValue jsonData) {
		getInstance().stopAll();

//		@SuppressWarnings("unchecked")
//		ArrayList<EngineTween> tweens = json.readValue("tweens",
//				ArrayList.class, EngineTween.class, jsonData);
//
//		for (EngineTween st : tweens) {
//			st.start();
//		}
	}
}

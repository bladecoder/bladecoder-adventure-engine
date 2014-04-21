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

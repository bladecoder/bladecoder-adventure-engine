package com.bladecoder.engine.model;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Music.OnCompletionListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.EngineLogger;

/**
 * Manager to play/load/dispose voices.
 * 
 * Plays a voice file, if another voice is playing, stops it before playing the
 * new voice.
 * 
 * @author rgarcia
 */
public class VoiceManager implements Serializable, AssetConsumer {
	transient private Music voice = null;
	
	String fileName = null;

	private boolean isPlayingSer = false;
	private float voicePosSer = 0;
	
	// the master volume
	private float volume = 1.0f;
	
	transient private boolean isPaused = false;
	transient private TextManager textManager = null;

	public VoiceManager(TextManager textManager) {
		this.textManager = textManager;
	}

	public void pause() {
		if (voice != null && voice.isPlaying()) {
			voice.pause();
			isPaused = true;
		}
	}

	public void resume() {
		if (voice != null && isPaused) {
			voice.play();
			isPaused = false;
		}
	}

	public void stop() {
		if (voice != null) {
			voice.stop();

			dispose();
		}
	}

	public void play(String fileName) {
		stop();
		
		this.fileName = fileName;
		
		if (fileName != null) {
			retrieveAssets();
			voice.play();
		}
	}
	

	public void setVolume(float volume) {
		this.volume = volume;
		
		if(voice != null)
			voice.setVolume(volume);
	}



	@Override
	public void dispose() {
		if (voice != null) {
			
			if(voice.isPlaying())
				voice.stop();
			
			EngineLogger.debug("DISPOSING VOICE: " + fileName);
			EngineAssetManager.getInstance().unload(EngineAssetManager.VOICE_DIR + fileName);
			
			voice = null;
			fileName = null;
			isPlayingSer = false;
			voicePosSer = 0;
		}
	}

	@Override
	public void loadAssets() {
		if (voice == null && fileName != null) {
			EngineLogger.debug("LOADING VOICE: " + fileName);
			EngineAssetManager.getInstance().load(EngineAssetManager.VOICE_DIR + fileName, Music.class);
		}
	}

	@Override
	public void retrieveAssets() {
		if (voice == null && fileName != null) {
			
			if(!EngineAssetManager.getInstance().isLoaded(EngineAssetManager.VOICE_DIR + fileName)) {
				loadAssets();
				EngineAssetManager.getInstance().finishLoading();
			}
			
			EngineLogger.debug("RETRIEVING VOICE: " + fileName);
			
			voice = EngineAssetManager.getInstance().get(EngineAssetManager.VOICE_DIR + fileName, Music.class);
			
			voice.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(Music music) {
					textManager.next();
				}
			});
			
			if(voice != null)
				voice.setVolume(volume);

			if (isPlayingSer) {
				voice.play();
				
				if (voice != null) {
					voice.setPosition(voicePosSer);
				}

				isPlayingSer = false;
				voicePosSer = 0f;
			}
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("fileName", fileName);
		json.writeValue("isPlaying", voice != null && (voice.isPlaying()|| isPaused));
		json.writeValue("musicPos", voice != null && (voice.isPlaying()|| isPaused) ? voice.getPosition() : 0f);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		fileName = json.readValue("fileName", String.class, jsonData);
		isPlayingSer = json.readValue("isPlaying", boolean.class, false, jsonData);
		voicePosSer = json.readValue("musicPos", float.class, 0f, jsonData);
	}
}

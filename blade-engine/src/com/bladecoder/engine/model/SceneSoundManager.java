package com.bladecoder.engine.model;

import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.bladecoder.engine.util.EngineLogger;

/**
 * Manages scene sounds.
 * 
 * @author rgarcia
 */
public class SceneSoundManager implements Serializable, AssetConsumer {

	private HashMap<String, LoadedSound> loadedSounds = new HashMap<String, LoadedSound>(0);
	private World w;
	
	public SceneSoundManager(World w) {
		this.w = w;
	}

	public void addSoundToLoad(SoundDesc s) {
		loadedSounds.put(s.getId(), new LoadedSound(s));
	}

	public void playSound(String id) {

		LoadedSound s = loadedSounds.get(id);

		if (s == null) {
			// Not loaded, load and add to the loaded list.
			SoundDesc sd = w.getSounds().get(id);

			if (sd != null) {
				addSoundToLoad(sd);
				s = loadedSounds.get(id);

				EngineLogger.debug("LOADING SOUND: " + s.desc.getId() + " - " + s.desc.getFilename());
				EngineAssetManager.getInstance().loadSound(s.desc.getFilename());
				EngineAssetManager.getInstance().finishLoading();
				s.sound = EngineAssetManager.getInstance().getSound(s.desc.getFilename());
			}
		}

		if (s != null && s.sound != null) {
			if (s.desc.getLoop())
				s.sound.loop(s.desc.getVolume(), s.desc.getPitch(), s.desc.getPan());
			else
				s.sound.play(s.desc.getVolume(), s.desc.getPitch(), s.desc.getPan());

			s.playing = true;
		} else {
			EngineLogger.error("Sound Not Found: " + id);
		}
	}

	/**
	 * Deletes the current sound played by the actor.
	 * 
	 * Note that actor sounds are like: actor_id
	 * 
	 * @param actor
	 */
	public void stopCurrentSound(String actor) {
		for (LoadedSound s : loadedSounds.values()) {
			String start = actor + "_";

			if (s.desc.getId().startsWith(start) && s.playing)
				stopSound(s.desc.getId());
		}
	}

	public void stopSound(String id) {
		LoadedSound s = loadedSounds.get(id);

		if (s != null) {
			s.sound.stop();
			s.playing = false;
		} else {
			EngineLogger.debug("Sound Not Found: " + id);
		}
	}

	public void stop() {
		for (LoadedSound s : loadedSounds.values()) {
			if (s.playing && s.sound != null)
				stopSound(s.desc.getId());
		}
	}

	public void resume() {
		for (LoadedSound s : loadedSounds.values()) {
			if (s.playing && s.sound != null)
				s.sound.resume();
		}
	}

	public void pause() {
		for (LoadedSound s : loadedSounds.values()) {
			if (s.playing && s.sound != null)
				s.sound.pause();
		}
	}

	@Override
	public void dispose() {
		for (LoadedSound s : loadedSounds.values()) {
			// EngineLogger.debug("DISPOSING SOUND: " + s.desc.getId() + " - " +
			// s.desc.getFilename());
			if (s.playing)
				s.sound.stop();

			EngineAssetManager.getInstance().disposeSound(s.desc.getFilename());
		}
	}

	@Override
	public void loadAssets() {
		for (LoadedSound s : loadedSounds.values()) {
			// EngineLogger.debug("LOADING SOUND: " + s.desc.getId() + " - " +
			// s.desc.getFilename());
			EngineAssetManager.getInstance().loadSound(s.desc.getFilename());
		}

		// Due to a bug in Android, try lo load the sounds as early as possible.
		if (Gdx.app.getType() == ApplicationType.Android)
			EngineAssetManager.getInstance().update();
	}

	@Override
	public void retrieveAssets() {
		for (LoadedSound s : loadedSounds.values()) {
			s.sound = EngineAssetManager.getInstance().getSound(s.desc.getFilename());

			// restore playing looping
			if (s.playing) {
				s.playing = false;

				if (s.desc.getLoop())
					playSound(s.desc.getId());
			}

		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("loadedSounds", loadedSounds, loadedSounds.getClass(), SoundDesc.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		loadedSounds = json.readValue("loadedSounds", HashMap.class, SoundDesc.class, jsonData);

		if (loadedSounds == null)
			loadedSounds = new HashMap<String, LoadedSound>(0);

		// Retrieve desc from World sound description.
		for (Entry<String, LoadedSound> e : loadedSounds.entrySet()) {
			e.getValue().desc = w.getSounds().get(e.getKey());
		}
	}

	static class LoadedSound {
		transient SoundDesc desc;
		transient Sound sound;

		// flag to restore the sound when looping.
		boolean playing = false;

		public LoadedSound(SoundDesc s) {
			desc = s;
		}

		public LoadedSound() {

		}
	}
}

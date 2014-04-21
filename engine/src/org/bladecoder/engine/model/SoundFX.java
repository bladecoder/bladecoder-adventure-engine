package org.bladecoder.engine.model;

import org.bladecoder.engine.assets.AssetConsumer;
import org.bladecoder.engine.assets.EngineAssetManager;

import com.badlogic.gdx.audio.Sound;

public class SoundFX implements AssetConsumer {
	transient private Sound s;
	private boolean loop;
	private String filename;
	private float volume = 1f;
	
	public SoundFX() {
		
	}
	
	public SoundFX(String filename, boolean loop, float volume) {
		this.filename = filename;
		this.loop = loop;
		this.volume = volume;
	}
	
	public void play() {
		if(loop) s.loop();
		else s.play(volume);
	}

	public void stop() {
		s.stop();
	}
	
	public boolean isLooping() {
		return loop;
	}
	
	@Override
	public void loadAssets() {
		EngineAssetManager.getInstance().loadSound(filename);
	}
	
	@Override
	public void retrieveAssets() {
		s = EngineAssetManager.getInstance().getSound(filename);
	}
	
	@Override
	public void dispose() {
		EngineAssetManager.getInstance().disposeSound(filename);
	}
}

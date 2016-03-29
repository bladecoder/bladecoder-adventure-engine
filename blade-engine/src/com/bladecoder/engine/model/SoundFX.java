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
package com.bladecoder.engine.model;

import com.badlogic.gdx.audio.Sound;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;

public class SoundFX implements AssetConsumer {
	transient private Sound s;
	private boolean loop;
	private String filename;
	private float volume = 1f;
	
	public SoundFX() {
		
	}
	
	public SoundFX(String filename, boolean loop, float volume) {
		this.setFilename(filename);
		this.loop = loop;
		this.setVolume(volume);
	}
	
	public void play() {
		if(s==null)
			return;
		
		if(loop) s.loop();
		else s.play(getVolume());
	}

	public void stop() {
		if(s==null)
			return;
		
		s.stop();
	}
	
	public void pause() {
		if(s==null)
			return;
		
		s.pause();
	}
	
	public void resume() {
		if(s==null)
			return;
		
		s.resume();
	}
	
	public boolean getLoop() {
		return loop;
	}
	
	public void setLoop(boolean loop) {
		this.loop = loop;
	}
	
	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public void loadAssets() {
		EngineAssetManager.getInstance().loadSound(getFilename());
	}
	
	@Override
	public void retrieveAssets() {
		s = EngineAssetManager.getInstance().getSound(getFilename());
	}
	
	@Override
	public void dispose() {
		stop();
		EngineAssetManager.getInstance().disposeSound(getFilename());
	}
}

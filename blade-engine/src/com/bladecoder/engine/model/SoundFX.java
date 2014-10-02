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

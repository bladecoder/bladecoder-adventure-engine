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
import com.bladecoder.engine.actions.ModelDescription;
import com.bladecoder.engine.actions.ModelPropertyType;
import com.bladecoder.engine.actions.Param;
import com.bladecoder.engine.assets.AssetConsumer;
import com.bladecoder.engine.assets.EngineAssetManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@ModelDescription("Actors can have a list of sounds that can be associated to Sprites or played with the 'sound' action")
public class SoundFX extends AbstractModel implements AssetConsumer {
	@JsonProperty(required = true)
	@JsonPropertyDescription("Filename of the sound")
	@ModelPropertyType(Param.Type.SOUND)
	private String filename;

	@JsonProperty
	@JsonPropertyDescription("True if the sound is looping")
	private boolean loop;

	@JsonProperty
	@JsonPropertyDescription("Select the volume")
	private float volume = 1f;

	transient private Sound s;

	public SoundFX() {
		
	}
	
	public SoundFX(String filename, boolean loop, float volume) {
		this.filename = filename;
		this.loop = loop;
		this.volume = volume;
	}
	
	public void play() {
		if(s==null)
			return;
		
		if(loop) s.loop();
		else s.play(volume);
	}

	public void stop() {
		if(s==null)
			return;
		
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

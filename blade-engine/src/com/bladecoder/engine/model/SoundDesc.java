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

public class SoundDesc {
	private String id;
	private boolean loop;
	private String filename;
	private float volume = 1f;
	private float pitch = 1f;
	private float pan = 0f;
	private boolean preload;
	
	public SoundDesc() {
		
	}
	
	public SoundDesc(String id, String filename, boolean loop, float volume, float pan, float pitch, boolean preload) {
		this.id = id;
		this.filename = filename;
		this.loop = loop;
		this.volume = volume;
		this.pan = pan;
		this.setPitch(pitch);
	}
	
	public boolean getLoop() {
		return loop;
	}
	
	public String getId() {
		return id != null? id: filename;
	}

	public void setId(String id) {
		this.id = id;
	}

	public float getPan() {
		return pan;
	}

	public void setPan(float pan) {
		this.pan = pan;
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

	public boolean isPreload() {
		return preload;
	}

	public void setPreload(boolean preload) {
		this.preload = preload;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}
}

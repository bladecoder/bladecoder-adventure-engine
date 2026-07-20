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

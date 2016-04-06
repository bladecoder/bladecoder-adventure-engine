package com.bladecoder.engine.model;

/**
 * Keeps the information to play a music file.
 * 
 * @author rgarcia
 */
public class MusicDesc {
	private String filename;
	
	private boolean loop = false;
	
	/**
	 * The music starts to play after the delay.
	 */
	private float initialDelay = 0;
	
	/** 
	 * Time for repeating the music when no looping.
	 *    
	 * -1 for no repeat 
	 */
	private float repeatDelay = -1;
	
	/**
	 * Stops the music when leaving the current scene 
	 */
	private boolean stopWhenLeaving = true;
	
	private float volume = 1.0f;
	
	public MusicDesc() {
		
	}
	
	public MusicDesc(MusicDesc md) {
		filename = md.getFilename();
		loop = md.isLoop();
		initialDelay = md.getInitialDelay();
		repeatDelay = md.getRepeatDelay();
		stopWhenLeaving = md.isStopWhenLeaving();
		volume = md.getVolume();
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String musicFilename) {
		this.filename = musicFilename;
	}

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	public float getInitialDelay() {
		return initialDelay;
	}

	public void setInitialDelay(float initialDelay) {
		this.initialDelay = initialDelay;
	}

	public float getRepeatDelay() {
		return repeatDelay;
	}

	public void setRepeatDelay(float repeatDelay) {
		this.repeatDelay = repeatDelay;
	}

	public boolean isStopWhenLeaving() {
		return stopWhenLeaving;
	}

	public void setStopWhenLeaving(boolean stopWhenLeaving) {
		this.stopWhenLeaving = stopWhenLeaving;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
	}
}

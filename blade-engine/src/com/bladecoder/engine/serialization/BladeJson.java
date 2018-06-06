package com.bladecoder.engine.serialization;

import com.badlogic.gdx.utils.Json;
import com.bladecoder.engine.model.World;

/**
 * The libgdx Json object with the World instance, the serialization mode and
 * the ActionCallback + World serializers added.
 * 
 * @author rgarcia
 */
public class BladeJson extends Json {
	public enum Mode {MODEL, STATE};
	
	private final World w;
	private final Mode mode;
	
	public BladeJson(World w, Mode mode) {
		super();
		
		this.w = w;
		this.mode = mode;
	}
	
	public World getWorld() {
		return w;
	}
	
	public Mode getMode() {
		return mode;
	}
}

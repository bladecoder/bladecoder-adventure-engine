package com.bladecoder.engine.loader;

public class SerializationHelper {

	public enum Mode {
		MODEL, STATE
	};

	private static SerializationHelper instance;

	private Mode mode = Mode.MODEL;

	private SerializationHelper() {

	}

	public static SerializationHelper getInstance() {
		if (instance == null)
			instance = new SerializationHelper();

		return instance;
	}

	public Mode getMode() {
		return mode;
	}
	
	public void setMode(Mode m) {
		this.mode = m;
	}
}

package com.bladecoder.engine.loader;

import java.util.HashMap;

import com.bladecoder.engine.model.BaseActor;

public class SerializationHelper {

	public enum Mode {
		INMUTABLE, MUTABLE
	};

	private static SerializationHelper instance;

	private final HashMap<String, BaseActor> actors = new HashMap<String, BaseActor>();

	private Mode mode = Mode.INMUTABLE;

	private SerializationHelper() {

	}

	public static SerializationHelper getInstance() {
		if (instance == null)
			instance = new SerializationHelper();

		return instance;
	}

	public void addActor(BaseActor a) {
		String id = a.getInitScene() + "." + a.getId();

		actors.put(id, a);
	}

	public BaseActor getActor(String initScene, String id) {
		return actors.get(initScene + "." + id);
	}
	
	public BaseActor getActor(String internalId) {
		return actors.get(internalId);
	}

	public void start(Mode m) {
		mode = m;
	}

	public Mode getMode() {
		return mode;
	}

	public void dispose() {
		actors.clear();
		mode = Mode.INMUTABLE;
	}
}

package org.bladecoder.engine.anim;

import java.util.ArrayList;
import java.util.List;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.TweenManager;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class TweenManagerSingleton {
	private static TweenManager instance = null;

	public static TweenManager getInstance() {
		if (instance == null) {
			instance = new TweenManager();
		}

		return instance;
	}

	public static void write(Json json) {
		List<BaseTween<?>> objects = getInstance().getObjects();

		// CREATE TWEEN LIST
		ArrayList<EngineTween> tweens = new ArrayList<EngineTween>();

		for (BaseTween<?> t : objects) {
			// Only serialize SpriteActors
			if (!t.isFinished() && t instanceof EngineTween) {
				EngineTween et = (EngineTween) t.getUserData();

				tweens.add(et);
			}
		}

		// SAVE LIST
		json.writeValue("tweens", tweens);

	}

	public static void read (Json json, JsonValue jsonData) {
		getInstance().killAll();

		@SuppressWarnings("unchecked")
		ArrayList<EngineTween> tweens = json.readValue("tweens",
				ArrayList.class, EngineTween.class, jsonData);

		for (EngineTween st : tweens) {
			getInstance().add(st);
			st.resumeSaved();
		}
	}
}

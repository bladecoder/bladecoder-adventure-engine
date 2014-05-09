package org.bladecoder.engine.anim;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.AtlasRenderer;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for spriteactor position animation
 */
public class FATween extends Tween {
	
	public FATween() {
	}

	public void start(AtlasRenderer target, int repeatType, int count, float duration, ActionCallback cb) {		
		setDuration(duration);
		setType(repeatType);
		setCount(count);

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}
	
	public void update(AtlasRenderer a, float delta) {
		update(delta);
		
		if(!isComplete())
			a.setFrame((int)(getPercent() * a.getNumFrames()));
	}
	
	@Override
	public void write(Json json) {
		super.write(json);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	

	}
}

package org.bladecoder.engine.anim;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.SpriteActor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for spriteactor position animation
 */
public class SpritePosTween extends Tween {
	
	private float startX, startY;
	private float targetX, targetY;
	
	public SpritePosTween() {
	}

	public void start(SpriteActor target, int repeatType, int count, float tx, float ty, float duration, ActionCallback cb) {
		
		Vector2 currentPos = target.getPosition();
		
		startX = currentPos.x;
		startY = currentPos.y;
		targetX = tx;
		targetY = ty;
		
		setDuration(duration);
		setType(repeatType);
		setCount(count);

		if (cb != null) {
			setCb(cb);
		}
		
		restart();
	}
	
	public void update(SpriteActor a, float delta) {
		update(delta);
		
		a.setPosition(startX + getPercent() * (targetX - startX),
				startY + getPercent() * (targetY - startY));
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startX", startX);
		json.writeValue("startY", startY);
		json.writeValue("targetX", targetX);
		json.writeValue("targetY", targetY);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		startX = json.readValue("startX", Float.class, jsonData);
		startY = json.readValue("startY", Float.class, jsonData);
		targetX = json.readValue("targetX", Float.class, jsonData);
		targetY = json.readValue("targetY", Float.class, jsonData);

	}
}

package org.bladecoder.engine.anim;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.model.SceneCamera;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for camera position and zoom animation
 */
public class CameraTween extends Tween {
	
	private float startX, startY, startZoom;
	private float targetX, targetY, targetZoom;
	
	public CameraTween() {
	}

	public void start( SceneCamera camera, int repeatType, int count, float targetX, float targetY, float targetZoom, float duration, ActionCallback cb) {
		
		Vector2 currentPos = camera.getPosition();
		
		startX = currentPos.x;
		startY = currentPos.y;
		startZoom = camera.getZoom();
		this.targetX = targetX;
		this.targetY = targetY;
		this.targetZoom = targetZoom;
		
		setDuration(duration);
		setType(repeatType);
		setCount(count);

		if (cb != null) {
			setCb(cb);
		}
	}
	
	public void update(float delta, SceneCamera camera) {
		update(delta);
		
		camera.setPosition(startX + getPercent() * (targetX - startX),
				startY + getPercent() * (targetY - startY));
		
		camera.setZoom(startZoom + getPercent() * (targetZoom- startZoom));
	}
	
	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("startX", startX);
		json.writeValue("startY", startY);
		json.writeValue("startZoom", startZoom);
		json.writeValue("targetX", targetX);
		json.writeValue("targetY", targetY);
		json.writeValue("targetZoom", targetZoom);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);	
		
		startX = json.readValue("startX", Float.class, jsonData);
		startY = json.readValue("startY", Float.class, jsonData);
		startZoom = json.readValue("startZoom", Float.class, jsonData);
		targetX = json.readValue("targetX", Float.class, jsonData);
		targetY = json.readValue("targetY", Float.class, jsonData);
		targetZoom = json.readValue("targetZoom", Float.class, jsonData);
	}
}

package org.bladecoder.engine.anim;

import java.util.ArrayList;

import org.bladecoder.engine.actions.ActionCallback;
import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.model.SpriteActor;
import org.bladecoder.engine.model.World;

import aurelienribon.tweenengine.TweenCallback;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Tween for frame animation
 */
public class WalkTween extends EngineTween implements Serializable {

	protected SpriteActor target;
	private ArrayList<Vector2> walkingPath;
	private int currentStep = 0;
	private float speed = 0;
	private float segmentDuration = 0;
	private float lastSegmentTime = 0;

	public WalkTween() {
		type = EngineTween.WALK_TYPE;

		startValues = new float[2];
		targetValues = new float[2];
		valueBuffer = new float[2];
	}

	public void start(SpriteActor target, ArrayList<Vector2> walkingPath,
			float speed, ActionCallback cb) {
		this.target = target;
		this.duration = 1000000;
		this.walkingPath = walkingPath;
		this.speed = speed;
		this.segmentDuration = 0;
		this.lastSegmentTime = 0;
		this.currentStep = 0;

		if (cb != null) {
			this.cb = cb;
			setCallback(tweenCb);
			setCallbackTriggers(TweenCallback.COMPLETE);
		}

		start();
		walkToNextStep();
	}

	private void walkToNextStep() {
		Vector2 p0 = walkingPath.get(currentStep);
		Vector2 pf = walkingPath.get(currentStep + 1);

		target.startWalkFA(p0, pf);

		startValues[0] = p0.x;
		startValues[1] = p0.y;
		targetValues[0] = pf.x;
		targetValues[1] = pf.y;

		segmentDuration = p0.dst(pf)
				/ (EngineAssetManager.getInstance().getScale() * speed);
	}

	private void segmentEnded() {

		currentStep++;

		if (currentStep < walkingPath.size() - 1) {
			lastSegmentTime = getCurrentTime();
			walkToNextStep();
		} else { // WALK ENDED

			target.stand();

			duration = getCurrentTime();
		}
	}

	@Override
	protected void updateOverride(int step, int lastStep,
			boolean isIterationStep, float delta) {
		if (getTarget() == null)
			return;

		// Normal behavior
		float time = getCurrentTime() - lastSegmentTime;
		float t = equation.compute(time / segmentDuration);

		for (int i = 0; i < startValues.length; i++) {
			valueBuffer[i] = startValues[i] + t
					* (targetValues[i] - startValues[i]);
		}

		setValues(valueBuffer);

		if (t >= 1.0) {
			segmentEnded();
		}
	}

	@Override
	protected void setValues(float[] values) {
		target.setPosition(values[0], values[1]);
	}

	@Override
	protected Object getTarget() {
		return target;
	}

	@Override
	public void write(Json json) {
		super.write(json);

		json.writeValue("targetId", target.getId());
		json.writeValue("path", walkingPath);
		json.writeValue("currentStep", currentStep);
		json.writeValue("speed", speed);
		json.writeValue("segmentDuration", segmentDuration);
		json.writeValue("lastSegmentTime", lastSegmentTime);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
		super.read(json, jsonData);

		String targetId = json.readValue("targetId", String.class, jsonData);
		target = (SpriteActor) World.getInstance().getCurrentScene()
				.getActor(targetId);
		
		walkingPath = json.readValue("path", ArrayList.class, Vector2.class, jsonData);
		currentStep = json.readValue("currentStep", Integer.class, jsonData);
		speed = json.readValue("speed", Float.class, jsonData);
		segmentDuration = json.readValue("segmentDuration", Float.class, jsonData);
		lastSegmentTime = json.readValue("lastSegmentTime", Float.class, jsonData);
	}
}

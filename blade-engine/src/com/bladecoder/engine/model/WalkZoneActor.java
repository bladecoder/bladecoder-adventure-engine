package com.bladecoder.engine.model;

import com.badlogic.gdx.utils.Json;
import com.bladecoder.engine.serialization.BladeJson;
import com.bladecoder.engine.serialization.BladeJson.Mode;
import com.bladecoder.engine.util.PolygonUtils;

/**
 * An Obstacle actor is used to restrict the walk zone in the scene
 * 
 * @author rgarcia
 */
public class WalkZoneActor extends BaseActor {

	@Override
	public void update(float delta) {
	}

	@Override
	public void setPosition(float x, float y) {
		getBBox().setPosition(x, y);

		if (scene != null && id.equals(scene.getWalkZone())) {
			scene.getPolygonalNavGraph().createInitialGraph(this, scene.getActors().values());
		}
	}

	@Override
	public void write(Json json) {
		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			PolygonUtils.ensureClockWise(getBBox().getVertices(), 0, getBBox().getVertices().length);
			getBBox().dirty();
		}

		super.write(json);
	}
}

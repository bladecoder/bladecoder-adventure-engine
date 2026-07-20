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
public class ObstacleActor extends BaseActor {

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (scene != null && scene.getPolygonalNavGraph() != null) {
			if (visible)
				scene.getPolygonalNavGraph().addDinamicObstacle(getBBox());
			else
				scene.getPolygonalNavGraph().removeDinamicObstacle(getBBox());
		}
	}

	@Override
	public void update(float delta) {
	}

	@Override
	public void setPosition(float x, float y) {
		boolean inNavGraph = false;

		if (scene != null && scene.getPolygonalNavGraph() != null) {
			inNavGraph = scene.getPolygonalNavGraph().removeDinamicObstacle(getBBox());
		}

		getBBox().setPosition(x, y);

		if (inNavGraph) {
			scene.getPolygonalNavGraph().addDinamicObstacle(getBBox());
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

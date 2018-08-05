/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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

	public void setPosition(float x, float y) {
		bbox.setPosition(x, y);

		if (scene != null && id.equals(scene.getWalkZone())) {
			scene.getPolygonalNavGraph().createInitialGraph(this, scene.getActors().values());
		}
	}

	@Override
	public void write(Json json) {
		BladeJson bjson = (BladeJson) json;
		if (bjson.getMode() == Mode.MODEL) {
			PolygonUtils.ensureClockWise(bbox.getVertices(), 0, bbox.getVertices().length);
			bbox.dirty();
		}

		super.write(json);
	}
}
